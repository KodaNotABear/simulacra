package studio.akuro.simulacra.content.network;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import studio.akuro.simulacra.content.ModTooltips;
import studio.akuro.simulacra.content.mainframe.MainframeControllerBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlock;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlock;

import java.util.Map;

/**
 * A passive conductor in the compute network, shaped and behaving like a thin pipe: a small central
 * core that grows an arm toward each neighbour it connects to (other cables, the Controller, or a
 * Neural Node). Connection state drives both the model (multipart) and the collision shape.
 *
 * <p>The Mainframe Controller still flood-fills through {@code DataCableBlock}s; the connection
 * properties here are purely about appearance/shape and use the same adjacency rule.
 */
public class DataCableBlock extends Block implements IWrenchable {
    public static final MapCodec<DataCableBlock> CODEC = simpleCodec(DataCableBlock::new);
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.addSummary(tooltip, "tooltip.simulacra.data_cable.summary");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] ARMS = new VoxelShape[6];
    private static final VoxelShape[] SHAPE_BY_MASK = new VoxelShape[64];

    static {
        ARMS[Direction.DOWN.get3DDataValue()]  = Block.box(6, 0, 6, 10, 6, 10);
        ARMS[Direction.UP.get3DDataValue()]    = Block.box(6, 10, 6, 10, 16, 10);
        ARMS[Direction.NORTH.get3DDataValue()] = Block.box(6, 6, 0, 10, 10, 6);
        ARMS[Direction.SOUTH.get3DDataValue()] = Block.box(6, 6, 10, 10, 10, 16);
        ARMS[Direction.WEST.get3DDataValue()]  = Block.box(0, 6, 6, 6, 10, 10);
        ARMS[Direction.EAST.get3DDataValue()]  = Block.box(10, 6, 6, 16, 10, 10);
        for (int mask = 0; mask < SHAPE_BY_MASK.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.get3DDataValue())) != 0) {
                    shape = Shapes.or(shape, ARMS[d.get3DDataValue()]);
                }
            }
            SHAPE_BY_MASK[mask] = shape;
        }
    }

    public DataCableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty prop : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(prop, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connectsTo(level.getBlockState(pos.relative(d))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighborState));
    }

    private static boolean connectsTo(BlockState neighbor) {
        Block block = neighbor.getBlock();
        return block instanceof DataCableBlock
                || block instanceof MainframeControllerBlock
                || block instanceof NeuralNodeBlock
                || block instanceof SimulationChamberBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(d))) {
                mask |= (1 << d.get3DDataValue());
            }
        }
        return SHAPE_BY_MASK[mask];
    }
}
