package studio.akuro.simulacra.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlockEntity;
import studio.akuro.simulacra.content.mainframe.MainframeControllerBlockEntity;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Simulacra.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NeuralNodeBlockEntity>> NEURAL_NODE =
            register("neural_node", NeuralNodeBlockEntity::new, ModBlocks.NEURAL_NODE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframeControllerBlockEntity>> MAINFRAME_CONTROLLER =
            register("mainframe_controller", MainframeControllerBlockEntity::new, ModBlocks.MAINFRAME_CONTROLLER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimulationChamberBlockEntity>> SIMULATION_CHAMBER =
            register("simulation_chamber", SimulationChamberBlockEntity::new, ModBlocks.SIMULATION_CHAMBER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LootFabricatorBlockEntity>> LOOT_FABRICATOR =
            register("loot_fabricator", LootFabricatorBlockEntity::new, ModBlocks.LOOT_FABRICATOR);

    @SafeVarargs
    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> supplier,
            Supplier<? extends Block>... blocks
    ) {
        return BLOCK_ENTITY_TYPES.register(name, () -> new BlockEntityType<>(
                supplier,
                Arrays.stream(blocks).map(Supplier::get).collect(Collectors.toSet()),
                null
        ));
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
