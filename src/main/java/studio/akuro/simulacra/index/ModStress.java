package studio.akuro.simulacra.index;

import com.simibubi.create.api.stress.BlockStressValues;
import studio.akuro.simulacra.SimulacraConfig;

/**
 * Registers Create stress values for this mod's kinetic blocks. Run once, on the main thread, during
 * common setup (see {@link studio.akuro.simulacra.Simulacra}).
 */
public class ModStress {

    public static void register() {
        // The Neural Node is a consumer: it draws stress to turn rotation into compute.
        BlockStressValues.IMPACTS.register(
                ModBlocks.NEURAL_NODE.get(),
                () -> SimulacraConfig.NEURAL_NODE_STRESS_IMPACT.get()
        );

        // The Fabricator is the other half of the split: the array decides what you can simulate,
        // the shaft decides how fast the results get stamped out.
        BlockStressValues.IMPACTS.register(
                ModBlocks.LOOT_FABRICATOR.get(),
                () -> SimulacraConfig.FABRICATOR_STRESS_IMPACT.get()
        );
    }
}
