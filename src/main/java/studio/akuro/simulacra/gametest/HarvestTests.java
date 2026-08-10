package studio.akuro.simulacra.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.index.ModBlocks;

import java.util.List;

/**
 * Every machine has to be harvestable by the tool a player would reach for.
 *
 * <p>A block that calls {@code requiresCorrectToolForDrops()} but appears in no {@code mineable/*} tag
 * has no correct tool at all: nothing satisfies the check, so it silently drops nothing however it is
 * mined. That is invisible in a build, invisible in a log, and only shows up as a player losing a
 * machine — which makes it exactly the sort of thing worth pinning down in a test.
 */
@GameTestHolder(Simulacra.MOD_ID)
@PrefixGameTestTemplate(false)
public class HarvestTests {

    private static final String PLATFORM = "platform";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static List<Block> machines() {
        return List.of(
                ModBlocks.NEURAL_NODE.get(),
                ModBlocks.MAINFRAME_CONTROLLER.get(),
                ModBlocks.SIMULATION_CHAMBER.get(),
                ModBlocks.LOOT_FABRICATOR.get(),
                ModBlocks.DATA_CABLE.get());
    }

    /**
     * Create's own casings sit in both mineable/pickaxe and mineable/axe, so a player carrying either
     * can take a machine down. Matching that is what stops these blocks feeling foreign in a Create
     * base — and with requiresCorrectToolForDrops set, being in neither tag means no tool works at all.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void everyMachineIsHarvestableByPickaxeAndAxe(GameTestHelper helper) {
        StringBuilder broken = new StringBuilder();
        for (Block block : machines()) {
            helper.setBlock(POS, block);
            BlockState state = helper.getBlockState(POS);
            if (!state.requiresCorrectToolForDrops()) {
                continue;
            }
            for (ItemStack tool : List.of(new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_AXE))) {
                if (!tool.isCorrectToolForDrops(state)) {
                    broken.append("\n  ").append(block.getName().getString())
                            .append(" drops nothing to ").append(tool.getHoverName().getString());
                }
            }
        }
        if (!broken.isEmpty()) {
            throw new GameTestAssertException("unharvestable combinations:" + broken);
        }
        helper.succeed();
    }

    /**
     * Every machine must take a wrench, the way every Create machine does: rotate with it, and
     * sneak-click to pick the block back up rather than mining it.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void everyMachineIsWrenchable(GameTestHelper helper) {
        StringBuilder missing = new StringBuilder();
        for (Block block : machines()) {
            if (!(block instanceof com.simibubi.create.content.equipment.wrench.IWrenchable)) {
                missing.append("\n  ").append(block.getName().getString()).append(" is not IWrenchable");
            }
        }
        if (!missing.isEmpty()) {
            throw new GameTestAssertException("blocks a wrench cannot handle:" + missing);
        }
        helper.succeed();
    }
}
