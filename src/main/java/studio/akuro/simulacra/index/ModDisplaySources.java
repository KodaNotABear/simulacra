package studio.akuro.simulacra.index;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.compat.SimulacraDisplaySources;

/**
 * Display Link sources, registered in two halves: the sources go into Create's registry, then each is
 * bound to a block through {@link DisplaySource#BY_BLOCK}, a plain runtime map that has to be filled
 * after the blocks exist.
 */
public class ModDisplaySources {

    public static final DeferredRegister<DisplaySource> SOURCES =
            DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, Simulacra.MOD_ID);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Chamber> SIMULATION_CHAMBER =
            SOURCES.register("simulation_chamber", SimulacraDisplaySources.Chamber::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Fabricator> LOOT_FABRICATOR =
            SOURCES.register("loot_fabricator", SimulacraDisplaySources.Fabricator::new);

    public static void register(IEventBus eventBus) {
        SOURCES.register(eventBus);
    }

    /** Called once blocks and sources both exist; see {@link Simulacra}'s common setup. */
    public static void attachToBlocks() {
        DisplaySource.BY_BLOCK.add(ModBlocks.SIMULATION_CHAMBER.get(), SIMULATION_CHAMBER.get());
        DisplaySource.BY_BLOCK.add(ModBlocks.LOOT_FABRICATOR.get(), LOOT_FABRICATOR.get());
    }
}
