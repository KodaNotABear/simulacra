package studio.akuro.simulacra.content.neuralnode;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import studio.akuro.simulacra.index.ModBlockEntities;

/**
 * A driven member of a Cognition Array. On its own it is just a kinetic block that draws stress; it
 * produces no compute by itself. The {@link studio.akuro.simulacra.content.mainframe.MainframeControllerBlockEntity}
 * reads the rotation speed of every node in its connected array and computes the pooled compute,
 * rewarding larger arrays. This keeps generation a driven multiblock rather than an instant per-block
 * conversion, in line with Create's conventions.
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
        // Light the drive front only while the node is actually being driven.
        //
        // Go through Create's helper rather than level.setBlock: this block entity is a
        // KineticBlockEntity, and a plain setBlock can cost it its kinetic network association, which
        // shows up as a driven node intermittently reporting no speed. switchToBlockState knows to
        // preserve it (and no-ops on the client and on an unchanged state).
        boolean lit = Math.abs(getSpeed()) > 0f;
        BlockState state = getBlockState();
        if (state.hasProperty(NeuralNodeBlock.LIT) && state.getValue(NeuralNodeBlock.LIT) != lit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition, state.setValue(NeuralNodeBlock.LIT, lit));
        }
    }
}
