package studio.akuro.simulacra.content.mainframe;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;
import studio.akuro.simulacra.content.ModTooltips;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * The root of a Cognition Array. Pools compute from all Neural Nodes reachable through Data Cables.
 *
 * <p>It flood-fills the array, sums its compute, and splits the pool across connected Simulation
 * Chambers in proportion to demand, spending any surplus on overclocking them. One network is owned by
 * exactly one controller; the rest go dormant. Right-clicking reports the pool, and goggles show it too.
 */
public class MainframeControllerBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IWrenchable {
    public static final MapCodec<MainframeControllerBlock> CODEC = simpleCodec(MainframeControllerBlock::new);

    /** True while the array is producing compute; lights the animated dashboard. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MainframeControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Screen faces the player who placed it.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.addSummary(tooltip, "tooltip.simulacra.mainframe_controller.summary");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MainframeControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.MAINFRAME_CONTROLLER.get()
                ? (l, p, s, be) -> ((MainframeControllerBlockEntity) be).tick()
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MainframeControllerBlockEntity controller) {
            player.displayClientMessage(
                    Component.translatable("message.simulacra.array_status",
                            String.format("%.1f", controller.getTotalCompute()),
                            controller.getActiveNodeCount(),
                            controller.getNodeCount()),
                    true);
        }
        return InteractionResult.SUCCESS;
    }
}
