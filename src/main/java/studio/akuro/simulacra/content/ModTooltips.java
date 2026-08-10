package studio.akuro.simulacra.content;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Item and block tooltips, following Create's convention.
 *
 * <p>Create hides a block's description behind Shift and shows a "Hold [Shift]" prompt instead, so an
 * inventory full of machines is readable at a glance and the detail is there when wanted. Printing the
 * summary unconditionally, which is what this mod used to do, makes every stack shout its manual.
 *
 * <p>The summary is word-wrapped through Create's own helper so line breaks land where they do on
 * Create's tooltips rather than wherever the translation happens to end.
 */
public final class ModTooltips {
    private ModTooltips() {}

    /**
     * Adds a Shift-gated summary line.
     *
     * @param key translation key holding the one-paragraph description
     */
    public static void addSummary(List<Component> tooltip, String key) {
        if (shiftDown()) {
            tooltip.addAll(TooltipHelper.cutTextComponent(
                    Component.translatable(key), FontHelper.Palette.GRAY_AND_WHITE));
        } else {
            tooltip.add(TooltipHelper.holdShift(FontHelper.Palette.GRAY_AND_WHITE, false));
        }
    }

    /**
     * Whether Shift is held, without dragging a client-only class onto a dedicated server.
     *
     * <p>{@code appendHoverText} lives in common code but is only ever rendered client-side. The dist
     * check short-circuits before {@code Screen} is touched, so the class is never resolved on a
     * server even if something asks an item for its tooltip there.
     */
    private static boolean shiftDown() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientOnly.shiftDown();
    }

    /** Isolated so the client class is only loaded once the dist check has passed. */
    private static final class ClientOnly {
        private static boolean shiftDown() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }
    }
}
