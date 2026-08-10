package studio.akuro.simulacra.index;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.data.PredictionItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Simulacra.MOD_ID);

    // Block items.
    public static final DeferredItem<BlockItem> NEURAL_NODE =
            ITEMS.registerSimpleBlockItem("neural_node", ModBlocks.NEURAL_NODE);

    public static final DeferredItem<BlockItem> MAINFRAME_CONTROLLER =
            ITEMS.registerSimpleBlockItem("mainframe_controller", ModBlocks.MAINFRAME_CONTROLLER);

    public static final DeferredItem<BlockItem> SIMULATION_CHAMBER =
            ITEMS.registerSimpleBlockItem("simulation_chamber", ModBlocks.SIMULATION_CHAMBER);

    public static final DeferredItem<BlockItem> LOOT_FABRICATOR =
            ITEMS.registerSimpleBlockItem("loot_fabricator", ModBlocks.LOOT_FABRICATOR);

    public static final DeferredItem<BlockItem> DATA_CABLE =
            ITEMS.registerSimpleBlockItem("data_cable", ModBlocks.DATA_CABLE);

    // Blank Data Matrix: the empty model you bind to a mob and fill with data by killing it.
    /** One roll of a subject's loot table, in the abstract. Spent at the Loot Fabricator. */
    public static final DeferredItem<PredictionItem> PREDICTION =
            ITEMS.register("prediction", () -> new PredictionItem(new Item.Properties()));

    public static final DeferredItem<DataMatrixItem> BLANK_DATA_MATRIX =
            ITEMS.register("blank_data_matrix", () -> new DataMatrixItem(new Item.Properties().stacksTo(1)));
    // Transitional item for the matrix's Sequenced Assembly line; Create's class adds the progress tooltip.
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_DATA_MATRIX =
            ITEMS.register("incomplete_data_matrix", () -> new SequencedAssemblyItem(new Item.Properties()));
    // Imprint blanks: the substrate a running simulation consumes to print loot onto. Higher grades
    // yield more loot rolls per job; only pristine can hold a boss-grade imprint.
    public static final DeferredItem<Item> CRUDE_IMPRINT_BLANK = ITEMS.registerSimpleItem("crude_imprint_blank");
    public static final DeferredItem<Item> REFINED_IMPRINT_BLANK = ITEMS.registerSimpleItem("refined_imprint_blank");
    public static final DeferredItem<Item> PRISTINE_IMPRINT_BLANK = ITEMS.registerSimpleItem("pristine_imprint_blank");
    // Resonant Catalyst: a chance print from boss simulations; compacts refined blanks into pristine,
    // making the pristine tier renewable once the boss loop is running.
    public static final DeferredItem<Item> RESONANT_CATALYST = ITEMS.registerSimpleItem("resonant_catalyst");
    // Corrupted Imprint: what a botched simulation prints instead of loot; crushes back to pulp.
    public static final DeferredItem<Item> CORRUPTED_IMPRINT = ITEMS.registerSimpleItem("corrupted_imprint");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
