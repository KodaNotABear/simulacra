package studio.akuro.simulacra.content.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import studio.akuro.simulacra.SimulacraConfig;

/**
 * Feeds mob kills into the offhand Data Matrix. Registered on the NeoForge (game) event bus from the
 * main mod class.
 */
public final class DataMatrixEvents {
    private DataMatrixEvents() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player || dead.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ResourceLocation typeId = EntityType.getKey(dead.getType());
        // Bosses are rare kills, so each is worth far more data than a regular mob's.
        int data = dead.getType().is(Tags.EntityTypes.BOSSES) ? SimulacraConfig.BOSS_KILL_DATA.get() : 1;
        DataMatrixItem.recordKill(player, typeId, data);
    }
}
