package studio.akuro.simulacra.content.simulation;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The single item-handler face the Simulation Chamber shows to neighbours: substrate input slots in
 * front of the loot output slots. Automation can push substrate in and pull loot out, never the
 * reverse. The chamber writes to the backing handlers directly.
 */
public class ChamberItemHandler implements IItemHandler {
    private final IItemHandler input;
    private final IItemHandler output;

    public ChamberItemHandler(IItemHandler input, IItemHandler output) {
        this.input = input;
        this.output = output;
    }

    private boolean isInput(int slot) {
        return slot < input.getSlots();
    }

    private int outputSlot(int slot) {
        return slot - input.getSlots();
    }

    @Override
    public int getSlots() {
        return input.getSlots() + output.getSlots();
    }

    /**
     * Input slots report empty on purpose. Extractors survey with {@code getStackInSlot} and then
     * pull from whatever looked promising, so a slot that shows an item but refuses every extraction
     * jams them for good - Create's ItemHelper.extract does exactly this, and an outbound funnel
     * would stop the moment anything sat in the input. Insertion still sees the real stack through
     * insertItem and isItemValid, and the screen reads the backing handler rather than this face.
     */
    @Override
    public ItemStack getStackInSlot(int slot) {
        return isInput(slot) ? ItemStack.EMPTY : output.getStackInSlot(outputSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        // Only the substrate input accepts items from outside; the loot buffer is pull-only.
        return isInput(slot) ? input.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        // Only finished loot can be pulled; substrate cannot be siphoned back out.
        return isInput(slot) ? ItemStack.EMPTY : output.extractItem(outputSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return isInput(slot) ? input.getSlotLimit(slot) : output.getSlotLimit(outputSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isInput(slot) && input.isItemValid(slot, stack);
    }
}
