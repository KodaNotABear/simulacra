package studio.akuro.simulacra.compat;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;
import studio.akuro.simulacra.content.simulation.SubstrateTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Display Link sources, so a Create player can wire a machine to a Display Board or Nixie Tubes.
 *
 * <p>This is the piece Hostile Neural Networks cannot have and we can. Reading a farm's state from
 * across the base, on a sign made of blocks, is how Create players actually build control rooms —
 * and it is the difference between an add-on and a port. Both machines already know everything worth
 * showing; they were just keeping it to their own screens.
 *
 * <p>Sources are trimmed to the target's row count, so the same link reads sensibly on a single
 * Nixie tube row and on a full board.
 */
public class SimulacraDisplaySources {

    private SimulacraDisplaySources() {}

    /** Subject, grade, progress and stall reason for a Simulation Chamber. */
    public static class Chamber extends DisplaySource {

        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            if (!(context.getSourceBlockEntity() instanceof SimulationChamberBlockEntity chamber)) {
                return EMPTY;
            }
            List<MutableComponent> lines = new ArrayList<>();
            ItemStack model = chamber.getModel();
            if (model.isEmpty()) {
                lines.add(Component.translatable("gui.simulacra.chamber.no_model"));
            } else {
                lines.add(DataMatrixItem.getBoundMob(model)
                        .map(key -> DataMatrixItem.mobName(key).copy())
                        .orElse(Component.translatable("gui.simulacra.chamber.unbound")));
                int tier = DataMatrixItem.getModelTier(model);
                lines.add(DataMatrixItem.tierName(tier).copy());
                lines.add(Component.literal(Math.round(chamber.getProgressPercent()) + "%"));
                SubstrateTier substrate = chamber.currentTier();
                lines.add(substrate == null
                        ? Component.translatable("gui.simulacra.chamber.substrate_none")
                        : Component.translatable("gui.simulacra.substrate."
                                + substrate.name().toLowerCase()));
            }
            return trim(lines, stats);
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.simulation_chamber");
        }
    }

    /** What a Loot Fabricator is set to make, and how far through it is. */
    public static class Fabricator extends DisplaySource {

        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            if (!(context.getSourceBlockEntity() instanceof LootFabricatorBlockEntity fabricator)) {
                return EMPTY;
            }
            List<MutableComponent> lines = new ArrayList<>();
            ItemStack target = fabricator.getTarget();
            lines.add(target.isEmpty()
                    ? Component.translatable("gui.simulacra.fabricator_any")
                    : target.getHoverName().copy());
            lines.add(Component.literal(Math.round(fabricator.getProgressFraction() * 100f) + "%"));
            return trim(lines, stats);
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.loot_fabricator");
        }
    }

    /**
     * Cuts a reading down to what the target can actually show.
     *
     * <p>A Nixie tube row is one line and a Display Board can be many, and a source that hands back
     * more than fits is simply truncated by the target — losing the progress figure, which is the
     * line most worth having. Ordering matters more than completeness here, so the list is written
     * most-important-first and cut from the end.
     */
    private static List<MutableComponent> trim(List<MutableComponent> lines, DisplayTargetStats stats) {
        int rows = Math.max(1, stats.maxRows());
        return lines.size() <= rows ? lines : new ArrayList<>(lines.subList(0, rows));
    }
}
