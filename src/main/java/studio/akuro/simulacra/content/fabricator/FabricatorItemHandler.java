package studio.akuro.simulacra.content.fabricator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The single face the Loot Fabricator shows to neighbours: Predictions in, finished items out, never
 * the reverse. Same shape as the chamber's handler, so funnels behave consistently across both
 * machines.
 *
 * <p>The filter slot rides along on the inbound half. Letting a build push a filter in is the whole
 * point of the slot — a target that only a player at the screen can set is not automation — but it
 * is deliberately not extractable, or an outbound funnel would steal the machine's own settings.
 */
public class FabricatorItemHandler implements IItemHandler {

    private final IItemHandler input;
    private final IItemHandler filter;
    private final IItemHandler output;

    public FabricatorItemHandler(IItemHandler input, IItemHandler filter, IItemHandler output) {
        this.input = input;
        this.filter = filter;
        this.output = output;
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
        return isInput(slot) || isFilter(slot)
                ? ItemStack.EMPTY : output.getStackInSlot(outputSlot(slot));
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
        return isInput(slot) || isFilter(slot)
                ? ItemStack.EMPTY : output.extractItem(outputSlot(slot), amount, simulate);
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
