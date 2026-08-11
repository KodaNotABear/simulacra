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
 *
 * <p>Touching nodes share rotation, so one shaft drives a whole rack. See
 * {@link #propagateRotationTo}.
 */
public class NeuralNodeBlockEntity extends KineticBlockEntity {

    public NeuralNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public NeuralNodeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.NEURAL_NODE.get(), pos, state);
    }

    /**
     * Hands rotation straight to a touching node, so a rack needs one shaft rather than one shaft per
     * node. Nodes that touch already count as one array for compute; this makes them one thing for
     * rotation too.
     *
     * <p>Create calls this from {@code RotationPropagator.getRotationSpeedModifier} before it works
     * out any built-in connection, and treats a non-zero return as the whole ratio. Returning exactly
     * 1 is load-bearing in three ways:
     *
     * <ul>
     * <li>No gearing, so the array reads one speed however wide it is.</li>
     * <li>No sign flip, so a loop of nodes — a 2x2 block is one — cannot come back around
     * disagreeing with itself. The propagator destroys a block on a sign conflict, and on a cycle it
     * only complains when the speed coming round differs; at 1:1 it never does.</li>
     * <li>It matches what an axis connection would already have conveyed between two nodes lined up
     * back-to-front, so no arrangement changes meaning.</li>
     * </ul>
     *
     * <p>This deliberately does not care whether the two nodes' rotation axes agree. The array is one
     * turning thing; a node's facing chooses which face wears the shaft, not what it may drive.
     *
     * <p>Stress is untouched by any of this: each node remains its own member of the kinetic network
     * and is billed its own impact, so sixteen nodes on one shaft cost sixteen nodes' worth. Sharing
     * rotation shares no stress. {@code NeuralNodeTests.stressScalesWithNodeCount} holds that line.
     */
    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                     BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        // Face-touching only. Create's default neighbour search is the six direct offsets, but another
        // block's addPropagationLocations can put a diagonal pair in front of this, and nodes that
        // merely share an edge are not touching.
        if (target instanceof NeuralNodeBlockEntity && diff.distManhattan(BlockPos.ZERO) == 1) {
            return 1;
        }
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
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
