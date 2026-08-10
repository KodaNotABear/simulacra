package studio.akuro.simulacra.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberMenu;
import studio.akuro.simulacra.content.simulation.SubstrateTier;

/**
 * The Simulation Chamber's screen: what it is modelling, how well, what it is printing onto, and why
 * it has stopped if it has.
 *
 * <p>Mostly a readout, which is what Hostile Neural Networks' equivalent screen is — theirs is a
 * block of text and slots with no mob on it at all. The viewport is the one addition, and it earns
 * its place because the chamber is where a subject is trained: seeing which mob is bound is the first
 * question anyone opening this block has.
 *
 * <p>It draws the bound Data Matrix at scale rather than an entity directly. The matrix already knows
 * how to render its own subject, so drawing the item large gets the mob and its platform for free,
 * and there is exactly one piece of code deciding how a subject looks anywhere in the mod.
 */
public class SimulationChamberScreen extends AbstractContainerScreen<SimulationChamberMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Simulacra.MOD_ID, "textures/gui/simulation_chamber.png");

    /** The recessed well the subject is drawn into. */
    private static final int VIEW_X = 7;
    private static final int VIEW_Y = 20;
    private static final int VIEW_SIZE = 46;

    /** The readout column beside the viewport. */
    private static final int TEXT_X = 58;
    private static final int LINE_1 = 22;
    private static final int LINE_STEP = 11;

    /** Interior of the progress track, inside its bevel. Full width, so it reads at a glance. */
    private static final int BAR_X = 8;
    private static final int BAR_Y = 98;
    private static final int BAR_W = 160;
    private static final int BAR_H = 6;

    /** The two full-width lines beside the substrate slot, which the narrow column cannot hold. */
    private static final int WIDE_X = 32;
    private static final int WIDE_1 = 75;
    private static final int WIDE_2 = 86;

    /** Outer bevel the inventory rows land on, and therefore what everything else aligns to. */
    private static final int RIGHT_EDGE = 169;

    /** The redstone button, and the edge the readout column stops at to make room for it. */
    private static final int BUTTON_X = 138;
    private static final int BUTTON_Y = 20;
    private static final int BUTTON_W = 30;
    private static final int COLUMN_EDGE = BUTTON_X - 4;

    private net.minecraft.client.gui.components.Button redstone;

    private static final int TEXT = 0x404040;
    private static final int TEXT_DIM = 0x6E6A60;
    private static final int SUBJECT = 0x2E4B5E;
    private static final int FILL = 0xFF6FC3A6;
    private static final int STALLED = 0xFFC46A5A;

    public SimulationChamberScreen(SimulationChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 136;
    }

    @Override
    protected void init() {
        super.init();
        redstone = addRenderableWidget(net.minecraft.client.gui.components.Button
                .builder(Component.empty(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId, SimulationChamberMenu.BUTTON_REDSTONE);
                    }
                })
                .bounds(leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_W, 16)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.simulacra.chamber.redstone.tooltip")))
                .build());
        refreshRedstoneButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshRedstoneButton();
    }

    /** Keeps the button's face and tooltip on the mode the server actually holds. */
    private void refreshRedstoneButton() {
        SimulationChamberBlockEntity.RedstoneMode mode = menu.getRedstoneMode();
        redstone.setMessage(Component.translatable(
                "gui.simulacra.chamber.redstone." + mode.name().toLowerCase() + ".short"));
        redstone.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.simulacra.chamber.redstone."
                        + mode.name().toLowerCase())));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The six-argument blit assumes a 256x256 sheet. This texture is exactly the panel, so the
        // size has to be passed explicitly or it samples a corner of an imagined 256x256
        // sheet and stretches that over the panel — the background then disagrees with every slot
        // position, which reads as the whole screen being scaled wrong.
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, imageWidth, imageHeight);

        int progress = menu.getProgress();
        if (progress > 0) {
            int filled = Math.max(1, BAR_W * progress / 100);
            boolean stalled = menu.getStallReason() != SimulationChamberBlockEntity.StallReason.NONE;
            graphics.fill(leftPos + BAR_X, topPos + BAR_Y,
                    leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H,
                    stalled ? STALLED : FILL);
        }

        renderSubject(graphics);
    }

    /** Draws the bound matrix large in the viewport, or a hint that there is nothing bound yet. */
    private void renderSubject(GuiGraphics graphics) {
        ItemStack model = menu.getModel();
        if (model.isEmpty()) {
            Component hint = Component.translatable("gui.simulacra.chamber.no_model");
            int wrapped = VIEW_SIZE - 8;
            int y = topPos + VIEW_Y + VIEW_SIZE / 2 - font.wordWrapHeight(hint, wrapped) / 2;
            graphics.drawWordWrap(font, hint, leftPos + VIEW_X + 4, y, wrapped, 0xA0A8AC);
            return;
        }

        // 2.5x lands a 16-pixel item at 40 pixels, which leaves a margin inside the 46-pixel well.
        float scale = 2.5f;
        int size = (int) (16 * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(
                leftPos + VIEW_X + (VIEW_SIZE - size) / 2f,
                topPos + VIEW_Y + (VIEW_SIZE - size) / 2f,
                0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.renderItem(model, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xF0E0C8, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);

        int line = 0;
        ItemStack model = menu.getModel();
        if (model.isEmpty()) {
            drawLine(graphics, line, Component.translatable("gui.simulacra.chamber.no_model"), TEXT_DIM);
        } else {
            Component subject = DataMatrixItem.getBoundMob(model)
                    .map(DataMatrixItem::mobName)
                    .orElse(Component.translatable("gui.simulacra.chamber.unbound"));
            drawLine(graphics, line++, subject, SUBJECT);

            // Grade and accuracy get a line each. Joined they were the longest string on the panel
            // and truncated every time; apart, neither comes close to the column width.
            int tier = DataMatrixItem.getModelTier(model);
            drawLine(graphics, line++, DataMatrixItem.tierName(tier), TEXT);
            drawLine(graphics, line++, Component.translatable("gui.simulacra.chamber.accuracy",
                    Math.round(DataMatrixItem.accuracy(model) * 100f)), TEXT);
            drawLine(graphics, line, dataLine(model, tier), TEXT);
        }

        // Substrate and status run the full width of the panel rather than the column, because both
        // carry a name that a hundred and ten pixels cannot hold.
        drawFitted(graphics, substrateLine(), WIDE_X, WIDE_1, TEXT_DIM);
        drawFitted(graphics, statusLine(), WIDE_X, WIDE_2,
                menu.getStallReason() == SimulationChamberBlockEntity.StallReason.NONE ? TEXT : 0xA03528);
    }

    /**
     * How much data the matrix has, and what it is counting toward.
     *
     * <p>{@code nextTierThreshold} returns -1 both for an untrained matrix and for a maxed one, so
     * reading it alone reports a brand new matrix as fully trained. Untrained ones count toward being
     * trained at all, which is a different number.
     */
    private static Component dataLine(ItemStack model, int tier) {
        int data = DataMatrixItem.getData(model);
        if (tier == 0) {
            return Component.translatable("gui.simulacra.chamber.data", data,
                    DataMatrixItem.dataToTrain());
        }
        int next = DataMatrixItem.nextTierThreshold(model);
        return next > 0
                ? Component.translatable("gui.simulacra.chamber.data", data, next)
                : Component.translatable("gui.simulacra.chamber.data_max", data);
    }

    /** What is loaded to print onto, by tier and count. */
    private Component substrateLine() {
        ItemStack substrate = menu.getSlot(1).getItem();
        SubstrateTier loaded = SubstrateTier.fromStack(substrate);
        return loaded == null
                ? Component.translatable("gui.simulacra.chamber.substrate_none")
                // The tier name, not the item name: the tier is the only part of it that changes
                // what the chamber can do, and the full name overruns even the wide row.
                : Component.translatable("gui.simulacra.chamber.substrate",
                        Component.translatable("gui.simulacra.substrate." + loaded.name().toLowerCase()),
                        substrate.getCount());
    }

    /** One line of the column beside the viewport. */
    private void drawLine(GuiGraphics graphics, int index, Component text, int colour) {
        drawFitted(graphics, text, TEXT_X, LINE_1 + index * LINE_STEP, colour, COLUMN_EDGE);
    }

    /**
     * Draws a line, shortened if it would run past the panel edge.
     *
     * <p>Every string here is written to fit, but mob names come from whatever a pack contains and
     * a translation can be any length, so nothing is trusted to fit on its own. The full text is
     * always available by hovering the viewport.
     */
    private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int colour) {
        drawFitted(graphics, text, x, y, colour, RIGHT_EDGE);
    }

    private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int colour, int edge) {
        String s = text.getString();
        int room = edge - x;
        if (font.width(s) > room) {
            s = font.plainSubstrByWidth(s, room - font.width("...")) + "...";
        }
        graphics.drawString(font, s, x, y, colour, false);
    }

    /** What the chamber is doing, or the single reason it is not. */
    private Component statusLine() {
        SimulationChamberBlockEntity.StallReason stall = menu.getStallReason();
        if (stall != SimulationChamberBlockEntity.StallReason.NONE) {
            // No bold: the row is already drawn red, and bold widens every glyph on the row that
            // has the least space to give.
            return Component.translatable("gui.simulacra.chamber.stall." + stall.name().toLowerCase());
        }
        return Component.translatable("gui.simulacra.chamber.mode." + menu.getMode().name().toLowerCase());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Hovering the subject gives the whole readout untruncated. The panel is written to fit, but
        // a long mob name or a translation can still overrun it, and a shortened line the player has
        // no way to finish reading is worse than one more tooltip.
        if (mouseX >= leftPos + VIEW_X && mouseX < leftPos + VIEW_X + VIEW_SIZE
                && mouseY >= topPos + VIEW_Y && mouseY < topPos + VIEW_Y + VIEW_SIZE) {
            graphics.renderComponentTooltip(font, readout(), mouseX, mouseY);
        }
    }

    /** The full, unshortened readout, for the viewport's tooltip. */
    private java.util.List<Component> readout() {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        ItemStack model = menu.getModel();
        if (model.isEmpty()) {
            lines.add(Component.translatable("gui.simulacra.chamber.no_model"));
        } else {
            lines.add(DataMatrixItem.getBoundMob(model)
                    .map(DataMatrixItem::mobName)
                    .orElse(Component.translatable("gui.simulacra.chamber.unbound")));
            int tier = DataMatrixItem.getModelTier(model);
            lines.add(DataMatrixItem.tierName(tier).copy().withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.simulacra.chamber.accuracy",
                    Math.round(DataMatrixItem.accuracy(model) * 100f)).withStyle(ChatFormatting.GRAY));
            lines.add(dataLine(model, tier).copy().withStyle(ChatFormatting.GRAY));
        }
        lines.add(substrateLine().copy().withStyle(ChatFormatting.GRAY));
        lines.add(statusLine().copy().withStyle(
                menu.getStallReason() == SimulationChamberBlockEntity.StallReason.NONE
                        ? ChatFormatting.GRAY : ChatFormatting.RED));
        return lines;
    }
}
