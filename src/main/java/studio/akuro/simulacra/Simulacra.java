package studio.akuro.simulacra;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.akuro.simulacra.content.data.DataMatrixEvents;
import studio.akuro.simulacra.index.ModBlockEntities;
import studio.akuro.simulacra.index.ModBlocks;
import studio.akuro.simulacra.index.ModCreativeTabs;
import studio.akuro.simulacra.index.ModItems;
import studio.akuro.simulacra.index.ModMenus;
import studio.akuro.simulacra.index.ModDisplaySources;
import studio.akuro.simulacra.index.ModSounds;
import studio.akuro.simulacra.index.ModStress;

/**
 * Simulacra — a Create add-on.
 *
 * <p>The core idea: surplus rotational power (SU) is converted into a new resource, <b>compute</b>,
 * by a mechanical "neural network" datacenter. Compute trains models of mobs (gathered as data) and
 * then runs simulations that output that mob's loot, with <b>zero spawned entities</b> — so it scales
 * on a server where a physical mob farm would not.
 *
 * <p>This 0.1.0 scaffold ships only the first vertical slice: the {@code Neural Node}, the block that
 * turns rotation into a compute rate. Everything downstream (Mainframe Controller, Simulation Chamber,
 * data models, the substrate chain) is documented in DESIGN.md and stubbed for later.
 */
@Mod(Simulacra.MOD_ID)
public class Simulacra {
    public static final String MOD_ID = "simulacra";
    public static final Logger LOGGER = LoggerFactory.getLogger("Simulacra");

    public Simulacra(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, SimulacraConfig.SPEC);

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        ModSounds.register(modBus);
        ModDisplaySources.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);

        // Game-bus events: feed mob kills into the offhand Data Matrix.
        NeoForge.EVENT_BUS.addListener(DataMatrixEvents::onLivingDeath);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // Expose the Simulation Chamber's loot buffer so hoppers and Create funnels can pull from it.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SIMULATION_CHAMBER.get(),
                (chamber, side) -> chamber.getItemHandler());
        // And the Fabricator's, which had a handler and never registered it — so no funnel could feed
        // it Predictions or pull its output, which is the whole way the machine is meant to be used.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LOOT_FABRICATOR.get(),
                (fabricator, side) -> fabricator.getItemHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Create's stress registries are not thread-safe to touch off-thread; enqueue onto the main thread.
        event.enqueueWork(ModStress::register);
        // DisplaySource.BY_BLOCK is a plain runtime map rather than a registry, so it is filled here,
        // once both the blocks and the sources exist.
        event.enqueueWork(ModDisplaySources::attachToBlocks);
        LOGGER.info("Simulacra loaded: rotation-to-compute online.");
    }
}
