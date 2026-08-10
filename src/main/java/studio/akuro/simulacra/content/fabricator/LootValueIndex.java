package studio.akuro.simulacra.content.fabricator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import studio.akuro.simulacra.SimulacraConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Works out what each of a mob's drops is worth, in Predictions.
 *
 * <p>The Loot Fabricator lets a player pick which drop to take, which would wreck the game's rarity
 * curve if every item cost the same: nobody would ever choose rotten flesh over an iron ingot. So the
 * price of an item is derived from how often the mob actually drops it. Sample the loot table enough
 * times to get a stable average yield per roll, then charge {@code 1 / yield} Predictions for it.
 *
 * <p>The effect is that spending Predictions is value-neutral against simply rolling the table - you
 * are choosing the shape of the reward, not the size of it. A zombie's rotten flesh comes out at
 * roughly one Prediction; its player-only iron ingot costs dozens.
 *
 * <p>Deriving this rather than hand-authoring a table per mob is what lets the Fabricator work for
 * any mob at all, including modded ones the mod has never heard of.
 */
public class LootValueIndex {

    /** Averaged yields per mob, built on first use and dropped whenever datapacks reload. */
    private static final Map<ResourceLocation, Map<Item, Double>> CACHE = new HashMap<>();

    private LootValueIndex() {
    }

    /** Datapack reload can rewrite any loot table, so previously derived prices are void. */
    public static void invalidate() {
        CACHE.clear();
    }

    /**
     * Average number of each item the given mob yields per loot roll. Empty when the mob has no
     * usable loot table.
     */
    public static Map<Item, Double> yields(ServerLevel level, EntityType<?> type, LootTable table) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Map<Item, Double> cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        Map<Item, Double> totals = new HashMap<>();
        int samples = SimulacraConfig.FABRICATOR_SAMPLES.get();
        Entity probe = type.create(level);
        if (!(probe instanceof LivingEntity living) || table == LootTable.EMPTY) {
            if (probe != null) {
                probe.discard();
            }
            CACHE.put(id, Map.of());
            return Map.of();
        }

        Vec3 origin = Vec3.ZERO;
        living.moveTo(origin.x, origin.y, origin.z, 0f, 0f);
        FakePlayer killer = FakePlayerFactory.getMinecraft(level);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);

        for (int i = 0; i < samples; i++) {
            List<ItemStack> rolled = table.getRandomItems(params);
            for (ItemStack stack : rolled) {
                if (!stack.isEmpty()) {
                    totals.merge(stack.getItem(), (double) stack.getCount(), Double::sum);
                }
            }
        }
        living.discard();

        Map<Item, Double> averaged = new HashMap<>();
        totals.forEach((item, total) -> averaged.put(item, total / samples));
        CACHE.put(id, Map.copyOf(averaged));
        return averaged;
    }

    /**
     * Predictions needed to fabricate one of {@code item} from this mob, or empty when the mob never
     * drops it. Always at least one, so nothing is free no matter how common.
     */
    public static Optional<Integer> price(ServerLevel level, EntityType<?> type, LootTable table, Item item) {
        Double yield = yields(level, type, table).get(item);
        if (yield == null || yield <= 0.0) {
            return Optional.empty();
        }
        double base = SimulacraConfig.FABRICATOR_PRICE_BASE.get();
        return Optional.of(Math.max(1, (int) Math.round(base / yield)));
    }
}
