package studio.akuro.simulacra.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlock;
import studio.akuro.simulacra.index.ModItems;

/**
 * Storyboards for the four machines. Each scene stages on the structure of the same name under
 * {@code assets/simulacra/ponder/}.
 *
 * <p>{@code title(...)} must run first: it sets the scene id every lang key derives from. Nothing
 * here may touch the level either, since Ponder replays each storyboard against a null world during
 * lang collection to harvest text keys.
 *
 * <p>The count and order of {@code showText} calls is load-bearing: keys are numbered {@code text_1},
 * {@code text_2}, ... in execution order, so inserting a line renumbers every key after it in
 * {@code en_us.json}.
 */
public class SimulacraScenes {

    /** A Prediction bound to a subject, for scenes that show them moving between machines. */
    private static ItemStack prediction(String mobKey) {
        ItemStack stack = new ItemStack(ModItems.PREDICTION.get());
        PredictionItem.setSubject(stack, mobKey);
        return stack;
    }

    /**
     * Rotation in, compute out. Staged on {@code ponder/neural_node/single.nbt}: a large cogwheel
     * gears down into a shaft feeding one node's back face.
     */
    public static void neuralNodeSingle(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("neural_node_single", "Turning rotation into compute");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos node = util.grid().at(2, 1, 2);
        BlockPos shaft = util.grid().at(3, 1, 2);

        // Reveal the drivetrain before the machine, so the rotation reads as the input.
        scene.world().showSection(util.select().fromTo(3, 1, 1, 4, 2, 2), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().position(node), Direction.WEST);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        // LIT is only driven server-side and a Ponder scene runs in a client level, so the front
        // would stay dark. Light it to match the rotation.
        scene.world().modifyBlocks(util.select().position(node),
                state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.effects().rotationSpeedIndicator(shaft);
        scene.idle(10);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(node, Direction.EAST))
                .text("Rotation on the back shaft becomes compute");
        scene.idle(90);

        scene.overlay().showText(80)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().centerOf(shaft))
                .text("Faster rotation, more compute, more stress");
        scene.idle(90);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.WHITE)
                .placeNearTarget()
                .pointAt(util.vector().topOf(node))
                .text("One node alone makes nothing. A Mainframe Controller pools them.");
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Why you build more than one. Staged on {@code ponder/neural_node/array.nbt}: a belt drives a
     * rack of three touching nodes.
     */
    public static void neuralNodeArray(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("neural_node_array", "Building a Cognition Array");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos middle = util.grid().at(2, 1, 2);

        scene.world().showSection(util.select().fromTo(3, 1, 1, 4, 2, 3), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(2, 1, 1, 2, 1, 3), Direction.WEST);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlocks(util.select().fromTo(2, 1, 1, 2, 1, 3),
                state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, "rack",
                util.select().fromTo(2, 1, 1, 2, 1, 3), 80);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .pointAt(util.vector().topOf(middle))
                .text("Touching nodes count as one array");
        scene.idle(90);

        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 1)))
                .text("Every driven node adds compute. Bigger arrays run more efficiently.");
        scene.idle(100);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.WHITE)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(middle, Direction.WEST))
                .text("Cable the rack to a Mainframe Controller");
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Binding a matrix by killing mobs with it in the offhand. Staged on
     * {@code ponder/data_matrix.nbt}, a bare plate holding only the player, the item and a zombie.
     */
    public static void dataMatrix(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("data_matrix", "Recording a mob");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos centre = util.grid().at(2, 1, 2);
        Vec3 standing = util.vector().centerOf(2, 1, 2);

        ElementLink<EntityElement> zombie = scene.world().createEntity(level -> {
            Zombie entity = EntityType.ZOMBIE.create(level);
            entity.setPos(standing.x, standing.y - 0.5, standing.z);
            entity.setYRot(180);
            entity.yBodyRot = 180;
            entity.yHeadRot = 180;
            return entity;
        });
        scene.idle(20);

        scene.overlay().showControls(standing.add(0, 1.5, 0), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModItems.BLANK_DATA_MATRIX.get()));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .placeNearTarget()
                .pointAt(standing.add(0, 1.0, 0))
                .text("Hold a Blank Data Matrix in your off hand and kill mobs");
        scene.idle(100);

        scene.world().modifyEntity(zombie, Entity::discard);
        scene.effects().indicateSuccess(centre);
        scene.idle(15);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .pointAt(standing.add(0, 1.0, 0))
                .text("The first kill binds it. Only that mob adds data now.");
        scene.idle(100);

        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().topOf(centre))
                .text("Enough kills and it is ready to train in a Simulation Chamber");
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Pooling a rack into one array. Staged on {@code ponder/mainframe_controller.nbt}: a belt-driven
     * 2x2 rack of nodes cabled around to the controller.
     */
    public static void mainframeController(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mainframe_controller", "Pooling an array");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos controller = util.grid().at(2, 1, 5);
        Selection rack = util.select().fromTo(4, 1, 1, 4, 2, 2);
        Selection drive = util.select().fromTo(5, 1, 1, 6, 2, 3);
        Selection cables = util.select().fromTo(3, 1, 5, 4, 1, 5)
                .add(util.select().fromTo(4, 1, 3, 4, 1, 4));

        scene.world().showSection(drive, Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(rack, Direction.WEST);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlocks(rack, state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, "rack", rack, 80);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 1)))
                .text("Driven nodes make compute. Touching nodes form one array.");
        scene.idle(90);

        scene.world().showSection(cables, Direction.NORTH);
        scene.idle(10);
        scene.world().showSection(util.select().position(controller), Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(controller))
                .text("The controller claims every node it can reach");
        scene.idle(100);

        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .text("Many slow nodes beat one fast node");
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Getting compute to a machine that is not next door. Staged on {@code ponder/data_cable.nbt},
     * where the run climbs over an empty tile to reach the chamber.
     */
    public static void dataCable(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("data_cable", "Carrying compute with Data Cables");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos controller = util.grid().at(3, 1, 6);
        BlockPos chamber = util.grid().at(1, 1, 6);
        Selection racks = util.select().fromTo(6, 1, 1, 6, 2, 2)
                .add(util.select().fromTo(6, 1, 5, 6, 2, 5));
        Selection drive = util.select().fromTo(7, 1, 1, 8, 2, 5);
        Selection trunk = util.select().fromTo(6, 1, 3, 6, 1, 4)
                .add(util.select().position(6, 1, 6))
                .add(util.select().fromTo(4, 1, 6, 5, 1, 6));
        Selection bridge = util.select().fromTo(1, 2, 6, 3, 2, 6);

        scene.world().showSection(drive, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(racks, Direction.WEST);
        scene.idle(15);

        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlocks(racks, state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.idle(10);

        scene.world().showSection(trunk, Direction.NORTH);
        scene.idle(10);
        scene.world().showSection(util.select().position(controller), Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(controller))
                .text("Cables join racks that are not touching");
        scene.idle(90);

        scene.world().showSection(bridge, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(chamber), Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(2, 2, 6)))
                .text("They route any direction, over obstacles");
        scene.idle(100);

        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().topOf(chamber))
                .text("Everything cabled in draws from the same pool");
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Training a bound matrix. Staged on {@code ponder/simulation_chamber.nbt}, shared with
     * {@link #simulationChamberJobs}.
     */
    public static void simulationChamberTraining(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simulation_chamber_training", "Training a model");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos chamber = util.grid().at(3, 2, 5);
        Selection rack = util.select().fromTo(6, 1, 1, 6, 2, 2);
        Selection supply = util.select().fromTo(7, 1, 1, 8, 2, 3)
                .add(util.select().fromTo(6, 1, 3, 6, 1, 5))
                .add(util.select().fromTo(4, 2, 5, 6, 2, 5));

        scene.world().showSection(supply, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(rack, Direction.WEST);
        scene.idle(15);
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlocks(rack, state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 2, 5), Direction.EAST);
        scene.idle(20);

        scene.overlay().showControls(util.vector().topOf(chamber), Pointing.DOWN, 60)
                .rightClick()
                .withItem(new ItemStack(ModItems.BLANK_DATA_MATRIX.get()));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(chamber))
                .text("Put a bound Data Matrix in the chamber");
        scene.idle(100);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(chamber, Direction.EAST))
                .text("Compute trains the recording into a working model");
        scene.idle(100);

        scene.effects().indicateSuccess(chamber);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .pointAt(util.vector().topOf(chamber))
                .text("Run a trained model to print that mob's drops");
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Running jobs: substrate in, loot out. Shares the training scene's structure. */
    public static void simulationChamberJobs(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simulation_chamber_jobs", "Printing Predictions without the mob");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos chamber = util.grid().at(3, 2, 5);
        Selection rack = util.select().fromTo(6, 1, 1, 6, 2, 2);
        Selection supply = util.select().fromTo(7, 1, 1, 8, 2, 3)
                .add(util.select().fromTo(6, 1, 3, 6, 1, 5))
                .add(util.select().fromTo(4, 2, 5, 6, 2, 5));
        // Blanks arrive along the back of the chamber; finished drops leave out of its side.
        BlockPos intakeBelt = util.grid().at(3, 1, 8);
        BlockPos intakeFunnel = util.grid().at(3, 2, 6);
        BlockPos outputBelt = util.grid().at(2, 1, 5);
        BlockPos outputFunnel = util.grid().at(2, 2, 5);
        Selection intakeLine = util.select().fromTo(3, 1, 6, 5, 1, 8)
                .add(util.select().position(intakeFunnel));
        Selection outputLine = util.select().fromTo(0, 1, 4, 2, 1, 5)
                .add(util.select().position(outputFunnel));

        scene.world().showSection(supply, Direction.DOWN);
        scene.world().showSection(rack, Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlocks(rack, state -> state.setValue(NeuralNodeBlock.LIT, true), false);
        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 2, 5), Direction.EAST);
        scene.idle(15);

        // Belt travel is Direction.fromAxisAndDirection(axis, speed < 0 ^ axis == X), so the sign
        // means opposite things on the two belts. The intake runs along Z and needs a negative speed
        // to carry blanks north into the chamber; the output runs along X, where negative would drive
        // drops back in, so it keeps the positive speed set above and carries them west.
        scene.world().setKineticSpeed(util.select().fromTo(3, 1, 6, 5, 1, 8), -32);

        scene.world().showSection(intakeLine, Direction.NORTH);
        scene.idle(15);
        // Insertion is refused when the side argument equals the belt's direction of travel, so
        // items are handed in from behind: south onto a northbound belt.
        scene.world().createItemOnBelt(intakeBelt, Direction.SOUTH,
                new ItemStack(ModItems.CRUDE_IMPRINT_BLANK.get()));
        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 7)))
                .text("Each job eats one Imprint Blank, fed in at the back");
        scene.idle(30);
        scene.world().createItemOnBelt(intakeBelt, Direction.SOUTH,
                new ItemStack(ModItems.CRUDE_IMPRINT_BLANK.get()));
        scene.idle(20);
        scene.world().flapFunnel(intakeFunnel, false);
        scene.idle(40);

        scene.world().showSection(outputLine, Direction.EAST);
        scene.idle(15);
        scene.effects().indicateSuccess(chamber);
        scene.world().flapFunnel(outputFunnel, true);
        // The chamber prints Predictions, not drops. Showing loot here left players expecting iron
        // to come out of this machine.
        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(outputBelt, Direction.EAST, prediction("minecraft:zombie"));
            scene.idle(12);
        }
        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(1, 1, 5)))
                .text("Jobs print Predictions, not drops");
        scene.idle(100);

        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(chamber, Direction.NORTH))
                .text("One Prediction is one roll of the loot table. A Loot Fabricator cashes it in.");
        scene.idle(100);

        scene.markAsFinished();
    }

    // --- Loot Fabricator ---------------------------------------------------------------------
    //
    // Both scenes stage on ponder/loot_fabricator.nbt: the machine on a brass casing at the centre,
    // belts in from the north and out to the south under a funnel each, driven from a gear train
    // along the east edge.
    //
    // The machine faces west because Ponder's camera is fixed at yRotation 145, which puts the north
    // and west faces toward the viewer. A south-facing machine shows the camera its back.

    /** The machine and its casing. */
    private static Selection fabricatorMachine(SceneBuildingUtil util) {
        return util.select().fromTo(3, 1, 3, 3, 2, 3);
    }

    /** The gear train along the back edge that drives both the machine and the belts. */
    private static Selection fabricatorDrive(SceneBuildingUtil util) {
        return util.select().fromTo(4, 1, 2, 6, 2, 4);
    }

    /** Belt in from the north, with the funnel that hands off to the machine. */
    private static Selection fabricatorIntake(SceneBuildingUtil util) {
        return util.select().fromTo(3, 1, 0, 3, 1, 2)
                .add(util.select().position(util.grid().at(3, 2, 2)));
    }

    /** Belt out to the south, with the funnel that fills it. */
    private static Selection fabricatorOutput(SceneBuildingUtil util) {
        return util.select().fromTo(3, 1, 4, 3, 1, 6)
                .add(util.select().position(util.grid().at(3, 2, 4)));
    }

    /**
     * Spending Predictions: a funnel in, a funnel out, and a shaft rather than an array.
     *
     * <p>Both belts lie along Z, and belt travel is
     * {@code Direction.fromAxisAndDirection(axis, speed < 0 ^ axis == X)}, so off the X axis a
     * positive speed carries items south. Both belts therefore run south on one positive speed: the
     * northern one into the machine, the southern one away from it.
     */
    public static void lootFabricator(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("loot_fabricator", "Turning Predictions into drops");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos fabricator = util.grid().at(3, 2, 3);
        BlockPos intakeBelt = util.grid().at(3, 1, 0);
        BlockPos intakeFunnel = util.grid().at(3, 2, 2);
        BlockPos outputBelt = util.grid().at(3, 1, 4);
        BlockPos outputFunnel = util.grid().at(3, 2, 4);

        scene.world().showSection(fabricatorMachine(util), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(fabricatorDrive(util), Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().everywhere(), 32);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(fabricator, Direction.WEST))
                .text("The Loot Fabricator runs on rotation, not compute");
        scene.idle(100);

        scene.world().showSection(fabricatorIntake(util), Direction.SOUTH);
        scene.idle(15);
        // Insertion is refused when the side argument equals the belt's direction of travel, so
        // items are handed in from behind: north onto a southbound belt.
        scene.world().createItemOnBelt(intakeBelt, Direction.NORTH, prediction("minecraft:zombie"));
        scene.idle(20);
        scene.world().createItemOnBelt(intakeBelt, Direction.NORTH, prediction("minecraft:zombie"));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 1)))
                .text("Feed it Predictions. A funnel in, a funnel out.");
        scene.idle(40);
        scene.world().flapFunnel(intakeFunnel, false);
        scene.world().modifyBlocks(fabricatorMachine(util),
                state -> state.hasProperty(LootFabricatorBlock.LIT)
                        ? state.setValue(LootFabricatorBlock.LIT, true) : state, false);
        scene.idle(60);

        // Drops out, assorted: unfiltered is the default mode.
        scene.world().showSection(fabricatorOutput(util), Direction.NORTH);
        scene.idle(15);
        scene.effects().indicateSuccess(fabricator);
        ItemStack[] rolls = {
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.CARROT),
        };
        for (ItemStack roll : rolls) {
            scene.world().flapFunnel(outputFunnel, true);
            scene.world().createItemOnBelt(outputBelt, Direction.NORTH, roll);
            scene.idle(15);
        }
        scene.overlay().showText(100)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 5)))
                .text("Left alone it rolls the loot table, one Prediction per roll");
        scene.idle(110);

        scene.markAsFinished();
    }

    /**
     * Choosing a drop. Ponder cannot open a screen, so this teaches by contrast: the previous scene's
     * build run again, with one item type leaving the machine instead of an assortment.
     */
    public static void lootFabricatorChoosing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("loot_fabricator_choosing", "Choosing what to make");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos fabricator = util.grid().at(3, 2, 3);
        BlockPos intakeBelt = util.grid().at(3, 1, 0);
        BlockPos intakeFunnel = util.grid().at(3, 2, 2);
        BlockPos outputBelt = util.grid().at(3, 1, 4);
        BlockPos outputFunnel = util.grid().at(3, 2, 4);

        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        scene.idle(15);
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.idle(10);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(fabricator, Direction.WEST))
                .text("Open it to see every drop the subject has");
        scene.idle(100);

        scene.world().createItemOnBelt(intakeBelt, Direction.NORTH, prediction("minecraft:zombie"));
        scene.idle(25);
        scene.world().flapFunnel(intakeFunnel, false);
        scene.world().modifyBlocks(fabricatorMachine(util),
                state -> state.hasProperty(LootFabricatorBlock.LIT)
                        ? state.setValue(LootFabricatorBlock.LIT, true) : state, false);
        scene.idle(40);

        scene.effects().indicateSuccess(fabricator);
        for (int i = 0; i < 3; i++) {
            scene.world().flapFunnel(outputFunnel, true);
            scene.world().createItemOnBelt(outputBelt, Direction.NORTH, new ItemStack(Items.IRON_INGOT));
            scene.idle(15);
        }
        scene.overlay().showText(90)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 5)))
                .text("Pick one and it makes only that");
        scene.idle(100);

        scene.overlay().showText(110)
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 6)))
                .text("Rarer drops cost more Predictions. Choosing only makes it predictable.");
        scene.idle(120);

        scene.markAsFinished();
    }
}
