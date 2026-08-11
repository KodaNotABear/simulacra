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
 * Display Link sources, so a machine can be wired to a Display Board or Nixie Tubes. Readings are
 * trimmed to the target's row count, so the same link works on one Nixie row and on a full board.
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
                // Not the job percentage. That is a sawtooth which resets every job, and a Display Link
                // samples on its own schedule, so an unrelated period lands on arbitrary points of the
                // ramp and the board reads as noise. Data toward the next grade only ever climbs.
                int next = DataMatrixItem.nextTierThreshold(model);
                lines.add(next > 0
                        ? Component.translatable("gui.simulacra.chamber.data",
                                DataMatrixItem.getData(model), next)
                        : Component.translatable("gui.simulacra.chamber.data_max",
                                DataMatrixItem.getData(model)));
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
            // A rate rather than a percentage, for the same reason: progress resets every item, so a
            // board sampling it shows arbitrary numbers. Output is exactly proportional to speed -
            // one item per 9600/RPM ticks, so RPM/8 items a minute - which is steady and is the
            // figure a player actually wants off a board.
            lines.add(Component.translatable("simulacra.display.rate",
                    String.format("%.1f", Math.abs(fabricator.getSpeed()) / 8f)));
            return trim(lines, stats);
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.loot_fabricator");
        }
    }

    /**
     * Cuts a reading down to what the target can show. The target truncates anything longer itself,
     * so the lists above are written most-important-first and cut from the end.
     */
    private static List<MutableComponent> trim(List<MutableComponent> lines, DisplayTargetStats stats) {
        int rows = Math.max(1, stats.maxRows());
        return lines.size() <= rows ? lines : new ArrayList<>(lines.subList(0, rows));
    }
}
