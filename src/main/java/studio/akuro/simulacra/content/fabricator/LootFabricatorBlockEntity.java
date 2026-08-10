package studio.akuro.simulacra.content.fabricator;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.SimulacraConfig;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.index.ModBlockEntities;

import java.util.List;
import java.util.Optional;

/**
 * Turns Predictions into a chosen drop.
 *
 * <p>Unlike the Simulation Chamber this is a physical machine rather than a thinking one, so it runs
 * on rotation instead of compute: the array decides what you can simulate, the shaft decides how fast
 * you can stamp the results out. That also gives players a second, separate thing to plumb rather
 * than routing the same utility everywhere.
 *
 * <p>It runs in one of two modes, and the default is the useful one. With no drop chosen it simply
 * rolls the subject's loot table, spending one Prediction per roll — which is exactly what a Prediction
 * is defined to be, so a freshly placed Fabricator fed Predictions works with no configuring at all.
 * Choosing a drop in its screen switches to the priced mode: pay the rarity-derived cost and get that
 * item every time.
 *
 * <p>The drop can be chosen two ways. A ghost slot in the screen is one, but a ghost slot can only
 * ever be set by a player standing at the machine, which left no way for a build to decide what a
 * Fabricator makes. So it also takes a Create filter, and a filter in the slot outranks the screen.
 *
 * <p>It holds a single stack of work at a time, like Create's processing blocks, so the intended build
 * is a Simulation Chamber feeding it rather than a player topping it up.
 */
public class LootFabricatorBlockEntity extends KineticBlockEntity
        implements IHaveGoggleInformation, net.minecraft.world.MenuProvider {

    /** Four by four, matching the block of output slots Hostile Neural Networks' fabricator uses. */
    private static final int OUTPUT_SLOTS = 16;

    private ItemStack target = ItemStack.EMPTY;
    private float progress = 0f;

    /**
     * Which drop the installed filter picks out of the loaded subject's table.
     *
     * <p>Derived, never saved: the filter item is the state, and re-deriving it on load costs one
     * lookup against an index that is cached anyway. It is still sent to clients, because working it
     * out needs the loot table and only the server has that.
     */
    private ItemStack filterTarget = ItemStack.EMPTY;
    private boolean filterResolved = false;

    /**
     * The Predictions being worked through: one stack, no more, the way Create's processing blocks
     * hold a single stack of work rather than acting as storage. Keeping it to a stack is what makes a
     * Simulation Chamber feeding this thing the natural build — the machine is a step in a line, not a
     * hopper you top up and walk away from.
     */
    private final ItemStackHandler predictions = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof PredictionItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            // A different subject drops different things, so the filter has to be read against it again.
            filterResolved = false;
            setChanged();
        }
    };

    /**
     * A Create filter naming what to make, outranking the screen's selection.
     *
     * <p>One filter, never a stack: a second one would be dead weight the machine ignores.
     */
    private final ItemStackHandler filter = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            filterResolved = false;
            setChanged();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FabricatorItemHandler itemHandler =
            new FabricatorItemHandler(predictions, filter, output);

    public LootFabricatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public LootFabricatorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LOOT_FABRICATOR.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** Backing handlers, exposed so the menu can wrap them in slots. */
    public ItemStackHandler getPredictions() {
        return predictions;
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public ItemStackHandler getFilter() {
        return filter;
    }

    /** Predictions of this subject the machine currently holds, for the screen's readout. */
    public int countPredictionsFor(ItemStack sample) {
        int total = 0;
        for (int slot = 0; slot < predictions.getSlots(); slot++) {
            ItemStack held = predictions.getStackInSlot(slot);
            if (!held.isEmpty() && studio.akuro.simulacra.content.data.PredictionItem.sameSubject(held, sample)) {
                total += held.getCount();
            }
        }
        return total;
    }

    /** How many drops the picker can show at once. */
    public static final int CANDIDATE_SLOTS = 18;

    /**
     * Everything the loaded Predictions' subject can drop, commonest first.
     *
     * <p>This is what lets the screen offer a choice instead of demanding one. Asking a player to
     * right-click with the exact item they want means knowing a mob's drop table from memory, which is
     * a wiki lookup wearing a game mechanic's clothes. The loot table already knows, so show it.
     */
    public List<ItemStack> candidateDrops() {
        Optional<EntityType<?>> type = subjectOfLoadedPredictions();
        if (type.isEmpty() || !(level instanceof ServerLevel server)) {
            return List.of();
        }
        return LootValueIndex.yields(server, type.get(), simulationTable(server, type.get()))
                .entrySet().stream()
                // Commonest drops first: the cheap staples are what a player reaches for most, and
                // it keeps the ordering stable between openings.
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(CANDIDATE_SLOTS)
                .map(e -> new ItemStack(e.getKey()))
                .toList();
    }

    /** Price of one specific item against the loaded Predictions' subject. */
    public Optional<Integer> priceOf(net.minecraft.world.item.Item item) {
        Optional<EntityType<?>> type = subjectOfLoadedPredictions();
        if (type.isEmpty() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return LootValueIndex.price(server, type.get(), simulationTable(server, type.get()), item);
    }

    private Optional<EntityType<?>> subjectOfLoadedPredictions() {
        ItemStack sample = anyPrediction();
        return sample.isEmpty() ? Optional.empty() : PredictionItem.resolveSubject(sample);
    }

    /**
     * How much work a tick of rotation is worth, using Create's own Mechanical Press curve:
     * {@code lerp(clamp(|rpm| / 512), 1, 60)}.
     *
     * <p>Copied deliberately rather than invented. It means the Fabricator speeds up with rotation on
     * the same curve as every other Create machine a player has already learned, including the cap at
     * 512 RPM — a plain multiple of RPM, which is what this used to be, keeps accelerating forever and
     * feels foreign next to a press doing the same job.
     */
    /**
     * Work done per tick at a given speed: rotation over eight, and nothing else.
     *
     * <p>This deliberately does not follow the Mechanical Press's own curve. That curve is concave and
     * rounds to whole ticks, which makes throughput a lumpy function of speed — the ratios between an
     * array, its chambers and this machine came out whole at 32 and 64 RPM and ragged everywhere else.
     *
     * <p>Straight proportionality makes the whole chain hold at once: four Neural Nodes drive one
     * Fabricator at any speed, and the number of Simulation Chambers between them is simply RPM over
     * 32. A player can work that out in their head at the speed they happen to be running, which is
     * worth more here than matching a curve nothing else in this mod shares.
     */
    private static float workPerTick(float rpm) {
        return Math.abs(rpm) / 8f;
    }

    /**
     * Progress toward the next item, 0 to 1.
     *
     * <p>The screen had no progress indicator at all, which for a machine sitting in a line is the one
     * thing it needs to show: without it there is no way to tell "working slowly" from "jammed".
     */
    public float getProgressFraction() {
        float work = SimulacraConfig.FABRICATOR_WORK.get().floatValue();
        return work <= 0f ? 0f : Math.min(1f, progress / work);
    }

    /** The first prediction stack held, or empty. Drives what the screen prices. */
    public ItemStack anyPrediction() {
        for (int slot = 0; slot < predictions.getSlots(); slot++) {
            ItemStack held = predictions.getStackInSlot(slot);
            if (!held.isEmpty()) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simulacra.loot_fabricator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inventory,
            net.minecraft.world.entity.player.Player player) {
        return new LootFabricatorMenu(id, inventory, this);
    }


    /**
     * What this machine stamps out. An empty stack still means "roll the table", not "make nothing".
     *
     * <p>A filter outranks the screen's selection, so a build can retarget a Fabricator no player
     * will ever open. A filter with nothing set in it is not a decision, so it leaves the machine
     * rolling exactly as if the slot were empty.
     */
    public ItemStack getTarget() {
        ItemStack held = filter.getStackInSlot(0);
        if (held.isEmpty()) {
            return target;
        }
        return isBlank(held) ? ItemStack.EMPTY : filterTarget;
    }

    /**
     * Whether a filter has anything set in it.
     *
     * <p>Create writes no components onto a filter until something is put in it, and strips them
     * again when it is emptied — which is the same test {@code FilterItemStack.of} uses to decide a
     * filter is worth reading, so a blank one is reliably distinguishable from a configured one.
     */
    private static boolean isBlank(ItemStack held) {
        return held.isComponentsPatchEmpty();
    }

    /**
     * A configured filter that matches nothing the loaded subject drops.
     *
     * <p>Kept separate from "no target" on purpose: falling back to rolling here would quietly hand
     * a build items it explicitly asked not to make, which is worse than making none.
     */
    private boolean filterUnsatisfied() {
        ItemStack held = filter.getStackInSlot(0);
        return !held.isEmpty() && !isBlank(held) && filterTarget.isEmpty();
    }

    /**
     * Read the filter against what the loaded subject can drop.
     *
     * <p>A Create filter is a predicate rather than a name, so the drop list is tested against it
     * instead of the filter being asked what it holds. That is also what makes an attribute filter
     * work here at all, and it costs nothing: the drop list is already derived and cached for the
     * screen's palette.
     */
    private void resolveFilter() {
        filterResolved = true;
        ItemStack held = filter.getStackInSlot(0);
        ItemStack resolved = ItemStack.EMPTY;
        if (!held.isEmpty() && !isBlank(held)) {
            // On a copy: reading a filter trims components off the stack it is handed.
            FilterItemStack predicate = FilterItemStack.of(held.copy());
            for (ItemStack drop : candidateDrops()) {
                if (predicate.test(level, drop)) {
                    resolved = drop.copyWithCount(1);
                    break;
                }
            }
        }
        if (ItemStack.matches(filterTarget, resolved)) {
            return;
        }
        filterTarget = resolved;
        progress = 0f;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Sets what this machine stamps out. Pass an empty stack to clear it. */
    public void setTarget(ItemStack stack) {
        this.target = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.progress = 0f;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        if (!filterResolved) {
            resolveFilter();
        }

        // A usable slot means every precondition for stamping is met at once: a target is set, the
        // shaft is turning, and we hold a prediction whose subject actually drops that item. That is
        // exactly what the lit front should mean, so derive both from it rather than letting the
        // display drift from the work.
        float speed = Math.abs(getSpeed());
        int slot = speed <= 0f ? -1 : firstUsablePrediction();
        updateLit(slot >= 0);
        if (slot < 0) {
            return;
        }

        progress += workPerTick(speed);
        if (progress < SimulacraConfig.FABRICATOR_WORK.get().floatValue()) {
            return;
        }

        if (fabricate(slot)) {
            progress = 0f;
            AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, worldPosition, 0.4f, 1.15f);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else {
            // Hold at the top rather than spinning the counter up forever while blocked.
            progress = SimulacraConfig.FABRICATOR_WORK.get().floatValue();
        }
    }


    /**
     * Lights the front while the machine can actually stamp, which is the convention every other
     * machine in the mod follows.
     *
     * <p>This is a {@link KineticBlockEntity}, so the state change goes through Create's helper: a
     * plain {@code setBlock} can cost a kinetic block its network association, which would surface as
     * the press stalling for no visible reason.
     */
    private void updateLit(boolean lit) {
        BlockState state = getBlockState();
        if (state.hasProperty(LootFabricatorBlock.LIT) && state.getValue(LootFabricatorBlock.LIT) != lit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(LootFabricatorBlock.LIT, lit));
        }
    }

    /**
     * Whether the machine is filtering to one drop, or rolling the table for whatever comes out.
     *
     * <p>No selection is not "off". A Prediction is defined as one roll of its subject's loot table,
     * so spending one to get exactly that is the machine's most honest behaviour and its natural
     * default — a freshly placed Fabricator fed Predictions simply works, with no configuring. Picking
     * a drop is the premium mode: pay the rarity-derived price to guarantee an item instead of taking
     * whatever the roll gives.
     */
    public boolean isFiltered() {
        return !getTarget().isEmpty();
    }

    /** First prediction slot this machine can currently spend. */
    private int firstUsablePrediction() {
        if (filterUnsatisfied()) {
            return -1;
        }
        for (int slot = 0; slot < predictions.getSlots(); slot++) {
            ItemStack stack = predictions.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // Unfiltered only needs a subject to roll. Filtered needs that subject to drop the target.
            boolean usable = isFiltered()
                    ? priceFor(stack).isPresent()
                    : PredictionItem.resolveSubject(stack).isPresent();
            if (usable) {
                return slot;
            }
        }
        return -1;
    }

    /** How many predictions of this subject one target item costs, if that mob drops it at all. */
    public Optional<Integer> priceFor(ItemStack prediction) {
        ItemStack goal = getTarget();
        if (goal.isEmpty() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        Optional<EntityType<?>> type = PredictionItem.resolveSubject(prediction);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        LootTable table = simulationTable(server, type.get());
        return LootValueIndex.price(server, type.get(), table, goal.getItem());
    }

    /** Mirrors the chamber's lookup so both machines agree on what a mob drops. */
    private static LootTable simulationTable(ServerLevel server, EntityType<?> type) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        ResourceKey<LootTable> override = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, "simulation/" + id.getPath()));
        LootTable table = server.getServer().reloadableRegistries().getLootTable(override);
        return table != LootTable.EMPTY ? table : server.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
    }

    private boolean fabricate(int slot) {
        ItemStack prediction = predictions.getStackInSlot(slot);
        return isFiltered() ? fabricateChosen(slot, prediction) : fabricateRolled(slot, prediction);
    }

    /** Spend the rarity-derived price to guarantee one of the chosen drop. */
    private boolean fabricateChosen(int slot, ItemStack prediction) {
        Optional<Integer> price = priceFor(prediction);
        if (price.isEmpty() || prediction.getCount() < price.get()) {
            return false;
        }
        ItemStack produced = getTarget().copyWithCount(1);
        if (!fitsAll(List.of(produced))) {
            return false;
        }
        net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(output, produced, false);
        predictions.extractItem(slot, price.get(), false);
        return true;
    }

    /**
     * Spend one Prediction on one roll of the subject's table and keep whatever it gives — which for
     * a table that does not always drop can be nothing at all. That is the trade: the roll is cheap
     * and honest, filtering costs more and is certain.
     */
    private boolean fabricateRolled(int slot, ItemStack prediction) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        Optional<EntityType<?>> type = PredictionItem.resolveSubject(prediction);
        if (type.isEmpty()) {
            return false;
        }
        List<ItemStack> rolled = rollLoot(server, type.get());
        // Hold rather than void: a roll that cannot fit waits for room, or the Prediction is spent
        // for nothing.
        if (!fitsAll(rolled)) {
            return false;
        }
        for (ItemStack drop : rolled) {
            if (!drop.isEmpty()) {
                net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(output, drop, false);
            }
        }
        predictions.extractItem(slot, 1, false);
        return true;
    }

    /**
     * One roll of the subject's loot table, attributed to a fake player so killed_by_player drops
     * behave as they do in the Simulation Chamber. The two machines have to agree about what a mob
     * drops, or the same Prediction would be worth different things depending where it is spent.
     */
    private List<ItemStack> rollLoot(ServerLevel server, EntityType<?> type) {
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

    private boolean fitsAll(List<ItemStack> stacks) {
        ItemStackHandler scratch = new ItemStackHandler(output.getSlots());
        for (int slot = 0; slot < output.getSlots(); slot++) {
            scratch.setStackInSlot(slot, output.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!net.neoforged.neoforge.items.ItemHandlerHelper
                    .insertItemStacked(scratch, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }


    public void dropContents() {
        if (level == null) {
            return;
        }
        for (ItemStackHandler handler : List.of(predictions, filter, output)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    net.minecraft.world.level.block.Block.popResource(level, worldPosition, stack);
                }
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Predictions", predictions.serializeNBT(registries));
        tag.put("Filter", filter.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putFloat("Progress", progress);
        if (!target.isEmpty()) {
            tag.put("Target", target.save(registries));
        }
        // Sent, not saved. Working out what a filter picks needs the loot table, which only the
        // server has, but the screen and the goggles both have to show what is actually being made.
        if (clientPacket && !filterTarget.isEmpty()) {
            tag.put("FilterTarget", filterTarget.save(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Predictions")) {
            predictions.deserializeNBT(registries, tag.getCompound("Predictions"));
        }
        if (tag.contains("Filter")) {
            filter.deserializeNBT(registries, tag.getCompound("Filter"));
        }
        if (tag.contains("Output")) {
            output.deserializeNBT(registries, tag.getCompound("Output"));
        }
        progress = tag.getFloat("Progress");
        target = tag.contains("Target")
                ? ItemStack.parse(registries, tag.getCompound("Target")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        if (clientPacket) {
            filterTarget = tag.contains("FilterTarget")
                    ? ItemStack.parse(registries, tag.getCompound("FilterTarget")).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
        } else {
            // Loaded from disk: the filter is the state, so read it again rather than trusting a
            // derived value that a datapack reload could have invalidated.
            filterResolved = false;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(
                Component.translatable("tooltip.simulacra.fabricator").withStyle(ChatFormatting.GRAY)));
        // A filter that matches nothing is not the same as no choice at all, and looks identical
        // from outside — the machine simply sits there — so name it.
        if (filterUnsatisfied()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("tooltip.simulacra.fabricator_filter_no_match")
                            .withStyle(ChatFormatting.RED)));
            return true;
        }
        ItemStack goal = getTarget();
        if (goal.isEmpty()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("tooltip.simulacra.fabricator_no_target").withStyle(ChatFormatting.RED)));
            return true;
        }
        tooltip.add(Component.literal("    ").append(
                Component.translatable("tooltip.simulacra.fabricator_target", goal.getHoverName())
                        .withStyle(ChatFormatting.AQUA)));
        int slot = firstUsablePrediction();
        if (slot >= 0) {
            priceFor(predictions.getStackInSlot(slot)).ifPresent(price ->
                    tooltip.add(Component.literal("    ").append(
                            Component.translatable("tooltip.simulacra.fabricator_price", price)
                                    .withStyle(ChatFormatting.GOLD))));
        } else {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("tooltip.simulacra.fabricator_no_match").withStyle(ChatFormatting.RED)));
        }
        return true;
    }
}
