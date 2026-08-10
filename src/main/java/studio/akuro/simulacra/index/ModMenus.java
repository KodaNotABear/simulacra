package studio.akuro.simulacra.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.fabricator.LootFabricatorMenu;
import studio.akuro.simulacra.content.simulation.SimulationChamberMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Simulacra.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LootFabricatorMenu>> LOOT_FABRICATOR =
            MENUS.register("loot_fabricator",
                    () -> IMenuTypeExtension.create(LootFabricatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SimulationChamberMenu>> SIMULATION_CHAMBER =
            MENUS.register("simulation_chamber",
                    () -> IMenuTypeExtension.create(SimulationChamberMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
