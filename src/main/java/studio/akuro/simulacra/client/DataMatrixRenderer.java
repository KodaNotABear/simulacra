package studio.akuro.simulacra.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import studio.akuro.simulacra.Simulacra;
import studio.akuro.simulacra.content.data.DataMatrixItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Draws a Data Matrix as its brass frame with the bound mob standing inside it.
 *
 * <p>A matrix's whole identity is its Subject, and a flat icon cannot show it: a chest of matrices
 * bound to six different mobs looks like six copies of the same item.
 *
 * <p>The transform sequence here is Hostile Neural Networks' {@code DataModelItemStackRenderer},
 * followed deliberately closely rather than re-derived. Two things in it are worth calling out
 * because they are not obvious and are what earlier attempts here got wrong:
 *
 * <ul>
 *   <li>Both the frame and the mob are drawn into a <em>private</em> buffer source that is flushed
 *       immediately, never into the shared buffer handed to us. Queueing an entity into the GUI's
 *       batch leaves it to be painted whenever that batch happens to flush, which is somewhere else
 *       entirely on screen.
 *   <li>The mob is anchored at the bottom of the item and scaled from there, not centred. That is
 *       what makes a chicken and an iron golem both look deliberate in a 16-pixel icon.
 * </ul>
 */
public class DataMatrixRenderer extends BlockEntityWithoutLevelRenderer {

    /** The frame drawn behind the mob. Registered for baking in {@link SimulacraClient}. */
    public static final ModelResourceLocation FRAME = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, "item/data_matrix_frame"), "standalone");

    /** How much of an inventory slot the finished icon is allowed to occupy. */
    private static final float ICON_FIT = 0.82f;

    /**
     * A buffer source of our own, drained the moment anything is drawn into it.
     *
     * <p>This is the single most important detail borrowed from Hostile Neural Networks. The buffer
     * passed to a BEWLR in a GUI is the screen's shared batch, and geometry put there is drawn when
     * that batch flushes rather than now — which for an entity means it lands over unrelated parts of
     * the screen. Drawing into a private immediate buffer sidesteps the whole ordering question.
     */
    private static final MultiBufferSource.BufferSource GHOST_BUFFER =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    /**
     * One live entity per species, reused across frames.
     *
     * <p>Entity construction is not cheap and this runs for every matrix on screen every frame, so
     * building one per frame would be visible in the frame time of a full inventory. Keyed by type,
     * not by stack: two matrices bound to zombies can share one zombie.
     */
    private static final Map<EntityType<?>, Entity> CACHE = new HashMap<>();

    /** Species whose renderer threw once and will not be tried again this session. */
    private static final Set<EntityType<?>> REFUSED = new java.util.HashSet<>();

    public DataMatrixRenderer(BlockEntityRenderDispatcher blockEntities, EntityModelSet models) {
        super(blockEntities, models);
    }

    /** Dropped by a resource reload, since entity renderers and models are rebuilt underneath us. */
    public static void clearCache() {
        CACHE.clear();
        // A reload rebuilds every entity renderer, so a species that failed against the old ones
        // deserves another chance against the new.
        REFUSED.clear();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // Shrink the whole composition about the item's centre rather than the mob alone. HNN's
            // numbers fill their icon edge to edge, which is right for their sprite and leaves ours
            // touching the sides of an inventory slot; scaling platform and subject together keeps
            // their proportions and only gives back the margin.
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.scale(ICON_FIT, ICON_FIT, ICON_FIT);
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
        renderFrame(stack, context, pose, light, overlay);

        Entity subject = subjectOf(stack);
        if (subject == null) {
            pose.popPose();
            return;
        }
        try {
            renderSubject(subject, context, pose, light);
        } catch (Throwable failure) {
            // A matrix can be bound to any mob in the pack, including ones whose renderer assumes a
            // real entity in a real world. One misbehaving mob must not take down the inventory
            // screen, so drop that species permanently and carry on with the frame alone.
            EntityType<?> type = subject.getType();
            REFUSED.add(type);
            CACHE.remove(type);
            Simulacra.LOGGER.warn("Data Matrix cannot render {}; showing the frame only",
                    BuiltInRegistries.ENTITY_TYPE.getKey(type), failure);
        }
        pose.popPose();
    }

    /**
     * Lays the frame down as the platform the subject stands on.
     *
     * <p>This is the part that makes the item read as a specimen on a plinth rather than a mob glued
     * to a card. The frame sprite is a flat square tile; tipping it back 75 degrees and spinning it 45
     * turns it into a diamond platform seen from above, and the mob is then placed standing on it.
     *
     * <p>Every number here is Hostile Neural Networks', and they only work because the item model
     * carries no display transforms — the item is positioned by hand for each context instead. Drawn
     * with {@code renderModelLists} rather than {@code ItemRenderer.render} for the same reason: that
     * method would apply a display transform and a recentre of its own on top of these.
     */
    // ItemBlockRenderTypes.getRenderType is deprecated in favour of asking the baked model, but it is
    // what Hostile Neural Networks calls here and it is correct for a single-pass generated sprite.
    @SuppressWarnings("deprecation")
    private static void renderFrame(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                    int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel frame = mc.getModelManager().getModel(FRAME);

        pose.pushPose();
        if (context == ItemDisplayContext.FIXED) {
            // Framed on a wall: the platform lies in the wall plane rather than on the ground.
            pose.translate(1f, 1f, 0f);
            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(-1.5f, -0.5f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.translate(0f, 0f, -1f);
        } else if (context != ItemDisplayContext.GUI) {
            // Held, dropped or worn: flat, with the subject floating over it.
            pose.translate(1f, 1f, 0f);
            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(-1.5f, -0.5f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
        } else {
            pose.translate(0f, -0.5f, -0.5f);
            pose.mulPose(Axis.XN.rotationDegrees(75f));
            pose.mulPose(Axis.ZP.rotationDegrees(45f));
            pose.scale(0.9f, 0.9f, 0.9f);
            pose.translate(0.775, 0.0, -0.0825);
        }

        mc.getItemRenderer().renderModelLists(frame, stack, light, overlay, pose,
                ItemRenderer.getFoilBufferDirect(GHOST_BUFFER,
                        ItemBlockRenderTypes.getRenderType(stack, true), true, false));
        GHOST_BUFFER.endBatch();
        pose.popPose();
    }

    /** The cached entity for this matrix's subject, or null when unbound or the mob's mod is gone. */
    private static Entity subjectOf(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Optional<String> bound = DataMatrixItem.getBoundMob(stack);
        if (bound.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(bound.get());
        if (id == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null || REFUSED.contains(type)) {
            return null;
        }
        Entity cached = CACHE.get(type);
        if (cached == null || cached.level() != mc.level) {
            cached = type.create(mc.level);
            if (cached == null) {
                return null;
            }
            CACHE.put(type, cached);
        }
        // Keep animations running in step with the world rather than frozen at tick zero.
        cached.tickCount = mc.player == null ? 0 : mc.player.tickCount;
        return cached;
    }

    // RenderSystem.runAsFancy is deprecated but is what both vanilla's in-inventory entity render and
    // Hostile Neural Networks' call, and it is what keeps translucent mob parts right under fabulous.
    @SuppressWarnings("deprecation")
    private void renderSubject(Entity subject, ItemDisplayContext context, PoseStack pose, int light) {
        Minecraft mc = Minecraft.getInstance();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float scale = fitScale(subject.getType());

        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);

        if (context == ItemDisplayContext.FIXED) {
            // Item frames hang the item flat against the wall, so the mob is laid into that plane.
            pose.translate(0f, -0.5f, 0f);
            scale *= 0.4f;
            pose.scale(scale, scale, scale);
            pose.translate(0f, 1.45f, 0f);
            pose.mulPose(Axis.XN.rotationDegrees(90f));
            pose.mulPose(Axis.YN.rotationDegrees(180f));
        } else if (context == ItemDisplayContext.GUI) {
            // Drop to the bottom of the item first, then grow upward from there.
            pose.translate(0f, -0.5f, 0f);
            scale *= 0.4f;
            pose.scale(scale, scale, scale);
            pose.translate(0f, 0.45f, 0f);
        } else {
            // Held and dropped copies float, and bob, so the matrix reads as containing something
            // alive rather than printed.
            scale *= 0.25f;
            pose.scale(scale, scale, scale);
            pose.translate(0f, 0.12f + 0.05f * (float) Math.sin((subject.tickCount + partial) / 12.0), 0f);
        }

        // Turned slightly off-axis so the subject has depth; mirrored for the left hand, and spun to
        // face out of the wall when framed.
        float yaw = -30f;
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            yaw = 30f;
        }
        if (context == ItemDisplayContext.FIXED) {
            yaw = 180f;
        }
        pose.mulPose(Axis.YP.rotationDegrees(yaw));

        // Without this the head and body disagree and the mob renders with its neck wrenched round.
        subject.setYRot(0f);
        if (subject instanceof LivingEntity living) {
            living.yBodyRot = living.yBodyRotO = subject.getYRot();
            living.yHeadRot = living.yHeadRotO = subject.getYRot();
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        // Shadows are for entities standing on ground; one cast inside an item icon looks like a bug.
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(subject, 0.0, 0.0, 0.0, 0.0f, partial,
                pose, GHOST_BUFFER, LightTexture.FULL_BRIGHT));
        GHOST_BUFFER.endBatch();
        dispatcher.setRenderShadow(true);

        pose.popPose();
    }

    /**
     * How much to resize a species before the per-context scaling above.
     *
     * <p>Hostile Neural Networks ships an explicit scale per data model and leaves it at 1 for almost
     * everything — a chicken and an iron golem both look right untouched, and only genuine outliers
     * like a ghast or the dragon carry an override. We cannot ship a table, because a matrix binds to
     * whatever mob a pack happens to contain, so this reproduces the default and shrinks only what
     * would not otherwise fit. Nothing is ever enlarged: that is where their table disagrees with a
     * formula, and small mobs looking small is the better failure.
     */
    private static float fitScale(EntityType<?> type) {
        float largest = Math.max(type.getWidth(), type.getHeight());
        return largest <= 2f ? 1f : 2f / largest;
    }
}
