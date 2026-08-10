package studio.akuro.simulacra.content.neuralnode;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import studio.akuro.simulacra.content.ModTooltips;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * The Neural Node: a kinetic consumer that turns rotation into a compute rate.
 *
 * <p>It accepts a shaft on the face opposite its horizontal facing (one axis in, like a Millstone or
 * the Mechanical Press's input). Faster rotation means more compute. For now the compute it produces
 * is only displayed on the goggle tooltip; wiring it into a shared Cognition Array pool comes with the
 * Mainframe Controller.
 */
public class NeuralNodeBlock extends HorizontalKineticBlock implements IBE<NeuralNodeBlockEntity> {

    /** True while the node is being driven; swaps the front to the lit, animated texture. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public NeuralNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Front (drive display) faces the player; the shaft input faces away, which is what players
        // expect when placing a machine. Sneak to flip.
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.addSummary(tooltip, "tooltip.simulacra.neural_node.summary");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public Class<NeuralNodeBlockEntity> getBlockEntityClass() {
        return NeuralNodeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NeuralNodeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.NEURAL_NODE.get();
    }
}
