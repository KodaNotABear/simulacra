package studio.akuro.simulacra.compat;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.content.data.DataMatrixItem;
import studio.akuro.simulacra.content.fabricator.LootFabricatorBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;
import studio.akuro.simulacra.content.simulation.SubstrateTier;

/**
 * Display Link sources, one reading each.
 *
 * <p>Split rather than bundled. A source that returns four lines eats four rows of a board from a
 * single selection, and the player cannot reorder them, drop one, or put them on separate boards.
 * One reading per source lets them build the layout they want.
 *
 * <p>All of them allow labeling, which gives the Display Link's own text box so a player can type
 * their own prefix. That is why none of these lines carry a built-in caption.
 *
 * <p>Nothing here reports live progress. A job resets to zero every cycle, so it is a sawtooth, and
 * a link sampling it on an unrelated period lands on arbitrary points of the ramp. These readings
 * either hold still or only climb.
 */
public class SimulacraDisplaySources {

    private SimulacraDisplaySources() {}

    /** Shared plumbing: one line, always labelable. */
    private abstract static class Line extends SingleLineDisplaySource {
        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    private abstract static class ChamberLine extends Line {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            return context.getSourceBlockEntity() instanceof SimulationChamberBlockEntity chamber
                    ? read(chamber) : EMPTY_LINE;
        }

        protected abstract MutableComponent read(SimulationChamberBlockEntity chamber);
    }

    private abstract static class FabricatorLine extends Line {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            return context.getSourceBlockEntity() instanceof LootFabricatorBlockEntity fabricator
                    ? read(fabricator) : EMPTY_LINE;
        }

        protected abstract MutableComponent read(LootFabricatorBlockEntity fabricator);
    }

    /** Which mob the chamber is modelling. */
    public static class Subject extends ChamberLine {
        @Override
        protected MutableComponent read(SimulationChamberBlockEntity chamber) {
            ItemStack model = chamber.getModel();
            if (model.isEmpty()) {
                return Component.translatable("gui.simulacra.chamber.no_model");
            }
            return DataMatrixItem.getBoundMob(model)
                    .map(key -> DataMatrixItem.mobName(key).copy())
                    .orElse(Component.translatable("gui.simulacra.chamber.unbound"));
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.subject");
        }
    }

    /** The model's grade. */
    public static class Grade extends ChamberLine {
        @Override
        protected MutableComponent read(SimulationChamberBlockEntity chamber) {
            ItemStack model = chamber.getModel();
            return model.isEmpty() ? EMPTY_LINE
                    : DataMatrixItem.tierName(DataMatrixItem.getModelTier(model)).copy();
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.grade");
        }
    }

    /** Data gathered against what the next grade needs. Only ever climbs. */
    public static class Data extends ChamberLine {
        @Override
        protected MutableComponent read(SimulationChamberBlockEntity chamber) {
            ItemStack model = chamber.getModel();
            if (model.isEmpty()) {
                return EMPTY_LINE;
            }
            int next = DataMatrixItem.nextTierThreshold(model);
            return next > 0
                    ? Component.translatable("gui.simulacra.chamber.data", DataMatrixItem.getData(model), next)
                    : Component.translatable("gui.simulacra.chamber.data_max", DataMatrixItem.getData(model));
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.data");
        }
    }

    /** Which grade of blank is loaded. */
    public static class Substrate extends ChamberLine {
        @Override
        protected MutableComponent read(SimulationChamberBlockEntity chamber) {
            SubstrateTier tier = chamber.currentTier();
            return tier == null
                    ? Component.translatable("gui.simulacra.chamber.substrate_none")
                    : Component.translatable("gui.simulacra.substrate." + tier.name().toLowerCase());
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.substrate");
        }
    }

    /** What the chamber is doing, or why it stopped. */
    public static class Status extends ChamberLine {
        @Override
        protected MutableComponent read(SimulationChamberBlockEntity chamber) {
            SimulationChamberBlockEntity.StallReason stall = chamber.stallReason();
            return stall != SimulationChamberBlockEntity.StallReason.NONE
                    ? Component.translatable("gui.simulacra.chamber.stall." + stall.name().toLowerCase())
                    : Component.translatable("gui.simulacra.chamber.mode."
                            + chamber.getMode().name().toLowerCase());
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.status");
        }
    }

    /** What the Fabricator is set to make. */
    public static class Target extends FabricatorLine {
        @Override
        protected MutableComponent read(LootFabricatorBlockEntity fabricator) {
            ItemStack target = fabricator.getTarget();
            return target.isEmpty()
                    ? Component.translatable("gui.simulacra.fabricator_mode_roll")
                    : target.getHoverName().copy();
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.target");
        }
    }

    /**
     * Items a minute.
     *
     * <p>Output is exactly proportional to speed — one item per 9600/RPM ticks — so this is RPM over
     * eight, and it holds still between refreshes the way a board wants.
     */
    public static class Rate extends FabricatorLine {
        @Override
        protected MutableComponent read(LootFabricatorBlockEntity fabricator) {
            return Component.translatable("simulacra.display.rate",
                    String.format("%.1f", Math.abs(fabricator.getSpeed()) / 8f));
        }

        @Override
        public Component getName() {
            return Component.translatable("simulacra.display_source.rate");
        }
    }
}
