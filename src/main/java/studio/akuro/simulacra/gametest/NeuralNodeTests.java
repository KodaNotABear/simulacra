package studio.akuro.simulacra.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.mainframe.MainframeControllerBlockEntity;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlockEntity;
import studio.akuro.simulacra.index.ModBlocks;

/**
 * Server-side tests for how rotation reaches a Neural Node, run headlessly by
 * {@code gradlew runGameTestServer}.
 *
 * <p>Touching nodes share rotation, so one shaft drives a whole rack. Everything here is driven by a
 * real Create creative motor rather than a stubbed speed, because the thing under test is Create's
 * propagator and not our arithmetic. Three properties matter and none of them are visible from a
 * build:
 *
 * <ul>
 * <li>Rotation actually crosses from node to node, at the same speed.</li>
 * <li>Stress still scales per node. If sharing rotation shared stress, a rack would be free power.</li>
 * <li>A loop of nodes survives. Create's propagator destroys blocks when a network comes back around
 * disagreeing with itself, so a 2x2 — the most natural thing a player will build — must be safe.</li>
 * </ul>
 */
@GameTestHolder(Simulacra.MOD_ID)
@PrefixGameTestTemplate(false)
public class NeuralNodeTests {

    /** Bare name; {@link GameTestHolder} supplies the namespace. The platform is 5x5x5. */
    private static final String PLATFORM = "platform";
    /** How long to let Create settle a network before reading it. */
    private static final int SETTLE_TICKS = 20;
    /** Two speeds this close are the same speed; Create's own cycle check uses a similar epsilon. */
    private static final float SPEED_EPSILON = 1e-3f;

    /**
     * A node facing north, so its shaft face is south. Facing only chooses which face wears the shaft
     * — propagation between touching nodes does not depend on it.
     */
    private static void placeNode(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.NEURAL_NODE.get().defaultBlockState()
                .setValue(NeuralNodeBlock.HORIZONTAL_FACING, Direction.NORTH));
    }

    /** A creative motor pointing north, so it drives the south shaft face of the node in front of it. */
    private static void placeMotor(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
    }

    private static NeuralNodeBlockEntity node(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof NeuralNodeBlockEntity node)) {
            throw new GameTestAssertException("no Neural Node at " + pos
                    + " — the kinetic propagator destroys blocks it cannot reconcile");
        }
        return node;
    }

    /**
     * Every listed position must hold a node turning at the same non-zero speed. Returns that speed.
     */
    private static float assertTurningTogether(GameTestHelper helper, BlockPos... positions) {
        float reference = Math.abs(node(helper, positions[0]).getSpeed());
        if (reference == 0f) {
            throw new GameTestAssertException("the driven node at " + positions[0] + " is not turning at all");
        }
        for (BlockPos pos : positions) {
            float speed = Math.abs(node(helper, pos).getSpeed());
            if (Math.abs(speed - reference) > SPEED_EPSILON) {
                throw new GameTestAssertException("node at " + pos + " turns at " + speed
                        + " rpm while the node at " + positions[0] + " turns at " + reference
                        + " rpm — rotation did not carry across the array at 1:1");
            }
        }
        return reference;
    }

    /**
     * One shaft, a line of three touching nodes, and every one of them turning. This is the whole
     * point of the change: before it, only the node the motor touched would move and the other two
     * would each have needed their own shaft.
     *
     * <p>It also checks the array actually produces compute, because a node reporting a speed while
     * contributing nothing would satisfy a speed-only assertion and still be useless.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void oneShaftDrivesALineOfNodes(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 1, 2);
        BlockPos middle = new BlockPos(2, 1, 2);
        BlockPos right = new BlockPos(3, 1, 2);
        BlockPos controllerPos = new BlockPos(2, 1, 1);

        placeNode(helper, left);
        placeNode(helper, middle);
        placeNode(helper, right);
        helper.setBlock(controllerPos, ModBlocks.MAINFRAME_CONTROLLER.get());
        // Only the middle node is touched by a shaft. The outer two must be reached by propagation.
        placeMotor(helper, new BlockPos(2, 1, 3));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertTurningTogether(helper, middle, left, right);

            if (!(helper.getBlockEntity(controllerPos) instanceof MainframeControllerBlockEntity controller)) {
                throw new GameTestAssertException("no Mainframe Controller at " + controllerPos);
            }
            if (controller.getActiveNodeCount() != 3) {
                throw new GameTestAssertException("one shaft drove " + controller.getActiveNodeCount()
                        + " of 3 touching nodes; all three should be producing compute");
            }
            if (controller.getTotalCompute() <= 0f) {
                throw new GameTestAssertException("three turning nodes pooled "
                        + controller.getTotalCompute() + " compute");
            }
            helper.succeed();
        });
    }

    /**
     * A 2x2 block of nodes is a loop: rotation leaves the driven node two ways and meets itself on the
     * far corner. Create answers a network that disagrees with itself by destroying a block, so the
     * real assertion is that all four nodes are still there afterwards, turning as one.
     *
     * <p>They survive because nodes convey rotation at exactly 1. A meshing ratio — the cogwheel
     * behaviour Create's Mechanical Crafters use — would reverse sign around the ring and could tear
     * the array apart.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void twoByTwoLoopHoldsTogether(GameTestHelper helper) {
        BlockPos driven = new BlockPos(2, 1, 2);
        BlockPos sideways = new BlockPos(1, 1, 2);
        BlockPos ahead = new BlockPos(2, 1, 1);
        BlockPos corner = new BlockPos(1, 1, 1);

        placeNode(helper, driven);
        placeNode(helper, sideways);
        placeNode(helper, ahead);
        placeNode(helper, corner);
        placeMotor(helper, new BlockPos(2, 1, 3));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            // Blocks first: a torn network shows up as a missing node, which reads better than a
            // confusing "no block entity" further down.
            for (BlockPos pos : new BlockPos[] { driven, sideways, ahead, corner }) {
                helper.assertBlockPresent(ModBlocks.NEURAL_NODE.get(), pos);
            }
            assertTurningTogether(helper, driven, sideways, ahead, corner);

            NeuralNodeBlockEntity cornerNode = node(helper, corner);
            if (cornerNode.isOverStressed()) {
                throw new GameTestAssertException(
                        "the 2x2 loop reported itself overstressed on a creative motor");
            }
            helper.succeed();
        });
    }

    /**
     * The exploit guard. Sharing rotation must not share stress: four nodes on one shaft have to cost
     * four nodes' worth, or a rack becomes free compute.
     *
     * <p>Two independent arrays are built side by side and their networks compared, rather than
     * checking a number against the config. That way the test is about the ratio — which is the
     * property that matters — and does not fail when someone retunes the impact value.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void stressScalesWithNodeCount(GameTestHelper helper) {
        // Lone node, its own motor.
        BlockPos solo = new BlockPos(0, 1, 0);
        placeNode(helper, solo);
        placeMotor(helper, new BlockPos(0, 1, 1));

        // Four touching nodes on a single motor, two blocks clear of the lone one so the two arrays
        // cannot touch and merge into one network.
        BlockPos quadDriven = new BlockPos(2, 1, 1);
        BlockPos[] quad = { quadDriven, new BlockPos(3, 1, 1), new BlockPos(2, 1, 0), new BlockPos(3, 1, 0) };
        for (BlockPos pos : quad) {
            placeNode(helper, pos);
        }
        placeMotor(helper, new BlockPos(2, 1, 2));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            NeuralNodeBlockEntity soloNode = node(helper, solo);
            float soloSpeed = assertTurningTogether(helper, solo);
            float quadSpeed = assertTurningTogether(helper, quad);
            if (Math.abs(soloSpeed - quadSpeed) > SPEED_EPSILON) {
                throw new GameTestAssertException("the two arrays turn at different speeds (" + soloSpeed
                        + " vs " + quadSpeed + "), so their stress is not comparable");
            }

            KineticNetwork soloNetwork = soloNode.getOrCreateNetwork();
            KineticNetwork quadNetwork = node(helper, quadDriven).getOrCreateNetwork();
            if (soloNetwork == quadNetwork) {
                throw new GameTestAssertException(
                        "the two arrays share a kinetic network; the comparison would be meaningless");
            }

            float soloStress = soloNetwork.calculateStress();
            float quadStress = quadNetwork.calculateStress();
            if (soloStress <= 0f) {
                throw new GameTestAssertException("a turning Neural Node drew no stress at all ("
                        + soloStress + " su)");
            }
            // Generous tolerance: the claim is "four nodes cost four nodes", not a float identity.
            if (Math.abs(quadStress - 4f * soloStress) > 0.01f * soloStress) {
                throw new GameTestAssertException("four nodes on one shaft drew " + quadStress
                        + " su where one node draws " + soloStress + " su; expected "
                        + (4f * soloStress) + " su. Rotation is shared, stress must not be");
            }
            helper.succeed();
        });
    }
}
