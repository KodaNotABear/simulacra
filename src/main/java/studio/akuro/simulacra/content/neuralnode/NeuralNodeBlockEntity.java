package studio.akuro.simulacra.content.neuralnode;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * A driven member of a Cognition Array. On its own it is a kinetic block that draws stress and
 * produces nothing; {@link studio.akuro.simulacra.content.mainframe.MainframeControllerBlockEntity}
 * reads the speed of every node in its array and pools the compute, rewarding larger arrays.
 */
public class NeuralNodeBlockEntity extends KineticBlockEntity {

    public NeuralNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public NeuralNodeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.NEURAL_NODE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        // Light the drive front only while driven. Via Create's helper, not level.setBlock: a plain
        // setBlock can cost a KineticBlockEntity its network association, which shows up as a driven
        // node intermittently reporting no speed.
        boolean lit = Math.abs(getSpeed()) > 0f;
        BlockState state = getBlockState();
        if (state.hasProperty(NeuralNodeBlock.LIT) && state.getValue(NeuralNodeBlock.LIT) != lit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition, state.setValue(NeuralNodeBlock.LIT, lit));
        }
    }
}
