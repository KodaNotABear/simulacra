package studio.akuro.simulacra.index;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.mainframe.MainframeControllerBlock;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlock;
import studio.akuro.simulacra.content.network.DataCableBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlock;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Simulacra.MOD_ID);

    public static final DeferredBlock<NeuralNodeBlock> NEURAL_NODE =
            BLOCKS.registerBlock("neural_node", NeuralNodeBlock::new, machineProperties());

    public static final DeferredBlock<MainframeControllerBlock> MAINFRAME_CONTROLLER =
            BLOCKS.registerBlock("mainframe_controller", MainframeControllerBlock::new, machineProperties());

    public static final DeferredBlock<SimulationChamberBlock> SIMULATION_CHAMBER =
            BLOCKS.registerBlock("simulation_chamber", SimulationChamberBlock::new, machineProperties());

    public static final DeferredBlock<LootFabricatorBlock> LOOT_FABRICATOR =
            BLOCKS.registerBlock("loot_fabricator", LootFabricatorBlock::new, machineProperties());

    public static final DeferredBlock<DataCableBlock> DATA_CABLE =
            BLOCKS.registerBlock("data_cable", DataCableBlock::new, cableProperties());

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_CYAN)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F)
                .sound(SoundType.COPPER)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties cableProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(1.5F, 3.0F)
                .sound(SoundType.COPPER);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
