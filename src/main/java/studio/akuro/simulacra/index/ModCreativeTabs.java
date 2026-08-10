package studio.akuro.simulacra.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;

public class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Simulacra.MOD_ID);

    public static void register(IEventBus eventBus) {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Simulacra.MOD_ID + ".main"))
                .icon(() -> new ItemStack(ModItems.NEURAL_NODE.get()))
                .displayItems((params, output) -> {
                    output.accept(ModItems.MAINFRAME_CONTROLLER.get());
                    output.accept(ModItems.NEURAL_NODE.get());
                    output.accept(ModItems.SIMULATION_CHAMBER.get());
                    output.accept(ModItems.LOOT_FABRICATOR.get());
                    output.accept(ModItems.DATA_CABLE.get());
                    output.accept(ModItems.BLANK_DATA_MATRIX.get());
                    output.accept(ModItems.CRUDE_IMPRINT_BLANK.get());
                    output.accept(ModItems.REFINED_IMPRINT_BLANK.get());
                    output.accept(ModItems.PRISTINE_IMPRINT_BLANK.get());
                    output.accept(ModItems.RESONANT_CATALYST.get());
                    output.accept(ModItems.CORRUPTED_IMPRINT.get());
                    // Predictions are subject-bound, so a blank one is only useful for testing.
                })
                .build());
        TABS.register(eventBus);
    }
}
