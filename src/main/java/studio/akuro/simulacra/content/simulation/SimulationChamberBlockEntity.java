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
     * <p>No run-only-when-powered mode: Create funnels stop passing items while powered, so a chamber
     * that needed power to run would have its substrate and output cut off by the signal that started
     * it. Powering to stop takes the machine and its funnels down together.
     */
    public enum RedstoneMode { IGNORED, OFF_WHEN_POWERED }

    private static final int OUTPUT_SLOTS = 9;
    /** How often a working chamber repeats its loop, matching the array hum's cadence. */
    private static final int AMBIENT_INTERVAL = 80;

    // Read live from config so packs can retune them.
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
            // Block update so the client goggle/status sees substrate changes immediately.
            markUpdated();
        }
    };
    /** Buffer that simulated loot lands in, so it can be funnelled out by hoppers or Create funnels. */
    private final ItemStackHandler inventory = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Any change may have made room for the batch that last failed to fit, so let it retry.
            outputBlocked = false;
            if (level != null && !level.isClientSide) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };
    /**
     * Set when a rolled batch would not fit even though the buffer had a free slot. Without it the
     * chamber sits at full progress drawing its whole compute allowance and starves the array.
     */
    private boolean outputBlocked = false;
    /**
     * Stall state at the last check, so the cue fires on the transition rather than every tick.
     * Null means "not yet observed", keeping a chamber that loads in already stalled quiet.
     */
    private StallReason lastStallReason = null;
    /** Single capability face: push substrate in, pull loot out, never the reverse. */
    private final ChamberItemHandler itemHandler = new ChamberItemHandler(substrate, inventory);

    public SimulationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMULATION_CHAMBER.get(), pos, state);
    }

    /**
     * The substrate buffer, for the screen's own slot. Not {@link #getItemHandler()}: that face
     * refuses to hand substrate back, which is right for automation and wrong for a player who
     * loaded the wrong tier.
     */
    public ItemStackHandler getSubstrateHandler() {
        return substrate;
    }

    /** The finished-loot buffer, for the screen's output grid. */
    public ItemStackHandler getOutputHandler() {
        return inventory;
    }

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
     * Pulls the loaded substrate back out by hand. The item handler refuses to give it to automation,
     * so without this the only way to change tiers is to break the block.
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

    /** Pops both buffers into the world when the chamber is broken. */
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
        // finish faster than a zombie's, since the same cost is paid by a much larger per-tick draw.
        return jobCost() * Math.max(0.25f, mult) * demandMultiplier();
    }

    /**
     * Rated compute draw per tick, scaled by what is being simulated: finer substrate and boss-grade
     * subjects cost proportionally more, so array size decides what can be simulated and not just how
     * many chambers can be fed. Training is charged at the base rate, no substrate involved yet.
     */
    public float getComputeDemand() {
        // A chamber held off by redstone asks for nothing, so the budget goes to the ones working.
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
     * Compute above the rated draw counts at a discount, so a big array speeds a chamber up without
     * beating the throughput of building another one.
     */
    private float effectiveProgress(float supplied) {
        float rated = getComputeDemand();
        if (rated <= 0f || supplied <= rated) {
            return supplied;
        }
        float efficiency = SimulacraConfig.OVERCLOCK_EFFICIENCY.get().floatValue();
        return rated + (supplied - rated) * efficiency;
    }

    public boolean canSimulate() {
        return stallReason() == StallReason.NONE;
    }

    public boolean isStalled() {
        return getMode() == Mode.SIMULATING && !canSimulate();
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    /** Called from the screen's button. */
    public void cycleRedstoneMode() {
        RedstoneMode[] all = RedstoneMode.values();
        redstoneMode = all[(redstoneMode.ordinal() + 1) % all.length];
        setChanged();
        markUpdated();
    }

    public boolean isRedstoneBlocked() {
        if (redstoneMode == RedstoneMode.IGNORED || level == null) {
            return false;
        }
        return level.hasNeighborSignal(worldPosition);
    }

    /** The first blocker in priority order, or NONE when the chamber can run. */
    public StallReason stallReason() {
        // First: a chamber told to stop is stopped, whatever else is wrong with it.
        if (isRedstoneBlocked()) {
            return StallReason.REDSTONE;
        }
        // A model whose mob no longer exists (its mod was removed) can never print anything, so
        // report it rather than draw compute against a job that must always fail.
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
     * Whether the buffer could take any more items. Counting only empty slots would call nine slots
     * holding one rotten flesh each "full".
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
                    // A silent chamber looks the same working as stalled, so training gets a cue.
                    // Offset by position so a bank of chambers does not churn in lockstep.
                    if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), AMBIENT_INTERVAL) == 0) {
                        playSound(ModSounds.CHAMBER_TRAINING.get(), 0.2f, 0.9f);
                    }
                    progress += applied;
                    if (progress >= trainCost()) {
                        // Carry the overshoot: an overclocked chamber would otherwise bin most of
                        // the last tick.
                        progress -= trainCost();
                        DataMatrixItem.setTrained(model, true);
                        DataMatrixItem.setProgress(model, 0f);
                        // Payoff of a long wait, so Create's confirm chime rather than machine noise.
                        AllSoundEvents.CONFIRM.playOnServer(level, worldPosition, 0.6f, 1.0f);
                    }
                }
                case SIMULATING -> {
                    if (canSimulate()) {
                        float cost = currentCost();
                        progress += applied;
                        if (progress >= cost) {
                            if (runJob()) {
                                progress -= cost;
                            } else {
                                // Loot did not fit. Hold at full rather than spilling; runJob has
                                // latched outputBlocked, so demand drops to zero until the buffer
                                // changes and the job resumes from here.
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
     * Rolls one batch of loot and, only if all of it fits, commits it and spends one substrate.
     * Returns false leaving everything untouched when the mob is unresolvable or the loot would not
     * fit, so the caller stalls instead of voiding drops.
     */
    private boolean runJob() {
        if (!(level instanceof ServerLevel server) || !canSimulate()) {
            return false;
        }
        EntityType<?> type = resolveBoundType();
        if (type == null) {
            // The mob's mod is gone; the model is stranded. Treat as a no-op job.
            return false;
        }
        List<ItemStack> drops = new ArrayList<>();
        // A low-grade model can botch the print. A failed job spends the compute and substrate like
        // any other but produces a Corrupted Imprint, and still learns from the failure.
        boolean success = server.random.nextFloat() < DataMatrixItem.accuracy(model);
        if (success) {
            // Substrate grade sets how many rolls the print is worth. Output is Predictions, not raw
            // loot: one Prediction is one roll of the subject's table, and the Loot Fabricator decides
            // later what shape that value takes. Self-Aware models get an extra roll.
            SubstrateTier tier = currentTier();
            int rolls = tier.lootRolls();
            if (DataMatrixItem.getModelTier(model) >= 4) {
                rolls += SimulacraConfig.SELF_AWARE_BONUS_ROLLS.get();
            }
            ItemStack predictions = new ItemStack(ModItems.PREDICTION.get(), rolls);
            PredictionItem.setSubject(predictions, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            drops.add(predictions);
            // Boss subjects can also print a catalyst, the renewable route to pristine blanks.
            if (type.is(Tags.EntityTypes.BOSSES)
                    && server.random.nextFloat() < SimulacraConfig.CATALYST_CHANCE.get().floatValue()) {
                drops.add(new ItemStack(ModItems.RESONANT_CATALYST.get()));
            }
        }
        if (!drainFits(drops)) {
            // Latch until the buffer changes. stallReason then reports OUTPUT_FULL, dropping this
            // chamber's demand to zero instead of holding the array hostage.
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
            // Corrupted Imprints are inserted after the fit check on purpose, and any remainder is
            // voided rather than stalling. A Loot Fabricator takes Predictions only, so the normal
            // build filters them out and leaves corrupted ones to pile up in the nine output slots
            // until the chamber deadlocks. Losing one is recoverable; deadlock is not.
            ItemHandlerHelper.insertItemStacked(inventory,
                    new ItemStack(ModItems.CORRUPTED_IMPRINT.get()), false);
        }
        substrate.extractItem(0, SimulacraConfig.SUBSTRATE_PER_JOB.get(), false);
        // The press sound pitched well down: a heavier, slower relative of the Fabricator's strike.
        AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(server, worldPosition, 0.35f, 0.65f);
        // Learning by doing: a trained model keeps grading up off its own jobs.
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
     * The loot table a simulation of this mob should roll: a Simulacra override at
     * {@code simulacra:simulation/<mob path>} if present, else the mob's own table.
     *
     * <p>The override exists because some mobs drop nothing through a loot table. The Wither's nether
     * star and the dragon's rewards come from {@code dropCustomDeathLoot} and hardcoded death logic,
     * so rolling the vanilla table alone yields nothing. Datapacks can override any mob.
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
     * Rolls the bound mob's live loot table, so drops other mods inject into that mob's pool are
     * simulated too. The throwaway entity exists only to satisfy the loot context's required
     * THIS_ENTITY parameter; it is never added to the world and is discarded after the roll.
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
        // The kill is attributed to a fake player, or every killed_by_player-gated drop is silently
        // skipped: blaze rods, wither skeleton skulls, zombie iron.
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
     * Plays the stall cue once, on the tick a running chamber first becomes blocked. Quiet, and not a
     * loop: the real signal is the working sound stopping, this is only a nudge to look.
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
     * A chamber is driven by its controller's tick, so a broken controller or cut cable stops
     * supplyCompute arriving and would leave the viewport lit forever. This notices and goes dark.
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
        // Finished work sitting in the buffer, so a chamber backing up is visible without opening it.
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
        // Synced as well as saved: the client goggle/status reads substrate and buffer state.
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
        // floorMod rather than a bare index: an out-of-range ordinal from another version would
        // otherwise throw on load and take the chunk with it.
        RedstoneMode[] modes = RedstoneMode.values();
        redstoneMode = modes[Math.floorMod(tag.getByte("Redstone"), modes.length)];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag, registries);
    }
}
