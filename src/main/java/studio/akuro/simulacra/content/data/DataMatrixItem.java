package studio.akuro.simulacra.content.data;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import studio.akuro.simulacra.SimulacraConfig;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A data model carrier. Held in the offhand, a blank matrix binds to the first mob type defeated, then
 * accumulates "data" from further kills of that mob. Once it has enough data it can be trained (in a
 * Simulation Chamber, by spending compute); a trained matrix can then be simulated for that mob's loot.
 *
 * <p>State lives in the vanilla {@code minecraft:custom_data} component (keys "Mob", "Data", "Trained").
 * The item does not stack, so each matrix is its own model.
 */
public class DataMatrixItem extends Item {
    private static final String KEY_MOB = "Mob";
    private static final String KEY_DATA = "Data";
    private static final String KEY_TRAINED = "Trained";
    /** Part-finished training, carried on the item so pulling a matrix out mid-train costs nothing. */
    private static final String KEY_PROGRESS = "Progress";

    /** Minimum raw data (mob kills) before a matrix can be trained; configurable. */
    public static int dataToTrain() {
        return SimulacraConfig.DATA_TO_TRAIN.get();
    }

    public DataMatrixItem(Properties properties) {
        super(properties);
    }

    private static void update(ItemStack stack, Consumer<CompoundTag> mutator) {
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = existing.copyTag();
        mutator.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static Optional<String> getBoundMob(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag.contains(KEY_MOB) ? Optional.of(tag.getString(KEY_MOB)) : Optional.empty();
    }

    public static int getData(ItemStack stack) {
        return read(stack).getInt(KEY_DATA);
    }

    public static boolean isTrained(ItemStack stack) {
        return read(stack).getBoolean(KEY_TRAINED);
    }

    public static boolean canTrain(ItemStack stack) {
        return getBoundMob(stack).isPresent() && !isTrained(stack) && getData(stack) >= dataToTrain();
    }

    /**
     * Grade of a trained model, 1-4 (Coarse, Tuned, Deep, Self-Aware), derived from total data.
     * 0 while untrained. Trained models keep learning: each completed simulation job adds data.
     */
    public static int getModelTier(ItemStack stack) {
        if (!isTrained(stack)) {
            return 0;
        }
        int data = getData(stack);
        if (data >= SimulacraConfig.MODEL_DATA_TIER4.get()) {
            return 4;
        }
        if (data >= SimulacraConfig.MODEL_DATA_TIER3.get()) {
            return 3;
        }
        if (data >= SimulacraConfig.MODEL_DATA_TIER2.get()) {
            return 2;
        }
        return 1;
    }

    /** Data needed for the next grade, or -1 at Self-Aware. */
    public static int nextTierThreshold(ItemStack stack) {
        return switch (getModelTier(stack)) {
            case 1 -> SimulacraConfig.MODEL_DATA_TIER2.get();
            case 2 -> SimulacraConfig.MODEL_DATA_TIER3.get();
            case 3 -> SimulacraConfig.MODEL_DATA_TIER4.get();
            default -> -1;
        };
    }

    public static Component tierName(int tier) {
        return Component.translatable("tooltip.simulacra.model_tier_" + tier);
    }

    /** Learning-by-doing: called by the Simulation Chamber when a job completes. */
    public static void addSimData(ItemStack stack, int amount) {
        update(stack, tag -> tag.putInt(KEY_DATA, tag.getInt(KEY_DATA) + amount));
    }

    /** Chance a simulation job on this model succeeds; failures print a Corrupted Imprint. */
    public static float accuracy(ItemStack stack) {
        return switch (getModelTier(stack)) {
            // Grade 0 is untrained, and an untrained matrix cannot be simulated at all. It used to
            // fall through to the Coarse figure and advertise itself as 70% accurate, which is a
            // number for a thing it cannot yet do.
            case 0 -> 0f;
            case 2 -> SimulacraConfig.ACCURACY_TUNED.get().floatValue();
            case 3 -> SimulacraConfig.ACCURACY_DEEP.get().floatValue();
            case 4 -> SimulacraConfig.ACCURACY_SELF_AWARE.get().floatValue();
            default -> SimulacraConfig.ACCURACY_COARSE.get().floatValue();
        };
    }

    public static void setTrained(ItemStack stack, boolean trained) {
        update(stack, tag -> tag.putBoolean(KEY_TRAINED, trained));
    }

    /** Compute banked toward training so far. */
    public static float getProgress(ItemStack stack) {
        return read(stack).getFloat(KEY_PROGRESS);
    }

    /**
     * Bank training progress on the matrix itself. Previously this lived on the chamber and was
     * wiped whenever a matrix was inserted or removed, so pulling one out at 99% silently binned
     * the compute; nothing in the UI warned about it.
     */
    public static void setProgress(ItemStack stack, float progress) {
        update(stack, tag -> {
            if (progress > 0f) {
                tag.putFloat(KEY_PROGRESS, progress);
            } else {
                tag.remove(KEY_PROGRESS);
            }
        });
    }

    private static void bindOrIncrement(ItemStack stack, String mobKey, int data) {
        update(stack, tag -> {
            tag.putString(KEY_MOB, mobKey);
            tag.putInt(KEY_DATA, data);
        });
    }

    /**
     * Records a kill of {@code type} worth {@code data} into the matrix held in the player's offhand: a
     * blank matrix binds to that mob, a matrix already bound to it gains the data. Only the offhand slot
     * is considered, so carried matrices do not bind to whatever the player happens to kill. Server-side.
     */
    public static void recordKill(Player player, ResourceLocation type, int data) {
        ItemStack matrix = player.getOffhandItem();
        if (!(matrix.getItem() instanceof DataMatrixItem)) {
            return;
        }
        String key = type.toString();
        Optional<String> bound = getBoundMob(matrix);
        if (bound.isEmpty()) {
            bindOrIncrement(matrix, key, data);
            // The first kill is the one that permanently commits the matrix to a species. It happened
            // silently before, which is how people ended up bound to the wrong mob without noticing.
            AllSoundEvents.CONFIRM_2.playOnServer(player.level(), player.blockPosition(), 0.7f, 1.4f);
        } else if (bound.get().equals(key)) {
            // Trained models keep learning from kills too, so hands-on hunting accelerates grades.
            bindOrIncrement(matrix, key, getData(matrix) + data);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<String> bound = getBoundMob(stack);
        if (bound.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.simulacra.model_blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.simulacra.model_bound", mobName(bound.get())).withStyle(ChatFormatting.AQUA));
        if (isTrained(stack)) {
            int tier = getModelTier(stack);
            tooltip.add(Component.translatable("tooltip.simulacra.model_grade", tierName(tier)).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.simulacra.model_accuracy",
                    String.format("%.0f", accuracy(stack) * 100f)).withStyle(ChatFormatting.GRAY));
            int next = nextTierThreshold(stack);
            if (next > 0) {
                tooltip.add(Component.translatable("tooltip.simulacra.model_data", getData(stack), next).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.simulacra.model_data", getData(stack), dataToTrain()).withStyle(ChatFormatting.GRAY));
        }
    }

    public static Component mobName(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) {
            return Component.literal(key);
        }
        return Component.translatable("entity." + id.getNamespace() + "." + id.getPath());
    }
}
