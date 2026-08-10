package studio.akuro.simulacra.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.client.ponder.SimulacraPonderPlugin;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeRenderer;
import studio.akuro.simulacra.index.ModBlockEntities;

/** Client-only setup. {@link Dist#CLIENT} keeps it from being classloaded on a dedicated server. */
@EventBusSubscriber(modid = Simulacra.MOD_ID, value = Dist.CLIENT)
public class SimulacraClient {

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NEURAL_NODE.get(), NeuralNodeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LOOT_FABRICATOR.get(),
                studio.akuro.simulacra.content.fabricator.LootFabricatorRenderer::new);
    }

    /** Ponder builds its index on load complete, so client setup is early enough to register. */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new SimulacraPonderPlugin());
    }

    /**
     * The frame the Data Matrix renderer draws behind its mob. It belongs to no blockstate or item
     * model chain, so nothing bakes it unless it is asked for explicitly.
     */
    @SubscribeEvent
    static void onRegisterAdditionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(DataMatrixRenderer.FRAME);
    }

    /** Hands Data Matrix rendering to {@link DataMatrixRenderer} so it can show its bound mob. */
    @SubscribeEvent
    static void onRegisterClientExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private DataMatrixRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    renderer = new DataMatrixRenderer(mc.getBlockEntityRenderDispatcher(),
                            mc.getEntityModels());
                }
                return renderer;
            }
        }, studio.akuro.simulacra.index.ModItems.BLANK_DATA_MATRIX.get());
    }

    /** Cached mob instances belong to the old level and old renderers; drop them on reload. */
    @SubscribeEvent
    static void onReloadRenderers(EntityRenderersEvent.AddLayers event) {
        DataMatrixRenderer.clearCache();
    }

    /** MenuScreens.register is not public in 1.21.1; NeoForge exposes this event instead. */
    @SubscribeEvent
    static void onRegisterScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(studio.akuro.simulacra.index.ModMenus.LOOT_FABRICATOR.get(),
                studio.akuro.simulacra.client.screen.LootFabricatorScreen::new);
        event.register(studio.akuro.simulacra.index.ModMenus.SIMULATION_CHAMBER.get(),
                studio.akuro.simulacra.client.screen.SimulationChamberScreen::new);
    }
}
