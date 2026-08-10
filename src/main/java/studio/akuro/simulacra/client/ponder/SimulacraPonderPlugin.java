package studio.akuro.simulacra.client.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.index.ModBlocks;
import studio.akuro.simulacra.index.ModItems;

/**
 * Registers Simulacra's Ponder scenes. Client-only: Ponder's registry lives on the client, so this
 * is reached exclusively from {@link studio.akuro.simulacra.client.SimulacraClient}.
 *
 * <p>The scene id passed to {@code scene.title(...)} drives the lang keys
 * ({@code simulacra.ponder.<id>.header} and {@code .text_<n>}); the second argument here is the
 * schematic path, resolved as {@code assets/simulacra/ponder/<path>.nbt}. They are deliberately the
 * same string per scene so the pairing stays obvious.
 */
public class SimulacraPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return Simulacra.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Two scenes for the node; Ponder pages between them with the arrows.
        helper.addStoryBoard(ModBlocks.NEURAL_NODE.getId(), "neural_node/single",
                SimulacraScenes::neuralNodeSingle);
        helper.addStoryBoard(ModBlocks.NEURAL_NODE.getId(), "neural_node/array",
                SimulacraScenes::neuralNodeArray);
        // The matrix is the least discoverable step in the loop, so it gets its own scene on the item.
        helper.addStoryBoard(ModItems.BLANK_DATA_MATRIX.getId(), "data_matrix",
                SimulacraScenes::dataMatrix);

        // The array scene explains the controller as much as it does the node, so it shows on both.
        helper.forComponents(ModBlocks.MAINFRAME_CONTROLLER.getId())
                .addStoryBoard("neural_node/array", SimulacraScenes::neuralNodeArray)
                .addStoryBoard("mainframe_controller", SimulacraScenes::mainframeController);

        // Likewise the cable run is really about extending a controller's reach.
        helper.forComponents(ModBlocks.DATA_CABLE.getId(), ModBlocks.MAINFRAME_CONTROLLER.getId())
                .addStoryBoard("data_cable", SimulacraScenes::dataCable);

        helper.forComponents(ModBlocks.SIMULATION_CHAMBER.getId())
                .addStoryBoard("simulation_chamber", SimulacraScenes::simulationChamberTraining)
                .addStoryBoard("simulation_chamber", SimulacraScenes::simulationChamberJobs);

        // The Fabricator is where Predictions are finally spent, so the Prediction itself carries
        // the scenes too: a player holding a stack of them wants to know what they are for.
        helper.forComponents(ModBlocks.LOOT_FABRICATOR.getId(), ModItems.PREDICTION.getId())
                .addStoryBoard("loot_fabricator", SimulacraScenes::lootFabricator)
                .addStoryBoard("loot_fabricator", SimulacraScenes::lootFabricatorChoosing);
    }
}
