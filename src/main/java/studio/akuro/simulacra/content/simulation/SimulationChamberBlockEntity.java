package studio.akuro.simulacra.content.simulation;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.SimulacraConfig;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.index.ModBlockEntities;
import studio.akuro.simulacra.index.ModItems;
import studio.akuro.simulacra.index.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Holds one Data Matrix and, while fed compute by a Mainframe Controller, either trains it (raw data to
 * a trained model) or simulates it (emitting that mob's loot). Controller-driven via
 * {@link #supplyCompute(float)}; no ticker of its own.
 */
public class SimulationChamberBlockEntity extends BlockEntity
        implements IHaveGoggleInformation, net.minecraft.world.MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simulacra.simulation_chamber");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inventory,
            net.minecraft.world.entity.player.Player player) {
        return new SimulationChamberMenu(id, inventory, this);
    }

    public enum Mode { EMPTY, UNBOUND, NEED_DATA, TRAINING, SIMULATING }

    /** Why a chamber in SIMULATING mode is not making progress. */
    public enum StallReason { NONE, NO_SUBSTRATE, SUBSTRATE_TOO_CRUDE, OUTPUT_FULL, UNKNOWN_SUBJECT, REDSTONE }

    /**
     * How the chamber answers a redstone signal.
     *
     * <p>Create players gate machinery on redstone constantly — a clock, a threshold switch on a
     * storage bank, a lever by the door — and a machine with no way to be told to stop has to be
     * unplugged from its whole array instead.
     *
     * <p>There is deliberately no run-only-when-powered mode. Create funnels stop passing items while
     * they carry a signal, and a funnel on the chamber is the normal build rather than an edge case,
     * so a chamber that needed power to run would have its substrate and its output cut off by the
     * very signal that started it. Powering to stop has no such problem: the machine and its funnels
     * go quiet together, which is what was being asked for.
     */
    public enum RedstoneMode { IGNORED, OFF_WHEN_POWERED }

    private static final int OUTPUT_SLOTS = 9;
    /** How often a working chamber repeats its loop, matching the array hum's cadence. */
    private static final int AMBIENT_INTERVAL = 80;

    // Balance values are read live from config so packs can retune them; see SimulacraConfig.
    private static float demand() {
        return SimulacraConfig.CHAMBER_COMPUTE_DEMAND.get().floatValue();
    }

    private static float trainCost() {
        return SimulacraConfig.TRAIN_COST.get().floatValue();
    }

    private static float jobCost() {
        return SimulacraConfig.JOB_COST.get().floatValue();
    }

    private ItemStack model = ItemStack.EMPTY;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private float progress = 0f;
    private float suppliedPerTick = 0f;
    private int lastSyncedDecile = -1;
    /** Game time of the last supplyCompute call; lets the watchdog notice a vanished controller. */
    private long lastFedTime = Long.MIN_VALUE;

    /** One job prints onto an imprint blank of any grade; with no substrate the chamber stalls. */
    private final ItemStackHandler substrate = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return SubstrateTier.fromStack(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Push a block update so the client-side goggle/status sees substrate changes immediately.
            markUpdated();
        }
    };
    /** Buffer that simulated loot lands in, so it can be funnelled out by hoppers or Create funnels. */
    private final ItemStackHandler inventory = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Any change to the buffer may have made room for the batch that last failed to fit,
            // so let the chamber try again rather than staying blocked until something else pokes it.
            outputBlocked = false;
            // Keep attached comparators tracking the buffer.
            if (level != null && !level.isClientSide) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };
    /**
     * Set when a rolled batch would not fit even though the buffer had a free slot. Without it the
     * chamber would sit at full progress drawing its whole compute allowance forever, starving every
     * other chamber on the array while producing nothing.
     */
    private boolean outputBlocked = false;
    /**
     * Stall state at the last check, so the cue fires on the transition rather than every tick.
     * Null means "not yet observed", which keeps a chamber that loads in already stalled quiet.
     */
    private StallReason lastStallReason = null;
    /** Single capability face: push substrate in, pull loot out, never the reverse. */
    private final ChamberItemHandler itemHandler = new ChamberItemHandler(substrate, inventory);

    public SimulationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMULATION_CHAMBER.get(), pos, state);
    }

    /**
     * The substrate buffer, for the screen's own slot.
     *
     * <p>Deliberately not the same thing as {@link #getItemHandler()}: that one is the face shown to
     * hoppers and funnels and refuses to hand substrate back, which is right for automation and wrong
     * for a player who put the wrong tier in.
     */
    public ItemStackHandler getSubstrateHandler() {
        return substrate;
    }

    /** The finished-loot buffer, for the screen's output grid. */
    public ItemStackHandler getOutputHandler() {
        return inventory;
    }

    /** The capability handed to neighbours: substrate in, loot out. */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** Tries to load a held substrate stack via right-click; returns how many were accepted. */
    public int insertSubstrate(ItemStack held) {
        ItemStack leftover = substrate.insertItem(0, held.copy(), false);
        int accepted = held.getCount() - leftover.getCount();
        if (accepted > 0) {
            playSound(ModSounds.CHAMBER_SUBSTRATE.get(), 0.4f, 1.1f);
        }
        return accepted;
    }

    /**
     * Pulls the loaded substrate back out. The item handler deliberately refuses to give substrate
     * back to automation (it is an input face, not a buffer), which left breaking the block as the
     * only way to change tiers by hand.
     */
    public ItemStack removeSubstrate() {
        ItemStack held = substrate.getStackInSlot(0);
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        substrate.setStackInSlot(0, ItemStack.EMPTY);
        playSound(ModSounds.CHAMBER_SUBSTRATE.get(), 0.4f, 0.9f);
        markUpdated();
        return held;
    }

    /** Comparator level for the loot buffer, using the vanilla container fullness formula. */
    public int getComparatorLevel() {
        float fill = 0f;
        boolean empty = true;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                empty = false;
                fill += (float) stack.getCount() / Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize());
            }
        }
        return Mth.floor(fill / inventory.getSlots() * 14f) + (empty ? 0 : 1);
    }

    /** Pops the substrate and loot buffers into the world; called when the chamber is broken. */
    public void dropContents() {
        if (level == null) {
            return;
        }
        popHandler(substrate);
        popHandler(inventory);
    }

    private void popHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
            }
        }
    }

    public Mode getMode() {
        if (model.isEmpty() || !(model.getItem() instanceof DataMatrixItem)) {
            return Mode.EMPTY;
        }
        if (DataMatrixItem.getBoundMob(model).isEmpty()) {
            return Mode.UNBOUND;
        }
        if (DataMatrixItem.isTrained(model)) {
            return Mode.SIMULATING;
        }
        return DataMatrixItem.getData(model) >= DataMatrixItem.dataToTrain() ? Mode.TRAINING : Mode.NEED_DATA;
    }

    private float currentCost() {
        if (getMode() == Mode.TRAINING) {
            return trainCost();
        }
        // Better-trained models simulate cheaper: each grade above Coarse shaves off speedBonusPerGrade.
        int grade = DataMatrixItem.getModelTier(model);
        float mult = 1f - SimulacraConfig.MODEL_SPEED_BONUS.get().floatValue() * Math.max(0, grade - 1);
        // Total cost scales alongside the draw rate. Scaling only the rate would make a boss job
        // finish ten times faster than a zombie's, since the same cost would be paid by a far larger
        // per-tick draw. Scaling both means a harder subject needs a proportionally bigger array to
        // run at the same pace, which is the point, rather than being quicker for free.
        return jobCost() * Math.max(0.25f, mult) * demandMultiplier();
    }

    /** Only ask the array for compute when there is real work to do and it would not stall. */
    /**
     * Rated compute draw: what this chamber wants per tick to run at its normal pace.
     *
     * <p>Scaled by what is actually being simulated. Finer substrate and boss-grade subjects cost
     * proportionally more, so the size of an array decides what it is capable of simulating rather
     * than only how many chambers it can keep fed. Training is charged at the base rate, since the
     * substrate is not involved yet.
     */
    public float getComputeDemand() {
        // A chamber held off by redstone asks for nothing, so the array's budget goes to the ones
        // still working rather than being reserved against a machine that is deliberately idle.
        if (isRedstoneBlocked()) {
            return 0f;
        }
        return switch (getMode()) {
            case TRAINING -> demand();
            case SIMULATING -> canSimulate() ? demand() * demandMultiplier() : 0f;
            default -> 0f;
        };
    }

    /** How much more than its rated demand this chamber would still accept. */
    public float getOverclockHeadroom() {
        float rated = getComputeDemand();
        if (rated <= 0f) {
            return 0f;
        }
        return rated * (SimulacraConfig.OVERCLOCK_MAX.get().floatValue() - 1f);
    }

    private float demandMultiplier() {
        float multiplier = 1f;
        SubstrateTier tier = currentTier();
        if (tier != null) {
            multiplier *= switch (tier) {
                case CRUDE -> 1f;
                case REFINED -> SimulacraConfig.SUBSTRATE_DEMAND_REFINED.get().floatValue();
                case PRISTINE -> SimulacraConfig.SUBSTRATE_DEMAND_PRISTINE.get().floatValue();
            };
        }
        EntityType<?> type = resolveBoundType();
        if (type != null && type.is(Tags.EntityTypes.BOSSES)) {
            multiplier *= SimulacraConfig.BOSS_DEMAND_MULTIPLIER.get().floatValue();
        }
        return multiplier;
    }

    /**
     * Compute above the rated draw still counts, but at a discount, so a big array speeds a chamber
     * up without ever beating the throughput of simply building another one.
     */
    private float effectiveProgress(float supplied) {
        float rated = getComputeDemand();
        if (rated <= 0f || supplied <= rated) {
            return supplied;
        }
        float efficiency = SimulacraConfig.OVERCLOCK_EFFICIENCY.get().floatValue();
        return rated + (supplied - rated) * efficiency;
    }

    /** True only while a simulation can actually make progress. */
    public boolean canSimulate() {
        return stallReason() == StallReason.NONE;
    }

    /** Simulating but blocked: used to surface a reason in the status readout. */
    public boolean isStalled() {
        return getMode() == Mode.SIMULATING && !canSimulate();
    }

    /** The first blocker in priority order, or NONE when the chamber can run. */
    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    /** Steps to the next mode; called from the screen's button. */
    public void cycleRedstoneMode() {
        RedstoneMode[] all = RedstoneMode.values();
        redstoneMode = all[(redstoneMode.ordinal() + 1) % all.length];
        setChanged();
        markUpdated();
    }

    /** True when the current signal and mode say this chamber should be idle. */
    public boolean isRedstoneBlocked() {
        if (redstoneMode == RedstoneMode.IGNORED || level == null) {
            return false;
        }
        return level.hasNeighborSignal(worldPosition);
    }

    public StallReason stallReason() {
        // Checked first: a chamber told to stop is stopped, whatever else is or is not wrong with it.
        if (isRedstoneBlocked()) {
            return StallReason.REDSTONE;
        }
        // A model whose mob no longer exists (its mod was removed) can never print anything. Report
        // it instead of letting the chamber draw compute against a job that must always fail.
        if (DataMatrixItem.getBoundMob(model).isPresent() && resolveBoundType() == null) {
            return StallReason.UNKNOWN_SUBJECT;
        }
        if (!hasSubstrate()) {
            return StallReason.NO_SUBSTRATE;
        }
        if (!substrateAllowsSubject()) {
            return StallReason.SUBSTRATE_TOO_CRUDE;
        }
        if (outputBlocked || !hasOutputRoom()) {
            return StallReason.OUTPUT_FULL;
        }
        return StallReason.NONE;
    }

    public boolean hasSubstrate() {
        return substrate.getStackInSlot(0).getCount() >= SimulacraConfig.SUBSTRATE_PER_JOB.get();
    }

    /** The loaded substrate's tier, or null when nothing usable is loaded. */
    public SubstrateTier currentTier() {
        return SubstrateTier.fromStack(substrate.getStackInSlot(0));
    }

    /** Boss-grade subjects (c:bosses) need pristine substrate to hold the imprint. */
    private boolean substrateAllowsSubject() {
        SubstrateTier tier = currentTier();
        if (tier == null) {
            return false;
        }
        if (tier.allowsBosses()) {
            return true;
        }
        EntityType<?> type = resolveBoundType();
        return type == null || !type.is(Tags.EntityTypes.BOSSES);
    }

    /**
     * Whether the buffer could take any more items at all. A partly-filled slot still has room, so
     * counting only empty slots would call nine slots of one rotten flesh "full".
     */
    private boolean hasOutputRoom() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                return true;
            }
            int cap = Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(slot));
            if (stack.getCount() < cap) {
                return true;
            }
        }
        return false;
    }

    /** Called by the controller each server tick with the compute it granted (may be 0). */
    public void supplyCompute(float amount) {
        if (level == null || level.isClientSide) {
            return;
        }
        suppliedPerTick = amount;
        lastFedTime = level.getGameTime();
        updateLit(amount > 0f);
        announceStall();
        if (amount > 0f) {
            float applied = effectiveProgress(amount);
            switch (getMode()) {
                case TRAINING -> {
                    // Training is the longest wait in the mod and was completely silent, so a player
                    // could not tell a working chamber from a stalled one without walking up to it.
                    // Offset by position so a bank of chambers does not churn in lockstep.
                    if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), AMBIENT_INTERVAL) == 0) {
                        playSound(ModSounds.CHAMBER_TRAINING.get(), 0.2f, 0.9f);
                    }
                    progress += applied;
                    if (progress >= trainCost()) {
                        // Carry the overshoot instead of discarding it, so a chamber fed far more
                        // than its rated draw is not quietly throwing away most of the last tick.
                        progress -= trainCost();
                        DataMatrixItem.setTrained(model, true);
                        DataMatrixItem.setProgress(model, 0f);
                        // The model resolving is the payoff of a long wait, so it gets Create's
                        // confirm chime rather than another machine noise.
                        AllSoundEvents.CONFIRM.playOnServer(level, worldPosition, 0.6f, 1.0f);
                    }
                }
                case SIMULATING -> {
                    // Stall (no progress) unless we have substrate and somewhere to put the loot.
                    if (canSimulate()) {
                        float cost = currentCost();
                        progress += applied;
                        if (progress >= cost) {
                            if (runJob()) {
                                progress -= cost;
                            } else {
                                // Loot did not fit after all. Hold at full rather than spilling, and
                                // runJob has latched outputBlocked, so demand drops to zero until the
                                // buffer changes and the job resumes from here.
                                progress = cost;
                            }
                        }
                    }
                }
                default -> { /* idle: nothing to do */ }
            }
        }
        syncProgress();
    }

    /**
     * Rolls one batch of loot and, only if all of it fits in the buffer, commits it and spends one
     * substrate. Returns false (leaving everything untouched) when the mob is unresolvable or the loot
     * would not fit, so the caller can stall instead of voiding drops.
     */
    private boolean runJob() {
        if (!(level instanceof ServerLevel server) || !canSimulate()) {
            return false;
        }
        EntityType<?> type = resolveBoundType();
        if (type == null) {
            // The mob's mod is no longer present; the model is stranded. Treat as a no-op job.
            return false;
        }
        List<ItemStack> drops = new ArrayList<>();
        // Accuracy check: a low-grade model can botch the print. A failed job spends the compute and
        // substrate like any other, but produces a Corrupted Imprint instead of loot. The model still
        // learns from the failure, so even a Coarse model grades up over time.
        boolean success = server.random.nextFloat() < DataMatrixItem.accuracy(model);
        if (success) {
            // Substrate grade sets the fidelity of the print, in Predictions rather than raw loot: one
            // Prediction is one roll of this subject's table, and the Loot Fabricator decides later
            // what shape that value takes. Handing out items here instead would mean either burying
            // the player in whatever the table felt like, or letting a filter hand out rare drops at
            // common-drop rates. A Self-Aware model squeezes out an extra roll on top.
            SubstrateTier tier = currentTier();
            int rolls = tier.lootRolls();
            if (DataMatrixItem.getModelTier(model) >= 4) {
                rolls += SimulacraConfig.SELF_AWARE_BONUS_ROLLS.get();
            }
            ItemStack predictions = new ItemStack(ModItems.PREDICTION.get(), rolls);
            PredictionItem.setSubject(predictions, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            drops.add(predictions);
            // Boss subjects can resonate with the substrate and print a catalyst, the renewable
            // route to more pristine blanks (the circular endgame economy).
            if (type.is(Tags.EntityTypes.BOSSES)
                    && server.random.nextFloat() < SimulacraConfig.CATALYST_CHANCE.get().floatValue()) {
                drops.add(new ItemStack(ModItems.RESONANT_CATALYST.get()));
            }
        }
        if (!drainFits(drops)) {
            // Latch until the buffer changes; stallReason reports OUTPUT_FULL meanwhile, which drops
            // this chamber's compute demand to zero instead of holding the array hostage.
            outputBlocked = true;
            return false;
        }
        outputBlocked = false;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                ItemHandlerHelper.insertItemStacked(inventory, drop, false);
            }
        }
        if (!success) {
            // A failed print leaves a Corrupted Imprint, but it is deliberately handled after the
            // fit check above and its remainder is dropped rather than stalling the machine.
            //
            // Predictions and Corrupted Imprints leave a chamber by the same face, and nothing
            // downstream accepts both — a Loot Fabricator takes Predictions only. So the normal
            // automated build filters for Predictions and leaves the corrupted ones behind, where
            // they accumulate at the failure rate until they own all nine slots and the chamber
            // stalls for good. Letting the failure product block the machine that produced it is
            // the worse of the two failure modes; losing one is recoverable, deadlock is not.
            ItemHandlerHelper.insertItemStacked(inventory,
                    new ItemStack(ModItems.CORRUPTED_IMPRINT.get()), false);
        }
        substrate.extractItem(0, SimulacraConfig.SUBSTRATE_PER_JOB.get(), false);
        // A finished job is the blank being impressed and cured: the press sound pitched well down,
        // so it reads as a heavier, slower relative of the Fabricator's strike.
        AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(server, worldPosition, 0.35f, 0.65f);
        // Learning by doing: every completed job feeds data back into the model.
        DataMatrixItem.addSimData(model, 1);
        return true;
    }

    private EntityType<?> resolveBoundType() {
        Optional<String> bound = DataMatrixItem.getBoundMob(model);
        if (bound.isEmpty()) {
            return null;
        }
        ResourceLocation mobId = ResourceLocation.tryParse(bound.get());
        return mobId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
    }

    /** Tests whether every drop fits the buffer by inserting into a throwaway copy of it. */
    private boolean drainFits(List<ItemStack> drops) {
        ItemStackHandler scratch = new ItemStackHandler(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            scratch.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
        }
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            if (!ItemHandlerHelper.insertItemStacked(scratch, drop.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The loot table a simulation of this mob should roll.
     *
     * <p>Prefers a Simulacra override at {@code simulacra:simulation/<mob path>}, falling back to the
     * mob's own table. The override exists because some mobs drop nothing through a loot table at all:
     * the Wither's nether star and the dragon's rewards are handed out in {@code dropCustomDeathLoot}
     * and hardcoded death logic, so a simulation rolling only the vanilla table produces literally
     * nothing for them. Datapacks can add or replace an override for any mob without touching Java.
     */
    private LootTable simulationTable(ServerLevel server, EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        ResourceKey<LootTable> override = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, "simulation/" + id.getPath()));
        LootTable table = server.getServer().reloadableRegistries().getLootTable(override);
        if (table != LootTable.EMPTY) {
            return table;
        }
        return server.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
    }

    /**
     * Rolls the bound mob's live loot table so any drops other mods inject into that mob's pool are
     * simulated too, instead of us hard-coding a drop list. We instantiate a throwaway entity only to
     * satisfy the loot context's required THIS_ENTITY parameter; it is never added to the world (farming
     * without spawning mobs is the whole point of the chamber) and is discarded right after the roll.
     */
    private List<ItemStack> rollSimulatedLoot(ServerLevel server, EntityType<?> type) {
        LootTable table = simulationTable(server, type);
        if (table == LootTable.EMPTY) {
            return List.of();
        }
        Entity probe = type.create(server);
        if (!(probe instanceof LivingEntity living)) {
            if (probe != null) {
                probe.discard();
            }
            return List.of();
        }
        Vec3 origin = Vec3.atCenterOf(worldPosition.above());
        living.moveTo(origin.x, origin.y, origin.z, 0f, 0f);
        // The kill is attributed to a fake player. Without it every killed_by_player-gated drop is
        // silently skipped, which is most of what players actually want a model for: blaze rods,
        // wither skeleton skulls, zombie iron. A model trained on blazes that cannot print blaze rods
        // reads as broken, so the simulation stands in for a player kill rather than an anonymous one.
        FakePlayer killer = FakePlayerFactory.getMinecraft(server);
        killer.moveTo(origin.x, origin.y, origin.z, 0f, 0f);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.DAMAGE_SOURCE, server.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        List<ItemStack> rolled = table.getRandomItems(params);
        living.discard();
        return rolled;
    }

    private void syncProgress() {
        float cost = currentCost();
        int decile = cost > 0f ? (int) (progress * 10f / cost) : 0;
        if (decile != lastSyncedDecile) {
            lastSyncedDecile = decile;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public ItemStack getModel() {
        return model;
    }

    public void setModel(ItemStack stack) {
        this.model = stack;
        // Training progress rides on the matrix, so swapping one in resumes where it left off.
        this.progress = DataMatrixItem.getProgress(stack);
        this.lastSyncedDecile = -1;
        playSound(ModSounds.CHAMBER_INSERT.get(), 0.4f, 1.0f);
        markUpdated();
    }

    public ItemStack removeModel() {
        ItemStack removed = model;
        if (!removed.isEmpty() && getMode() == Mode.TRAINING) {
            DataMatrixItem.setProgress(removed, progress);
        }
        this.model = ItemStack.EMPTY;
        this.progress = 0f;
        this.lastSyncedDecile = -1;
        this.lastStallReason = null;
        playSound(ModSounds.CHAMBER_EJECT.get(), 0.4f, 1.2f);
        markUpdated();
        return removed;
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Plays the stall cue once, on the tick a running chamber first becomes blocked.
     *
     * <p>Deliberately quiet, and deliberately not a loop. The real signal that something is wrong is
     * the working sound stopping; this is only a nudge to look. An alarm is welcome once and resented
     * by the twentieth chamber.
     */
    private void announceStall() {
        StallReason reason = getMode() == Mode.SIMULATING ? stallReason() : StallReason.NONE;
        if (lastStallReason != null && reason != lastStallReason && reason != StallReason.NONE) {
            playSound(ModSounds.CHAMBER_STALL.get(), 0.25f, 1.0f);
        }
        lastStallReason = reason;
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    /**
     * Server ticker: a chamber is normally driven by its controller's tick, so if the controller is
     * broken or the cable is cut, supplyCompute stops arriving and the viewport would stay lit forever.
     * This watchdog notices the silence and goes dark.
     */
    public void tickWatchdog() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (suppliedPerTick > 0f && level.getGameTime() - lastFedTime > 2) {
            suppliedPerTick = 0f;
            updateLit(false);
            setChanged();
        }
    }

    /** Lights the viewport only while the chamber is being fed compute. */
    private void updateLit(boolean lit) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(SimulationChamberBlock.LIT) && state.getValue(SimulationChamberBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(SimulationChamberBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public boolean isRunning() {
        return suppliedPerTick > 0f;
    }

    public float getSuppliedComputePerTick() {
        return suppliedPerTick;
    }

    public float getProgressPercent() {
        float cost = currentCost();
        return cost > 0f ? Mth.clamp(progress * 100f / cost, 0f, 100f) : 0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Show which mob this chamber is modelling (and the model's grade), readable through goggles.
        DataMatrixItem.getBoundMob(model).ifPresent(key -> {
            Component subject = DataMatrixItem.isTrained(model)
                    ? Component.translatable("tooltip.simulacra.sim_model_graded", DataMatrixItem.mobName(key),
                            DataMatrixItem.tierName(DataMatrixItem.getModelTier(model)))
                    : Component.translatable("tooltip.simulacra.sim_model", DataMatrixItem.mobName(key));
            tooltip.add(Component.literal("    ").append(subject).withStyle(ChatFormatting.GRAY));
        });
        Component line;
        if (isStalled()) {
            line = Component.translatable(switch (stallReason()) {
                case SUBSTRATE_TOO_CRUDE -> "tooltip.simulacra.sim_stalled_crude";
                case OUTPUT_FULL -> "tooltip.simulacra.sim_stalled_full";
                case UNKNOWN_SUBJECT -> "tooltip.simulacra.sim_stalled_subject";
                default -> "tooltip.simulacra.sim_stalled_substrate";
            }).withStyle(ChatFormatting.RED);
        } else {
            line = switch (getMode()) {
                case EMPTY -> Component.translatable("tooltip.simulacra.sim_empty").withStyle(ChatFormatting.DARK_GRAY);
                case UNBOUND -> Component.translatable("tooltip.simulacra.sim_unbound").withStyle(ChatFormatting.RED);
                case NEED_DATA -> Component.translatable("tooltip.simulacra.sim_needdata").withStyle(ChatFormatting.RED);
                case TRAINING -> Component.translatable("tooltip.simulacra.sim_training", String.format("%.0f", getProgressPercent())).withStyle(ChatFormatting.GOLD);
                case SIMULATING -> Component.translatable("tooltip.simulacra.sim_simulating", String.format("%.0f", getProgressPercent())).withStyle(ChatFormatting.AQUA);
            };
        }
        tooltip.add(Component.literal("    ").append(line));
        if (getMode() == Mode.SIMULATING) {
            tooltip.add(Component.literal("    ").append(Component
                    .translatable("tooltip.simulacra.sim_substrate", substrate.getStackInSlot(0).getCount())
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        // How much finished work is sitting in the buffer. Without this the only way to know whether a
        // chamber is backing up — or whether the run you left going produced anything — is to break it
        // open, and a chamber whose buffer fills stops dead.
        int stored = storedPredictions();
        if (stored > 0) {
            tooltip.add(Component.literal("    ").append(Component
                    .translatable("tooltip.simulacra.sim_predictions", stored)
                    .withStyle(hasOutputRoom() ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
        return true;
    }

    /** Predictions waiting in the output buffer, across every slot. */
    public int storedPredictions() {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack held = inventory.getStackInSlot(slot);
            if (held.getItem() instanceof PredictionItem) {
                total += held.getCount();
            }
        }
        return total;
    }

    private void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("Progress", progress);
        tag.putFloat("Supplied", suppliedPerTick);
        if (!model.isEmpty()) {
            tag.put("Model", model.save(registries));
        }
        // Synced as well as saved, so the client goggle/status reflect substrate and buffer state.
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Substrate", substrate.serializeNBT(registries));
        tag.putByte("Redstone", (byte) redstoneMode.ordinal());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeData(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getFloat("Progress");
        suppliedPerTick = tag.getFloat("Supplied");
        model = tag.contains("Model")
                ? ItemStack.parse(registries, tag.getCompound("Model")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Substrate")) {
            substrate.deserializeNBT(registries, tag.getCompound("Substrate"));
        }
        // floorMod rather than a bare index: an ordinal written by a future version that dropped or
        // reordered a mode would otherwise throw on load and take the chunk with it.
        RedstoneMode[] modes = RedstoneMode.values();
        redstoneMode = modes[Math.floorMod(tag.getByte("Redstone"), modes.length)];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag, registries);
    }
}
