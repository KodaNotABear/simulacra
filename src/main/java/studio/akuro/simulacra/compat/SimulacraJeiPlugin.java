package studio.akuro.simulacra.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.index.ModBlocks;
import studio.akuro.simulacra.index.ModItems;

/**
 * JEI integration.
 *
 * <p>Compiled against JEI's API only, and never referenced from the mod's own code — JEI finds this
 * class by scanning for {@link JeiPlugin}, so a pack without JEI installed never loads it and no
 * runtime dependency is needed.
 *
 * <p>Two jobs. The first is subtypes: a Data Matrix bound to a zombie and one bound to a creeper are
 * the same item with different data, and without an interpreter JEI collapses them into a single
 * entry, so searching for a mob finds nothing and recipe lookup on a bound matrix is meaningless.
 * The second is the information pages, which are where the chain gets explained — the loop here runs
 * across four machines and is not something recipe shapes can convey on their own.
 */
@JeiPlugin
public class SimulacraJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.BLANK_DATA_MATRIX.get(),
                bySubject(stack -> DataMatrixItem.getBoundMob(stack).orElse("")));
        registration.registerSubtypeInterpreter(ModItems.PREDICTION.get(),
                bySubject(stack -> PredictionItem.getSubject(stack).orElse("")));
    }

    /**
     * Groups stacks by whose data they hold and nothing else.
     *
     * <p>Deliberately ignores the data count and grade: a Coarse zombie matrix and a Self-Aware one
     * are the same ingredient as far as looking things up goes, and splitting on progress would fill
     * the ingredient list with a separate entry for every kill.
     */
    // getLegacyStringSubtypeInfo is deprecated but still abstract on the interface, so it has to be
    // implemented; it returns the same grouping key as the modern method.
    @SuppressWarnings("deprecation")
    private static ISubtypeInterpreter<ItemStack> bySubject(
            java.util.function.Function<ItemStack, String> subject) {
        return new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return subject.apply(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return subject.apply(stack);
            }
        };
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, ModBlocks.NEURAL_NODE.get(), "neural_node");
        info(registration, ModBlocks.MAINFRAME_CONTROLLER.get(), "mainframe_controller");
        info(registration, ModBlocks.DATA_CABLE.get(), "data_cable");
        info(registration, ModBlocks.SIMULATION_CHAMBER.get(), "simulation_chamber");
        info(registration, ModBlocks.LOOT_FABRICATOR.get(), "loot_fabricator");

        info(registration, ModItems.BLANK_DATA_MATRIX.get(), "blank_data_matrix");
        info(registration, ModItems.PREDICTION.get(), "prediction");
        info(registration, ModItems.CRUDE_IMPRINT_BLANK.get(), "imprint_blank");
        info(registration, ModItems.REFINED_IMPRINT_BLANK.get(), "imprint_blank");
        info(registration, ModItems.PRISTINE_IMPRINT_BLANK.get(), "imprint_blank");
        info(registration, ModItems.CORRUPTED_IMPRINT.get(), "corrupted_imprint");
        info(registration, ModItems.RESONANT_CATALYST.get(), "resonant_catalyst");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.simulacra." + key));
    }
}
