package studio.akuro.simulacra.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.data.PredictionItem;
import studio.akuro.simulacra.content.fabricator.LootFabricatorMenu;

/**
 * The Loot Fabricator's screen, arranged after Hostile Neural Networks' loot fabricator.
 *
 * <p>Their solution to the same problem is worth following: the subject is named at the top, a small
 * paged palette offers its drops, the current choice sits in a preview slot beside it, and finished
 * items fill a block on the right. Each drop carries its price and anything unaffordable is dimmed, so
 * the decision is made by looking rather than by reading — which is why there is almost no prose here.
 *
 * <p>The arrangement is borrowed; the art is this mod's own.
 */
public class LootFabricatorScreen extends AbstractContainerScreen<LootFabricatorMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, "textures/gui/loot_fabricator.png");

    private static final int ACCENT = 0xFF6BA84F;
    private static final int UNAFFORDABLE = 0x60201010;

    private Button prev;
    private Button next;

    public LootFabricatorScreen(LootFabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 187;
        this.inventoryLabelY = 94;
    }

    @Override
    protected void init() {
        super.init();
        // Paging only exists when a subject drops more than one page's worth, which most do not.
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> page(LootFabricatorMenu.BUTTON_PREV))
                .bounds(leftPos + 30, topPos + 76, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> page(LootFabricatorMenu.BUTTON_NEXT))
                .bounds(leftPos + 64, topPos + 76, 20, 16).build());
    }

    private void page(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean paged = menu.getPages() > 1;
        prev.visible = paged;
        next.visible = paged;
        prev.active = menu.getPage() > 0;
        next.active = menu.getPage() < menu.getPages() - 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The six-argument blit assumes a 256x256 sheet. This texture is exactly the panel, so the
        // size has to be passed explicitly or it samples a fraction of the image and stretches it.
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        // Whose drop table this is, right-aligned in the title bar. Without it the palette is items
        // from nowhere in particular; drawn anywhere else on this panel it lands on a slot, and drawn
        // at the left it runs straight through the window title.
        ItemStack loaded = menu.getFabricator().anyPrediction();
        Component header = loaded.isEmpty()
                ? Component.translatable("gui.simulacra.fabricator_load")
                : PredictionItem.getSubject(loaded)
                        .map(DataMatrixItem::mobName)
                        .orElse(Component.translatable("tooltip.simulacra.prediction_unbound"));
        // Leave a gap after the machine name. Without it a full-width header is drawn hard against
        // the title and the two read as one run-on string.
        String text = fit(header.getString(), imageWidth - 16 - font.width(title) - 6);
        graphics.drawString(font, text, imageWidth - 8 - font.width(text), titleLabelY, 0xF0E4CE, false);

        // Which of the two modes the machine is in, said outright. The accent ring below marks the
        // same thing, but a ring only means something to a player who already knows what it marks,
        // and the tooltip that explains it has to be hunted for. This sits in the strip of bare panel
        // beside the inventory label — the only gap wide enough for a line of text — and is
        // right-aligned under the subject name, so the two facts about the current job share an edge.
        ItemStack target = menu.getFabricator().getTarget();
        Component mode = target.isEmpty()
                ? Component.translatable("gui.simulacra.fabricator_mode_roll")
                : Component.translatable("gui.simulacra.fabricator_mode_make", target.getHoverName());
        String modeText = fit(mode.getString(),
                imageWidth - 8 - inventoryLabelX - font.width(playerInventoryTitle) - 6);
        graphics.drawString(font, modeText, imageWidth - 8 - font.width(modeText),
                inventoryLabelY, 0x404040, false);

        // An empty preview means "roll the table", not "off". Said in words it overlapped the palette
        // — the gap beside the preview is 24px and the shortest honest phrasing is nearly twice that —
        // so the mode is marked the same way a chosen drop is, with the accent ring. Hovering explains.
        Slot preview = menu.slots.get(LootFabricatorMenu.PREVIEW_SLOT);
        if (!preview.hasItem()) {
            outline(graphics, preview.x - 1, preview.y - 1, 18, 18, ACCENT);
        }

        // Vertical progress, filling upward, between the palette and the output block.
        int filled = Math.round(menu.getProgress() / 100f * 52f);
        if (filled > 0) {
            graphics.fill(88, 73 - filled, 92, 73, ACCENT);
        }

        for (int i = 0; i < LootFabricatorMenu.PAGE_SIZE; i++) {
            Slot slot = menu.slots.get(LootFabricatorMenu.FIRST_CANDIDATE + i);
            if (!slot.hasItem()) {
                continue;
            }
            int price = menu.getCandidatePrice(i);
            boolean affordable = affordable(i);
            if (!affordable) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, UNAFFORDABLE);
            }
            if (!target.isEmpty() && ItemStack.isSameItem(target, slot.getItem())) {
                outline(graphics, slot.x - 1, slot.y - 1, 18, 18, ACCENT);
            }
            if (price > 0) {
                String label = price > 99 ? "99+" : String.valueOf(price);
                graphics.pose().pushPose();
                graphics.pose().translate(0f, 0f, 200f);
                graphics.drawString(font, label, slot.x + 17 - font.width(label), slot.y + 9,
                        affordable ? 0xFFFFFF : 0xFFFF8080, true);
                graphics.pose().popPose();
            }
        }
    }

    /** Shortens a line to fit, the way the subject name in the title bar has always been shortened. */
    private String fit(String text, int available) {
        return font.width(text) <= available
                ? text
                : font.plainSubstrByWidth(text, available - font.width("...")) + "...";
    }

    /** Whether the drop in the given palette slot can be paid for out of what is loaded. */
    private boolean affordable(int index) {
        int price = menu.getCandidatePrice(index);
        return price > 0 && menu.getHeld() >= price;
    }

    /**
     * The palette is ghost slots routed through the menu, not buttons, so nothing in vanilla makes a
     * noise when one is picked — a screen whose only interactive part is silent reads as broken.
     *
     * <p>Sounded here rather than in the menu because this is the click the player made: the menu also
     * runs server-side, where there is nobody to hear it.
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        announce(slotId);
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    private void announce(int slotId) {
        if (minecraft == null || slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }
        if (slotId == LootFabricatorMenu.PREVIEW_SLOT) {
            // Clicking the preview always ends in the rolling mode, whether it cleared a choice or
            // confirmed there was none, so it always acknowledges.
            play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
            return;
        }
        int index = slotId - LootFabricatorMenu.FIRST_CANDIDATE;
        if (index < 0 || index >= LootFabricatorMenu.PAGE_SIZE || !menu.slots.get(slotId).hasItem()) {
            return;
        }
        // A drop that cannot be paid for is dimmed on screen; say so out loud as well, or clicking one
        // is indistinguishable from clicking a dead panel.
        if (affordable(index)) {
            play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        } else {
            play(SoundEvents.DISPENSER_FAIL, 0.8f);
        }
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    private static void outline(GuiGraphics graphics, int x, int y, int w, int h, int colour) {
        graphics.fill(x, y, x + w, y + 1, colour);
        graphics.fill(x, y + h - 1, x + w, y + h, colour);
        graphics.fill(x, y, x + 1, y + h, colour);
        graphics.fill(x + w - 1, y, x + w, y + h, colour);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // The preview explains the two modes: empty is a roll, filled is a guaranteed item.
        if (hoveredSlot != null && menu.slots.indexOf(hoveredSlot) == LootFabricatorMenu.PREVIEW_SLOT
                && !hoveredSlot.hasItem()) {
            graphics.renderTooltip(font, java.util.List.of(
                    Component.translatable("gui.simulacra.fabricator_any"),
                    Component.translatable("gui.simulacra.fabricator_any_hint")
                            .withStyle(ChatFormatting.GRAY)
            ), java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        // A hovered drop explains its own cost, so the window never has to.
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            int index = menu.slots.indexOf(hoveredSlot) - LootFabricatorMenu.FIRST_CANDIDATE;
            if (index >= 0 && index < LootFabricatorMenu.PAGE_SIZE) {
                int price = menu.getCandidatePrice(index);
                if (price > 0) {
                    graphics.renderTooltip(font, java.util.List.of(
                            hoveredSlot.getItem().getHoverName(),
                            Component.translatable("gui.simulacra.fabricator_cost", price)
                                    .withStyle(menu.getHeld() >= price
                                            ? ChatFormatting.GRAY : ChatFormatting.RED)
                    ), java.util.Optional.empty(), mouseX, mouseY);
                    return;
                }
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
