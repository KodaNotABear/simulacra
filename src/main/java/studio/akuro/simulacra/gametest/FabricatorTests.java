package studio.akuro.simulacra.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import studio.akuro.simulacra.content.fabricator.LootFabricatorMenu;

import java.util.List;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlock;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlockEntity;
import studio.akuro.simulacra.index.ModBlocks;
import studio.akuro.simulacra.index.ModItems;

/**
 * Tests for the Loot Fabricator, driven by a real Create creative motor so the kinetic hookup is
 * exercised rather than stubbed.
 *
 * <p>The lit front is the only feedback this machine gives while working — it has no progress bar and
 * its press stroke is brief — so "does it light up when it should" is worth asserting. It shipped
 * declaring the property, with four lit blockstate variants and an emissive lit model, while nothing
 * ever set it; that is invisible in a build and easy to reintroduce.
 */
@GameTestHolder(Simulacra.MOD_ID)
@PrefixGameTestTemplate(false)
public class FabricatorTests {

    private static final String PLATFORM = "platform";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    /**
     * A Create list filter naming the given items, built the way Create's own filter screen builds
     * one: the chosen items go into the FILTER_ITEMS component and nothing else is written.
     */
    private static ItemStack filter(ItemStack... contents) {
        ItemStack stack = AllItems.FILTER.asStack();
        stack.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(contents)));
        return stack;
    }

    private static ItemStack prediction(String mobId, int count) {
        ItemStack stack = new ItemStack(ModItems.PREDICTION.get(), count);
        CompoundTag tag = new CompoundTag();
        tag.putString("Mob", mobId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /**
     * A fabricator facing north — so its shaft face is south — with a creative motor behind it
     * pointing north into that face.
     */
    private static LootFabricatorBlockEntity drivenFabricator(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.LOOT_FABRICATOR.get().defaultBlockState()
                .setValue(LootFabricatorBlock.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(POS.south(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(POS);
    }

    /** Turning with nothing loaded is not working: the front must stay dark. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void spinningButEmptyStaysDark(GameTestHelper helper) {
        drivenFabricator(helper);
        helper.runAfterDelay(20, () -> {
            helper.assertBlockProperty(POS, LootFabricatorBlock.LIT, false);
            helper.succeed();
        });
    }

    /**
     * With Predictions loaded and no drop chosen, the machine rolls the subject's table rather than
     * sitting idle. No selection is the default state of a freshly placed Fabricator, so if it means
     * "off" the machine appears broken until the player finds the picker.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 700)
    public static void unfilteredRollsTheTable(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        if (fabricator.isFiltered()) {
            throw new GameTestAssertException("a fabricator with no chosen drop reported itself filtered");
        }
        helper.succeedWhen(() -> {
            // A roll can legitimately produce nothing, so wait for Predictions to be spent rather
            // than for a specific item to appear.
            if (fabricator.getPredictions().getStackInSlot(0).getCount() >= 64) {
                throw new GameTestAssertException("no Predictions spent: the machine is idling unfiltered");
            }
        });
    }

    /** Choosing a drop switches out of rolling and into the guaranteed, priced mode. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void choosingADropFiltersIt(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 16), false);
        fabricator.setTarget(new ItemStack(Items.ROTTEN_FLESH));
        if (!fabricator.isFiltered()) {
            throw new GameTestAssertException("a chosen drop did not put the machine in filtered mode");
        }
        fabricator.setTarget(ItemStack.EMPTY);
        if (fabricator.isFiltered()) {
            throw new GameTestAssertException("clearing the drop did not return it to rolling");
        }
        helper.succeed();
    }

    /** Driven, targeted, and stocked with usable predictions: the front must light. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void workingFabricatorLightsUp(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.setTarget(new ItemStack(Items.ROTTEN_FLESH));
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.succeedWhen(() -> helper.assertBlockProperty(POS, LootFabricatorBlock.LIT, true));
    }

    /**
     * A target the held predictions cannot produce is not workable, so the machine must stay dark
     * rather than implying it is busy. A zombie does not drop diamonds.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void impossibleTargetStaysDark(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.setTarget(new ItemStack(Items.DIAMOND));
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.runAfterDelay(20, () -> {
            helper.assertBlockProperty(POS, LootFabricatorBlock.LIT, false);
            helper.succeed();
        });
    }


    /**
     * The picker must offer the loaded subject's drops, and selecting one must set the target.
     *
     * <p>This is the failure that made the machine look completely broken: with an empty picker and
     * nothing selectable, opening the Fabricator presented a player with nothing they could do, and no
     * amount of staring at it explained why.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void pickerOffersTheSubjectsDropsAndSelects(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 16), false);

        List<ItemStack> drops = fabricator.candidateDrops();
        if (drops.isEmpty()) {
            throw new GameTestAssertException("picker offered nothing for a loaded zombie Prediction");
        }
        boolean flesh = drops.stream().anyMatch(d -> d.is(Items.ROTTEN_FLESH));
        if (!flesh) {
            throw new GameTestAssertException(
                    "zombie drops did not include rotten flesh; got " + drops.stream()
                            .map(d -> d.getItem().toString()).toList());
        }
        for (ItemStack drop : drops) {
            if (fabricator.priceOf(drop.getItem()).isEmpty()) {
                throw new GameTestAssertException("no price for offered drop " + drop.getItem());
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LootFabricatorMenu menu = new LootFabricatorMenu(1, player.getInventory(), fabricator);
        menu.broadcastChanges();
        int slot = LootFabricatorMenu.FIRST_CANDIDATE;
        menu.clicked(slot, 0, ClickType.PICKUP, player);
        ItemStack offered = menu.slots.get(slot).getItem();
        if (fabricator.getTarget().isEmpty() || !fabricator.getTarget().is(offered.getItem())) {
            throw new GameTestAssertException("clicking a picker slot did not set the target; target is "
                    + fabricator.getTarget());
        }
        helper.succeed();
    }

    /**
     * Clicking the preview clears the selection, which is how Hostile Neural Networks' fabricator
     * works and the only way to stop this machine short of breaking it. Picking a drop sets it;
     * picking a different one replaces it.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void previewClearsTheSelection(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 16), false);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LootFabricatorMenu menu = new LootFabricatorMenu(1, player.getInventory(), fabricator);
        menu.broadcastChanges();

        menu.clicked(LootFabricatorMenu.FIRST_CANDIDATE, 0, ClickType.PICKUP, player);
        if (fabricator.getTarget().isEmpty()) {
            throw new GameTestAssertException("picking a drop did not set a target");
        }
        // The preview must mirror the choice, or the player cannot see what the machine is making.
        ItemStack preview = menu.slots.get(LootFabricatorMenu.PREVIEW_SLOT).getItem();
        if (preview.isEmpty() || !ItemStack.isSameItem(preview, fabricator.getTarget())) {
            throw new GameTestAssertException("preview does not show the selected drop; shows " + preview);
        }

        menu.clicked(LootFabricatorMenu.PREVIEW_SLOT, 0, ClickType.PICKUP, player);
        if (!fabricator.getTarget().isEmpty()) {
            throw new GameTestAssertException("clicking the preview left the target set to "
                    + fabricator.getTarget());
        }
        helper.succeed();
    }

    /**
     * The machine holds one stack of work, like Create's processing blocks. If this ever grows into a
     * multi-slot buffer it stops being a step in a line and becomes a chest that happens to stamp
     * things, and a Simulation Chamber feeding it stops being the obvious build.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void holdsOnlyOneStackOfWork(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        int slots = fabricator.getPredictions().getSlots();
        if (slots != 1) {
            throw new GameTestAssertException("Prediction buffer should be a single stack, has "
                    + slots + " slots");
        }
        ItemStack leftover = fabricator.getPredictions()
                .insertItem(0, prediction("minecraft:zombie", 64), false);
        ItemStack overflow = fabricator.getPredictions()
                .insertItem(0, prediction("minecraft:zombie", 64), false);
        if (!leftover.isEmpty()) {
            throw new GameTestAssertException("a full stack should fit; " + leftover.getCount() + " bounced");
        }
        if (overflow.getCount() != 64) {
            throw new GameTestAssertException("a second stack should not fit, but "
                    + (64 - overflow.getCount()) + " went in");
        }
        helper.succeed();
    }

    /** Progress has to advance while working, or the screen's bar is decoration. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void progressAdvancesWhileWorking(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.setTarget(new ItemStack(Items.ROTTEN_FLESH));
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.succeedWhen(() -> {
            if (fabricator.getProgressFraction() <= 0f) {
                throw new GameTestAssertException("progress has not advanced");
            }
        });
    }

    /**
     * The machine must be usable through its item capability, which is what a funnel, hopper or belt
     * actually talks to — Predictions in one side, finished drops out the other.
     *
     * <p>Every other test here reaches into the block entity's handlers directly, which is precisely
     * why they all passed while the capability was never registered at all and no funnel could move a
     * single item. Going through the capability is the only way to test the way the machine is played.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void funnelsCanFeedAndDrainIt(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        BlockPos absolute = helper.absolutePos(POS);
        IItemHandler face = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, absolute, null);
        if (face == null) {
            throw new GameTestAssertException("no item capability on the Loot Fabricator: "
                    + "nothing can feed it or drain it");
        }

        ItemStack leftover = ItemHandlerHelper.insertItem(face, prediction("minecraft:zombie", 8), false);
        if (!leftover.isEmpty()) {
            throw new GameTestAssertException("the item face refused Predictions; " + leftover.getCount()
                    + " bounced");
        }
        if (fabricator.getPredictions().getStackInSlot(0).getCount() != 8) {
            throw new GameTestAssertException("Predictions inserted through the face did not reach the buffer");
        }

        // Anything that is not a Prediction must be refused rather than silently swallowed.
        ItemStack junk = ItemHandlerHelper.insertItem(face, new ItemStack(Items.DIAMOND, 1), false);
        if (junk.isEmpty()) {
            throw new GameTestAssertException("the item face accepted a non-Prediction");
        }

        // Finished output has to be extractable, or an outbound funnel has nothing to take.
        fabricator.getOutput().setStackInSlot(0, new ItemStack(Items.ROTTEN_FLESH, 4));
        ItemStack drained = ItemStack.EMPTY;
        for (int slot = 0; slot < face.getSlots() && drained.isEmpty(); slot++) {
            drained = face.extractItem(slot, 4, false);
        }
        if (drained.isEmpty() || !drained.is(Items.ROTTEN_FLESH)) {
            throw new GameTestAssertException("nothing could be pulled out of the item face");
        }
        helper.succeed();
    }

    /** Hand-loading must still work: automation is the efficient path, not the only one. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void predictionsCanBeLoadedByHand(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LootFabricatorMenu menu = new LootFabricatorMenu(1, player.getInventory(), fabricator);
        Slot input = menu.slots.get(LootFabricatorMenu.INPUT_SLOT);
        if (!input.mayPlace(prediction("minecraft:zombie", 1))) {
            throw new GameTestAssertException("the Prediction input slot refuses Predictions");
        }
        if (input.mayPlace(new ItemStack(Items.DIAMOND))) {
            throw new GameTestAssertException("the Prediction input slot accepts non-Predictions");
        }
        helper.succeed();
    }

    /**
     * Predictions must actually be spent and the chosen drop produced.
     *
     * <p>Given a long window on purpose: the creative motor drives at 16 RPM, which on Create's press
     * curve is 2 work a tick, so a full cycle is a few hundred ticks. Testing at a realistic speed
     * beats shortening the cycle and testing something the game never does.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 700)
    public static void fabricatorStampsTheChosenDrop(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.setTarget(new ItemStack(Items.ROTTEN_FLESH));
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.succeedWhen(() -> {
            ItemStack out = fabricator.getOutput().getStackInSlot(0);
            if (out.isEmpty() || !out.is(Items.ROTTEN_FLESH)) {
                throw new GameTestAssertException("fabricator has not produced rotten flesh yet");
            }
        });
    }

    /**
     * An extractor that picks a slot by looking at it must never be able to get stuck on the
     * Prediction input.
     *
     * <p>This is the shape of Create's own ItemHelper.extract: survey every slot with
     * getStackInSlot, then pull from the first that looks worth pulling. When the input slot
     * advertised its Prediction but refused extraction, an outbound funnel drained the output
     * happily until the moment a Prediction arrived, then jammed on that slot for good - which
     * looked like the machine producing exactly once and never again.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void anExtractorCannotJamOnThePredictionSlot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.LOOT_FABRICATOR.get());
        LootFabricatorBlockEntity fabricator =
                (LootFabricatorBlockEntity) helper.getBlockEntity(pos);

        // A Prediction sitting in the input, and something waiting in the output.
        ItemStack prediction = new ItemStack(ModItems.PREDICTION.get(), 4);
        PredictionItem.setSubject(prediction, "minecraft:zombie");
        fabricator.getPredictions().insertItem(0, prediction, false);
        fabricator.getOutput().insertItem(0, new ItemStack(Items.ROTTEN_FLESH, 3), false);

        IItemHandler face = fabricator.getItemHandler();
        // Survey then pull, the way Create does it.
        ItemStack pulled = ItemStack.EMPTY;
        for (int slot = 0; slot < face.getSlots(); slot++) {
            if (face.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            pulled = face.extractItem(slot, 64, false);
            break;
        }
        if (pulled.isEmpty()) {
            throw new GameTestAssertException(
                    "an extractor surveying this face found an item it could not pull, which is how "
                    + "an outbound funnel jams permanently");
        }
        helper.succeed();
    }

    /**
     * A filter must decide what comes out, or the machine is only ever configurable by a player
     * standing at its screen.
     *
     * <p>Cow rather than zombie on purpose: a cow always drops beef as well as leather, so a single
     * scrap of beef in the output is proof the filter was ignored and the table was simply rolled.
     * Filtered to leather, nothing else may appear.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 700)
    public static void aFilterDecidesWhatIsMade(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getFilter().insertItem(0, filter(new ItemStack(Items.LEATHER)), false);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:cow", 64), false);

        // 1200 work at 2 a tick is a 600 tick cycle, so one item is due well before this fires.
        helper.runAfterDelay(650, () -> {
            boolean made = false;
            for (int slot = 0; slot < fabricator.getOutput().getSlots(); slot++) {
                ItemStack out = fabricator.getOutput().getStackInSlot(slot);
                if (out.isEmpty()) {
                    continue;
                }
                if (!out.is(Items.LEATHER)) {
                    throw new GameTestAssertException(
                            "the filter asked for leather and the machine produced " + out);
                }
                made = true;
            }
            if (!made) {
                throw new GameTestAssertException("filtered to leather, the machine produced nothing");
            }
            helper.succeed();
        });
    }

    /**
     * An attribute filter must work too.
     *
     * <p>This is the case that pins the design down: an attribute filter names no item anywhere, so
     * the target can only come from testing the subject's drops against the filter rather than from
     * reading a list out of it. A skeleton drops bones and arrows; only one of them is an arrow.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void anAttributeFilterDecidesToo(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        ItemStack attributes = AllItems.ATTRIBUTE_FILTER.asStack();
        attributes.set(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES,
                List.of(new ItemAttribute.ItemAttributeEntry(new InTagAttribute(ItemTags.ARROWS), false)));
        fabricator.getFilter().insertItem(0, attributes, false);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:skeleton", 64), false);

        helper.runAfterDelay(5, () -> {
            if (!fabricator.getTarget().is(Items.ARROW)) {
                throw new GameTestAssertException("an attribute filter matching arrows settled on "
                        + fabricator.getTarget());
            }
            helper.succeed();
        });
    }

    /**
     * A filter with nothing set in it must leave the machine rolling.
     *
     * <p>Create's own filters match nothing when blank, which here would read as a machine that
     * stopped for no stated reason the moment a filter was slotted in. Rolling is the honest
     * default and is what an empty slot already means.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aBlankFilterStillRollsTheTable(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getFilter().insertItem(0, AllItems.FILTER.asStack(), false);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.runAfterDelay(20, () -> {
            if (fabricator.isFiltered()) {
                throw new GameTestAssertException("a filter with nothing in it reported a chosen drop");
            }
            // Lit is the only outward sign the machine is working rather than stalled.
            helper.assertBlockProperty(POS, LootFabricatorBlock.LIT, true);
            helper.succeed();
        });
    }

    /** A filter outranks the screen, and taking it out hands the decision back rather than jamming. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aFilterOverridesTheScreenSelection(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:cow", 16), false);
        fabricator.setTarget(new ItemStack(Items.BEEF));
        fabricator.getFilter().insertItem(0, filter(new ItemStack(Items.LEATHER)), false);

        helper.runAfterDelay(5, () -> {
            if (!fabricator.getTarget().is(Items.LEATHER)) {
                throw new GameTestAssertException("the screen's beef outranked the filter's leather; making "
                        + fabricator.getTarget());
            }
            fabricator.getFilter().extractItem(0, 1, false);
            if (!fabricator.getTarget().is(Items.BEEF)) {
                throw new GameTestAssertException("removing the filter did not restore the screen's choice; "
                        + "making " + fabricator.getTarget());
            }
            helper.succeed();
        });
    }

    /**
     * A filter naming something the subject never drops stops the machine rather than falling back
     * to rolling. Quietly making the wrong thing is worse than making none, and a build that asked
     * for diamonds has no use for rotten flesh.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void anUnmatchableFilterStaysDark(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        fabricator.getFilter().insertItem(0, filter(new ItemStack(Items.DIAMOND)), false);
        fabricator.getPredictions().insertItem(0, prediction("minecraft:zombie", 64), false);
        helper.runAfterDelay(20, () -> {
            helper.assertBlockProperty(POS, LootFabricatorBlock.LIT, false);
            if (!fabricator.getOutput().getStackInSlot(0).isEmpty()) {
                throw new GameTestAssertException("an unmatchable filter fell back to rolling the table");
            }
            helper.succeed();
        });
    }

    /**
     * The filter has to be settable through the block's item face, or it is only a second screen
     * control and no build is any better off. It must not be extractable through the same face,
     * though, or an outbound funnel walks off with the machine's own settings.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aFilterCanBeSetThroughTheItemFace(GameTestHelper helper) {
        LootFabricatorBlockEntity fabricator = drivenFabricator(helper);
        IItemHandler face = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS), null);
        if (face == null) {
            throw new GameTestAssertException("no item capability on the Loot Fabricator");
        }

        ItemStack leftover = ItemHandlerHelper.insertItem(face, filter(new ItemStack(Items.LEATHER)), false);
        if (!leftover.isEmpty()) {
            throw new GameTestAssertException("the item face refused a filter, so no build can retarget this machine");
        }
        if (fabricator.getFilter().getStackInSlot(0).isEmpty()) {
            throw new GameTestAssertException("a filter inserted through the face never reached the slot");
        }

        for (int slot = 0; slot < face.getSlots(); slot++) {
            ItemStack pulled = face.extractItem(slot, 64, false);
            if (!pulled.isEmpty()) {
                throw new GameTestAssertException("the face let the machine's own filter be pulled back out as "
                        + pulled);
            }
        }
        helper.succeed();
    }
}
