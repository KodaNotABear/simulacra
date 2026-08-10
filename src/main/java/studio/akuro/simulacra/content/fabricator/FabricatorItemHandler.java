package studio.akuro.simulacra.content.fabricator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The single face the Loot Fabricator shows to neighbours: Predictions in, finished items out, never
 * the reverse. Same shape as the chamber's handler, so funnels behave consistently across both
 * machines.
 */
public class FabricatorItemHandler implements IItemHandler {

    private final IItemHandler input;
    private final IItemHandler output;

    public FabricatorItemHandler(IItemHandler input, IItemHandler output) {
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

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isInput(slot) ? input.getStackInSlot(slot) : output.getStackInSlot(outputSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isInput(slot) ? input.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
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
