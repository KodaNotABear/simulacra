package studio.akuro.simulacra.content.fabricator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The single face the Loot Fabricator shows to neighbours: Predictions in, finished items out, never
 * the reverse. Same shape as the chamber's handler, so funnels behave consistently across both
 * machines.
 *
 * <p>The filter slot rides along on the inbound half, and how much of it shows depends on the face.
 * From the top it is visible and extractable, so a build can swap the machine's target; from every
 * other face it is hidden and insert-only, so an outbound funnel cannot steal the settings.
 *
 * <p>Visible and extractable move together on purpose. A slot that shows an item but refuses to give
 * it up is the permanent jam this class already guards against elsewhere.
 */
public class FabricatorItemHandler implements IItemHandler {

    private final IItemHandler input;
    private final IItemHandler filter;
    private final IItemHandler output;
    /** Whether this view lets the filter be seen and taken back out. */
    private final boolean filterExposed;

    public FabricatorItemHandler(IItemHandler input, IItemHandler filter, IItemHandler output,
                                 boolean filterExposed) {
        this.input = input;
        this.filter = filter;
        this.output = output;
        this.filterExposed = filterExposed;
    }

    private boolean isInput(int slot) {
        return slot < input.getSlots();
    }

    private boolean isFilter(int slot) {
        return !isInput(slot) && slot < input.getSlots() + filter.getSlots();
    }

    private int filterSlot(int slot) {
        return slot - input.getSlots();
    }

    private int outputSlot(int slot) {
        return slot - input.getSlots() - filter.getSlots();
    }

    @Override
    public int getSlots() {
        return input.getSlots() + filter.getSlots() + output.getSlots();
    }

    /**
     * Inbound slots report empty to this face, deliberately.
     *
     * <p>Extractors survey a handler with {@code getStackInSlot} and then pull from whatever slot
     * looked promising. An input slot that shows a Prediction but refuses every extraction is a slot
     * they can get stuck on forever - Create's own ItemHelper.extract works exactly this way, so an
     * outbound funnel would drain the output until the moment something sat in the input, then stop
     * for good. Hiding the contents costs nothing here: insertion goes through insertItem and
     * isItemValid, which still see the real stack, and the screen reads the handler directly rather
     * than through this face.
     */
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (isInput(slot)) {
            return ItemStack.EMPTY;
        }
        if (isFilter(slot)) {
            return filterExposed ? filter.getStackInSlot(filterSlot(slot)) : ItemStack.EMPTY;
        }
        return output.getStackInSlot(outputSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (isInput(slot)) {
            return input.insertItem(slot, stack, simulate);
        }
        return isFilter(slot) ? filter.insertItem(filterSlot(slot), stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isInput(slot)) {
            return ItemStack.EMPTY;
        }
        if (isFilter(slot)) {
            return filterExposed ? filter.extractItem(filterSlot(slot), amount, simulate) : ItemStack.EMPTY;
        }
        return output.extractItem(outputSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (isInput(slot)) {
            return input.getSlotLimit(slot);
        }
        return isFilter(slot) ? filter.getSlotLimit(filterSlot(slot)) : output.getSlotLimit(outputSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (isInput(slot)) {
            return input.isItemValid(slot, stack);
        }
        return isFilter(slot) && filter.isItemValid(filterSlot(slot), stack);
    }
}
