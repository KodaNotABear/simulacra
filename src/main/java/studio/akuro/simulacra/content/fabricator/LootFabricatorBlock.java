package studio.akuro.simulacra.content.fabricator;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import studio.akuro.simulacra.content.ModTooltips;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * Stamps a chosen drop out of Predictions, driven by rotation.
 *
 * <p>Right-click to open its screen and pick from everything the loaded Predictions' subject can
 * drop. Predictions arrive through the item face from a funnel, chute or belt, never by hand: this is
 * a Create machine, so transport feeds it. That is what makes automating it worthwhile without
 * touching its rate, which stays a plain function of rotation so a factory can be sized on paper.
 */
public class LootFabricatorBlock extends HorizontalKineticBlock implements IBE<LootFabricatorBlockEntity> {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LootFabricatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.addSummary(tooltip, "tooltip.simulacra.loot_fabricator.summary");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LootFabricatorBlockEntity fabricator)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(fabricator, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof LootFabricatorBlockEntity fabricator) {
                fabricator.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public Class<LootFabricatorBlockEntity> getBlockEntityClass() {
        return LootFabricatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LootFabricatorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.LOOT_FABRICATOR.get();
    }
}
