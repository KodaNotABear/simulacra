package studio.akuro.simulacra.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity.StallReason;
import studio.akuro.simulacra.index.ModBlocks;
import studio.akuro.simulacra.index.ModItems;

/**
 * Server-side tests for the Simulation Chamber, run headlessly by {@code gradlew runGameTestServer}.
 *
 * <p>These target the rules that are cheap to break and expensive to notice: when the chamber refuses
 * to run, and how much compute it asks the array for. Both are invisible from outside — a chamber that
 * quietly draws its full allowance against a job it can never finish looks exactly like a working one
 * until the whole array is starved.
 *
 * <p>Rendering is deliberately out of scope. A GameTest can prove the chamber reports OUTPUT_FULL; it
 * cannot prove the panel looks right.
 */
@GameTestHolder(Simulacra.MOD_ID)
@PrefixGameTestTemplate(false)
public class ChamberTests {

    /**
     * Bare name, not "simulacra:platform" — {@link GameTestHolder} already supplies the namespace,
     * and qualifying it here yields the id "simulacra:simulacra:platform".
     */
    private static final String PLATFORM = "platform";
    /** Centre of the 5x5x5 platform, one block above its floor. */
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static SimulationChamberBlockEntity placeChamber(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.SIMULATION_CHAMBER.get());
        return helper.getBlockEntity(POS);
    }

    /**
     * Builds a matrix in the state training would have left it in. Written through the component keys
     * rather than the item's own binding path, because that path needs a player holding the matrix in
     * their offhand while something dies — far more machinery than the thing under test.
     */
    private static ItemStack trainedMatrix(String mobId) {
        ItemStack stack = new ItemStack(ModItems.BLANK_DATA_MATRIX.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Mob", mobId);
        tag.putInt("Data", 4096);
        tag.putBoolean("Trained", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void expectStall(GameTestHelper helper, SimulationChamberBlockEntity chamber,
                                    StallReason expected) {
        StallReason actual = chamber.stallReason();
        if (actual != expected) {
            throw new GameTestAssertException("expected stall reason " + expected + " but got " + actual);
        }
        // A stalled chamber must also stop asking for compute, or it starves its neighbours on the
        // array while producing nothing.
        if (chamber.getComputeDemand() != 0f) {
            throw new GameTestAssertException(
                    "chamber stalled on " + expected + " but still demands "
                            + chamber.getComputeDemand() + " cu/t");
        }
    }

    @GameTest(template = PLATFORM)
    public static void emptyChamberDrawsNothing(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        if (chamber.getComputeDemand() != 0f) {
            throw new GameTestAssertException("an empty chamber should draw no compute, drew "
                    + chamber.getComputeDemand());
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void withoutSubstrateStalls(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        chamber.setModel(trainedMatrix("minecraft:zombie"));
        expectStall(helper, chamber, StallReason.NO_SUBSTRATE);
        helper.succeed();
    }

    /**
     * A model whose mob no longer exists (its mod was uninstalled) can never print anything. It has to
     * be reported rather than silently drawing compute against a job that must always fail.
     */
    @GameTest(template = PLATFORM)
    public static void vanishedSubjectStalls(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        chamber.setModel(trainedMatrix("somemod:creature_that_left"));
        chamber.insertSubstrate(new ItemStack(ModItems.PRISTINE_IMPRINT_BLANK.get(), 16));
        expectStall(helper, chamber, StallReason.UNKNOWN_SUBJECT);
        helper.succeed();
    }

    /** Only pristine substrate can hold a boss imprint; crude must refuse rather than print junk. */
    @GameTest(template = PLATFORM)
    public static void bossSubjectRejectsCrudeSubstrate(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        chamber.setModel(trainedMatrix("minecraft:wither"));
        chamber.insertSubstrate(new ItemStack(ModItems.CRUDE_IMPRINT_BLANK.get(), 16));
        expectStall(helper, chamber, StallReason.SUBSTRATE_TOO_CRUDE);
        helper.succeed();
    }

    /**
     * Substrate has to come back out by hand. The capability face deliberately refuses to give it to
     * automation, which once left breaking the block as the only way to change tiers.
     */
    @GameTest(template = PLATFORM)
    public static void substrateComesBackOut(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        int accepted = chamber.insertSubstrate(new ItemStack(ModItems.REFINED_IMPRINT_BLANK.get(), 8));
        if (accepted != 8) {
            throw new GameTestAssertException("chamber accepted " + accepted + " of 8 blanks");
        }
        ItemStack back = chamber.removeSubstrate();
        if (!back.is(ModItems.REFINED_IMPRINT_BLANK.get()) || back.getCount() != 8) {
            throw new GameTestAssertException("got back " + back.getCount() + "x " + back.getItem()
                    + " instead of 8 refined blanks");
        }
        if (chamber.hasSubstrate()) {
            throw new GameTestAssertException("chamber still reports substrate after removal");
        }
        helper.succeed();
    }

    /**
     * The array is the game: a harder subject on finer substrate must cost proportionally more per
     * tick, so the size of an array decides what it can simulate rather than only how many chambers it
     * can feed. If these ever collapse to the same number, that whole design has quietly stopped
     * working, and nothing else would report it.
     */
    @GameTest(template = PLATFORM)
    public static void harderJobsDemandMoreCompute(GameTestHelper helper) {
        SimulationChamberBlockEntity common = placeChamber(helper);
        common.setModel(trainedMatrix("minecraft:zombie"));
        common.insertSubstrate(new ItemStack(ModItems.CRUDE_IMPRINT_BLANK.get(), 16));
        float commonDemand = common.getComputeDemand();

        BlockPos bossPos = POS.offset(1, 0, 0);
        helper.setBlock(bossPos, ModBlocks.SIMULATION_CHAMBER.get());
        SimulationChamberBlockEntity boss = helper.getBlockEntity(bossPos);
        boss.setModel(trainedMatrix("minecraft:wither"));
        boss.insertSubstrate(new ItemStack(ModItems.PRISTINE_IMPRINT_BLANK.get(), 16));
        float bossDemand = boss.getComputeDemand();

        if (commonDemand <= 0f) {
            throw new GameTestAssertException("a runnable common-mob chamber demanded " + commonDemand);
        }
        if (bossDemand <= commonDemand) {
            throw new GameTestAssertException("a wither on pristine substrate demanded " + bossDemand
                    + " cu/t, no more than a zombie on crude at " + commonDemand);
        }
        helper.succeed();
    }

    /** Surplus array output should have somewhere to go, but only while there is a job to speed up. */
    @GameTest(template = PLATFORM)
    public static void overclockHeadroomOnlyWhileRunnable(GameTestHelper helper) {
        SimulationChamberBlockEntity chamber = placeChamber(helper);
        if (chamber.getOverclockHeadroom() != 0f) {
            throw new GameTestAssertException("an idle chamber offered overclock headroom of "
                    + chamber.getOverclockHeadroom());
        }
        chamber.setModel(trainedMatrix("minecraft:zombie"));
        chamber.insertSubstrate(new ItemStack(ModItems.CRUDE_IMPRINT_BLANK.get(), 16));
        if (chamber.getOverclockHeadroom() <= 0f) {
            throw new GameTestAssertException("a running chamber offered no overclock headroom");
        }
        helper.succeed();
    }
}
