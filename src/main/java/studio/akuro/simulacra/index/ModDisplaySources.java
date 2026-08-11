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

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Subject> SUBJECT =
            SOURCES.register("subject", SimulacraDisplaySources.Subject::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Grade> GRADE =
            SOURCES.register("grade", SimulacraDisplaySources.Grade::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Data> DATA =
            SOURCES.register("data", SimulacraDisplaySources.Data::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Substrate> SUBSTRATE =
            SOURCES.register("substrate", SimulacraDisplaySources.Substrate::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Status> STATUS =
            SOURCES.register("status", SimulacraDisplaySources.Status::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Target> TARGET =
            SOURCES.register("target", SimulacraDisplaySources.Target::new);

    public static final DeferredHolder<DisplaySource, SimulacraDisplaySources.Rate> RATE =
            SOURCES.register("rate", SimulacraDisplaySources.Rate::new);

    public static void register(IEventBus eventBus) {
        SOURCES.register(eventBus);
    }

    /** Called once blocks and sources both exist; see {@link Simulacra}'s common setup. */
    public static void attachToBlocks() {
        for (DeferredHolder<DisplaySource, ?> chamber : java.util.List.of(SUBJECT, GRADE, DATA, SUBSTRATE, STATUS)) {
            DisplaySource.BY_BLOCK.add(ModBlocks.SIMULATION_CHAMBER.get(), chamber.get());
        }
        for (DeferredHolder<DisplaySource, ?> fabricator : java.util.List.of(TARGET, RATE)) {
            DisplaySource.BY_BLOCK.add(ModBlocks.LOOT_FABRICATOR.get(), fabricator.get());
        }
    }
}
