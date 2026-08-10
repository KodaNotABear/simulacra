package studio.akuro.simulacra.content.fabricator;

import com.simibubi.create.content.logistics.filter.FilterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.index.ModMenus;

import java.util.List;

/**
 * Screen contents for the Loot Fabricator, arranged after Hostile Neural Networks' loot fabricator.
 *
 * <p>Their layout solves this exact problem and is worth following: a single Prediction slot feeding
 * the machine, a preview of what is currently being made, a small paged palette of the subject's drops
 * to choose from, and a block of output slots for finished items. Items move in and out through the
 * block's item capability, so a funnel on one side and a funnel on the other is the intended build;
 * the slots here exist so a player can see and reach the same buffers.
 *
 * <p>The palette is nine ghost slots showing one page at a time rather than every drop at once. Ghost
 * slots ride the vanilla menu's own item syncing, so the server hands the client a live list without a
 * custom packet, and paging is a menu button — also vanilla plumbing.
 *
 * <p>Below the preview sits a Create filter slot, which this screen does not share with Hostile
 * Neural Networks. Their palette can only ever be worked by a player standing at the machine; a
 * filter is an item, so a build can set one.
 */
public class LootFabricatorMenu extends AbstractContainerMenu {

    /** Ghost slot previewing the current choice. Clicking it clears the selection. */
    public static final int PREVIEW_SLOT = 0;
    /** The Create filter, which outranks anything picked from the palette. */
    public static final int FILTER_SLOT = 1;
    /** The single Prediction input. */
    public static final int INPUT_SLOT = 2;
    /** First of the nine palette ghosts. */
    public static final int FIRST_CANDIDATE = 3;
    /** How many drops one page of the palette shows. */
    public static final int PAGE_SIZE = 9;

    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;

    private static final int CANDIDATES = LootFabricatorBlockEntity.CANDIDATE_SLOTS;
    /** Data layout: [0] page, [1] pages available, [2] predictions held, [3] progress 0-100, [4..] prices. */
    private static final int PAGE = 0;
    private static final int PAGES = 1;
    private static final int HELD = 2;
    private static final int PROGRESS = 3;
    private static final int FIRST_PRICE = 4;
    private static final int DATA_SIZE = FIRST_PRICE + PAGE_SIZE;

    private final LootFabricatorBlockEntity fabricator;
    private final Container previewDisplay = new SimpleContainer(1);
    private final Container candidateDisplay = new SimpleContainer(PAGE_SIZE);
    private final ContainerData data = new SimpleContainerData(DATA_SIZE);
    private int page = 0;

    public LootFabricatorMenu(int id, Inventory inventory, LootFabricatorBlockEntity fabricator) {
        super(ModMenus.LOOT_FABRICATOR.get(), id);
        this.fabricator = fabricator;

        addSlot(ghost(previewDisplay, 0, 8, 20));

        // A real slot, not a ghost: the filter item itself is the setting, so it has to be an item
        // the machine holds rather than a copy of one.
        addSlot(new SlotItemHandler(fabricator.getFilter(), 0, 8, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof FilterItem;
            }
        });

        addSlot(new SlotItemHandler(fabricator.getPredictions(), 0, 8, 68) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PredictionItem;
            }
        });

        for (int i = 0; i < PAGE_SIZE; i++) {
            addSlot(ghost(candidateDisplay, i, 30 + (i % 3) * 18, 20 + (i / 3) * 18));
        }

        // Finished items, four by four on the right. Pull-only: automation and players take from here,
        // nothing pushes in.
        for (int i = 0; i < fabricator.getOutput().getSlots(); i++) {
            addSlot(new SlotItemHandler(fabricator.getOutput(), i, 98 + (i % 4) * 18, 20 + (i / 4) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
        addDataSlots(data);
    }

    private int outputStart() {
        return FIRST_CANDIDATE + PAGE_SIZE;
    }

    private int machineEnd() {
        return outputStart() + fabricator.getOutput().getSlots();
    }

    private static Slot ghost(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        };
    }

    public int getPage() {
        return data.get(PAGE);
    }

    public int getPages() {
        return Math.max(1, data.get(PAGES));
    }

    /** Predictions held, which is what every price is measured against. */
    public int getHeld() {
        return data.get(HELD);
    }

    /** Progress toward the next item, 0-100. */
    public int getProgress() {
        return data.get(PROGRESS);
    }

    /** Cost of the drop in the given palette slot on the current page, or -1 if unknown. */
    public int getCandidatePrice(int index) {
        return index < 0 || index >= PAGE_SIZE ? -1 : data.get(FIRST_PRICE + index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int pages = Math.max(1, (candidateTotal() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (id == BUTTON_PREV) {
            page = Math.max(0, page - 1);
        } else if (id == BUTTON_NEXT) {
            page = Math.min(pages - 1, page + 1);
        } else {
            return false;
        }
        broadcastChanges();
        return true;
    }

    private int candidateTotal() {
        return fabricator.candidateDrops().size();
    }

    @Override
    public void broadcastChanges() {
        ItemStack sample = fabricator.anyPrediction();
        data.set(HELD, sample.isEmpty() ? 0 : fabricator.countPredictionsFor(sample));
        data.set(PROGRESS, Math.round(fabricator.getProgressFraction() * 100f));

        List<ItemStack> drops = fabricator.candidateDrops();
        int pages = Math.max(1, (drops.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            // The feed changed to a subject with fewer drops; do not strand the view on a dead page.
            page = 0;
        }
        data.set(PAGE, page);
        data.set(PAGES, pages);

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            ItemStack drop = index < drops.size() ? drops.get(index) : ItemStack.EMPTY;
            if (!ItemStack.matches(candidateDisplay.getItem(i), drop)) {
                candidateDisplay.setItem(i, drop.copy());
            }
            data.set(FIRST_PRICE + i, drop.isEmpty() ? -1 : fabricator.priceOf(drop.getItem()).orElse(-1));
        }
        ItemStack target = fabricator.getTarget();
        if (!ItemStack.matches(previewDisplay.getItem(0), target)) {
            previewDisplay.setItem(0, target.copy());
        }
        super.broadcastChanges();
    }

    /** Client constructor: the block entity is resolved from the position written by the server. */
    public LootFabricatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, resolve(inventory, buf.readBlockPos()));
    }

    private static LootFabricatorBlockEntity resolve(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof LootFabricatorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No Loot Fabricator at " + pos);
    }

    public LootFabricatorBlockEntity getFabricator() {
        return fabricator;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        // Ghost slots are decisions, not inventory: clicking one never moves an item, so nothing can
        // be lost to this screen.
        if (slotId == PREVIEW_SLOT) {
            fabricator.setTarget(ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        if (slotId >= FIRST_CANDIDATE && slotId < FIRST_CANDIDATE + PAGE_SIZE) {
            ItemStack picked = candidateDisplay.getItem(slotId - FIRST_CANDIDATE);
            if (!picked.isEmpty()) {
                fabricator.setTarget(picked);
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index == PREVIEW_SLOT
                || (index >= FIRST_CANDIDATE && index < outputStart())) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < machineEnd()) {
            if (!moveItemStackTo(stack, machineEnd(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof PredictionItem) {
            if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FilterItem) {
            if (!moveItemStackTo(stack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return !fabricator.isRemoved()
                && player.distanceToSqr(fabricator.getBlockPos().getCenter()) <= 64.0;
    }
}
