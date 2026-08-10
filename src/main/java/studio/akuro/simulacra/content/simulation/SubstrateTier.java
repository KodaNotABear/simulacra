package studio.akuro.simulacra.content.simulation;

import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.SimulacraConfig;
import studio.akuro.simulacra.index.ModItems;

/**
 * Grade of an imprint blank. Grade caps sim fidelity: higher tiers print more loot rolls per job,
 * and only pristine substrate can hold a boss-grade imprint (mobs in the {@code c:bosses} tag).
 */
public enum SubstrateTier {
    CRUDE(false),
    REFINED(false),
    PRISTINE(true);

    private final boolean allowsBosses;

    SubstrateTier(boolean allowsBosses) {
        this.allowsBosses = allowsBosses;
    }

    /** The tier of the given stack, or null if it is not an imprint blank. */
    public static SubstrateTier fromStack(ItemStack stack) {
        if (stack.is(ModItems.PRISTINE_IMPRINT_BLANK.get())) {
            return PRISTINE;
        }
        if (stack.is(ModItems.REFINED_IMPRINT_BLANK.get())) {
            return REFINED;
        }
        if (stack.is(ModItems.CRUDE_IMPRINT_BLANK.get())) {
            return CRUDE;
        }
        return null;
    }

    public boolean allowsBosses() {
        return allowsBosses;
    }

    /** Loot-table rolls printed per finished job; configurable per tier. */
    public int lootRolls() {
        return switch (this) {
            case CRUDE -> SimulacraConfig.LOOT_ROLLS_CRUDE.get();
            case REFINED -> SimulacraConfig.LOOT_ROLLS_REFINED.get();
            case PRISTINE -> SimulacraConfig.LOOT_ROLLS_PRISTINE.get();
        };
    }
}
