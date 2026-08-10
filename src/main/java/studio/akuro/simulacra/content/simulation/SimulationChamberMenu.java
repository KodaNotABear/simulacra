package studio.akuro.simulacra.content.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import studio.akuro.simulacra.index.ModMenus;

/**
 * Screen contents for the Simulation Chamber.
 *
 * <p>The bound matrix rides along as an inactive ghost slot. Inactive slots are skipped for drawing
 * and hit-testing but still ride the menu's item syncing, so the client gets a live copy of the
 * matrix (and therefore its subject, grade and data count) with no custom packet.
 */
public class SimulationChamberMenu extends AbstractContainerMenu {

    public static final int OUTPUT_SLOTS = 9;

    /** Data layout: [0] mode ordinal, [1] stall ordinal, [2] progress 0-100, [3] redstone mode. */
    private static final int MODE = 0;
    private static final int STALL = 1;
    private static final int PROGRESS = 2;
    private static final int REDSTONE = 3;
    private static final int DATA_SIZE = 4;

    /** Button id the screen sends to step the redstone mode on. */
    public static final int BUTTON_REDSTONE = 0;

    private final SimulationChamberBlockEntity chamber;
    private final Container modelDisplay = new SimpleContainer(1);
    private final ContainerData data = new SimpleContainerData(DATA_SIZE);

    public SimulationChamberMenu(int id, Inventory inventory, SimulationChamberBlockEntity chamber) {
        super(ModMenus.SIMULATION_CHAMBER.get(), id);
        this.chamber = chamber;

        // The bound matrix. Drawn by the screen at scale in the viewport, so it is never drawn here.
        addSlot(new Slot(modelDisplay, 0, 0, 0) {
            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new SlotItemHandler(chamber.getSubstrateHandler(), 0, 8, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SubstrateTier.fromStack(stack) != null;
            }
        });

        // Finished loot, one row of nine. Pull-only: the chamber fills these itself.
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(chamber.getOutputHandler(), i, 8 + i * 18, 112) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 146 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 204));
        }

        addDataSlots(data);
    }

    /** Client constructor: the menu type hands us a buffer holding the chamber's position. */
    public SimulationChamberMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, resolve(inventory, buf.readBlockPos()));
    }

    private static SimulationChamberBlockEntity resolve(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber) {
            return chamber;
        }
        throw new IllegalStateException("No Simulation Chamber at " + pos);
    }

    @Override
    public void broadcastChanges() {
        modelDisplay.setItem(0, chamber.getModel());
        data.set(MODE, chamber.getMode().ordinal());
        data.set(STALL, chamber.stallReason().ordinal());
        data.set(PROGRESS, (int) chamber.getProgressPercent());
        data.set(REDSTONE, chamber.getRedstoneMode().ordinal());
        super.broadcastChanges();
    }

    /** The bound matrix, or empty. Carries the subject, grade and data count the screen reads. */
    public ItemStack getModel() {
        return getSlot(0).getItem();
    }

    public SimulationChamberBlockEntity.Mode getMode() {
        return SimulationChamberBlockEntity.Mode.values()[
                Math.floorMod(data.get(MODE), SimulationChamberBlockEntity.Mode.values().length)];
    }

    public SimulationChamberBlockEntity.StallReason getStallReason() {
        return SimulationChamberBlockEntity.StallReason.values()[
                Math.floorMod(data.get(STALL), SimulationChamberBlockEntity.StallReason.values().length)];
    }

    /** Progress through the current job, 0-100. */
    public int getProgress() {
        return data.get(PROGRESS);
    }

    public SimulationChamberBlockEntity.RedstoneMode getRedstoneMode() {
        SimulationChamberBlockEntity.RedstoneMode[] all =
                SimulationChamberBlockEntity.RedstoneMode.values();
        return all[Math.floorMod(data.get(REDSTONE), all.length)];
    }

    /**
     * Steps the redstone mode through vanilla's button plumbing: no custom packet, and the client
     * cannot set the mode itself.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_REDSTONE || !stillValid(player)) {
            return false;
        }
        chamber.cycleRedstoneMode();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int machineEnd = 1 + OUTPUT_SLOTS + 1;      // ghost, substrate, outputs
        int playerEnd = machineEnd + 36;
        if (index < machineEnd) {
            if (!moveItemStackTo(stack, machineEnd, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
            // Only the substrate slot accepts anything from the player; outputs are pull-only.
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return !chamber.isRemoved()
                && player.distanceToSqr(chamber.getBlockPos().getCenter()) <= 64.0;
    }
}
