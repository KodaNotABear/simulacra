package studio.akuro.simulacra.content.data;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A unit of simulated value for one specific mob, printed by the Simulation Chamber in place of loot.
 * One Prediction is worth one roll of its subject's table, whatever that contains; which drop to
 * actually take is chosen later at the Loot Fabricator, priced by how often the mob drops it.
 *
 * <p>Filtering at the chamber instead would either void everything you did not ask for or hand out
 * rare drops at common-drop rates.
 */
public class PredictionItem extends Item {

    private static final String KEY_MOB = "Mob";

    public PredictionItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** The registry id of the mob this prediction is for, if it is bound. */
    public static Optional<String> getSubject(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag.contains(KEY_MOB) ? Optional.of(tag.getString(KEY_MOB)) : Optional.empty();
    }

    public static void setSubject(ItemStack stack, String mobKey) {
        CompoundTag tag = read(stack);
        tag.putString(KEY_MOB, mobKey);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Resolves the bound mob to a live entity type, or empty if that mob is no longer registered. */
    public static Optional<EntityType<?>> resolveSubject(ItemStack stack) {
        return getSubject(stack)
                .map(ResourceLocation::tryParse)
                .flatMap(id -> id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id));
    }

    /** True when two predictions are for the same mob and can be spent together. */
    public static boolean sameSubject(ItemStack a, ItemStack b) {
        return getSubject(a).equals(getSubject(b));
    }

    @Override
    public Component getName(ItemStack stack) {
        return resolveSubject(stack)
                .map(type -> (Component) Component.translatable("item.simulacra.prediction.of",
                        Component.translatable(type.getDescriptionId())))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getSubject(stack).isEmpty()) {
            tooltip.add(Component.translatable("tooltip.simulacra.prediction_unbound").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // The subject is already in the item's name, so the hint goes behind Shift like Create's own.
        studio.akuro.simulacra.content.ModTooltips.addSummary(tooltip, "tooltip.simulacra.prediction_hint");
    }
}
