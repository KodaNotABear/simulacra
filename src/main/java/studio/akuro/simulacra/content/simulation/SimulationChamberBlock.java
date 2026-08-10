package studio.akuro.simulacra.content.simulation;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.ModTooltips;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * Holds a Data Matrix and consumes Cognition Array compute to train it, then to print Predictions of
 * its subject. Right-click with a matrix to insert, or with Imprint Blanks of any grade to load
 * substrate; right-click empty-handed for status; sneak + right-click to take back the matrix, or the
 * substrate once the matrix is out.
 */
public class SimulationChamberBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IWrenchable {
    public static final MapCodec<SimulationChamberBlock> CODEC = simpleCodec(SimulationChamberBlock::new);

    /** True while the chamber is receiving compute; lights the animated viewport. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SimulationChamberBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulationChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server-only watchdog: goes dark if the controller feeding this chamber disappears.
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.SIMULATION_CHAMBER.get()
                ? (l, p, s, be) -> ((SimulationChamberBlockEntity) be).tickWatchdog()
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof DataMatrixItem && chamber.getModel().isEmpty()) {
            if (!level.isClientSide) {
                chamber.setModel(stack.copyWithCount(1));
                stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (SubstrateTier.fromStack(stack) != null) {
            if (!level.isClientSide) {
                int loaded = chamber.insertSubstrate(stack);
                if (loaded > 0) {
                    stack.shrink(loaded);
                    return ItemInteractionResult.SUCCESS;
                }
                // Nothing went in: the slot is full or already holds a different tier. Say so
                // rather than no-op silently.
                player.displayClientMessage(Component.translatable("message.simulacra.sim_substrate_rejected"), true);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.sidedSuccess(true);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && !chamber.getModel().isEmpty()) {
                ItemStack removed = chamber.removeModel();
                if (!player.addItem(removed)) {
                    player.drop(removed, false);
                }
                player.displayClientMessage(Component.translatable("message.simulacra.sim_extracted"), true);
            } else if (player.isShiftKeyDown()) {
                // No matrix to take, so take the substrate instead. Lets a tier be swapped in place.
                ItemStack substrate = chamber.removeSubstrate();
                if (!substrate.isEmpty()) {
                    if (!player.addItem(substrate)) {
                        player.drop(substrate, false);
                    }
                    player.displayClientMessage(
                            Component.translatable("message.simulacra.sim_substrate_removed"), true);
                } else {
                    player.displayClientMessage(statusMessage(chamber), true);
                }
            } else {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(chamber, buf -> buf.writeBlockPos(pos));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component statusMessage(SimulationChamberBlockEntity chamber) {
        if (chamber.isStalled()) {
            return Component.translatable(switch (chamber.stallReason()) {
                case SUBSTRATE_TOO_CRUDE -> "message.simulacra.sim_stalled_crude";
                case OUTPUT_FULL -> "message.simulacra.sim_stalled_full";
                case UNKNOWN_SUBJECT -> "message.simulacra.sim_stalled_subject";
                default -> "message.simulacra.sim_stalled_substrate";
            });
        }
        return switch (chamber.getMode()) {
            case EMPTY -> Component.translatable("message.simulacra.sim_empty");
            case UNBOUND -> Component.translatable("message.simulacra.sim_unbound");
            case NEED_DATA -> Component.translatable("message.simulacra.sim_needdata",
                    DataMatrixItem.getData(chamber.getModel()), DataMatrixItem.dataToTrain());
            case TRAINING -> Component.translatable("message.simulacra.sim_training",
                    String.format("%.0f", chamber.getProgressPercent()));
            case SIMULATING -> Component.translatable("message.simulacra.sim_simulating",
                    String.format("%.0f", chamber.getProgressPercent()),
                    String.format("%.1f", chamber.getSuppliedComputePerTick()));
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.addSummary(tooltip, "tooltip.simulacra.simulation_chamber.summary");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber
                ? chamber.getComparatorLevel()
                : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber) {
                if (!chamber.getModel().isEmpty()) {
                    Block.popResource(level, pos, chamber.getModel());
                }
                chamber.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
