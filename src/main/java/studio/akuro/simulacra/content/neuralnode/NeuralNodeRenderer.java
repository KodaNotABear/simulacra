package studio.akuro.simulacra.content.neuralnode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Renders the Neural Node's input shaft as a rotating half-shaft on its back face, the Create
 * convention. The casing is the static block model; this only adds the moving shaft.
 *
 * <p>Unlike Create's own renderers this does not bail out when Flywheel visualization is active,
 * because there is no Flywheel visual to fall back on.
 */
public class NeuralNodeRenderer extends KineticBlockEntityRenderer<NeuralNodeBlockEntity> {

    public NeuralNodeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(NeuralNodeBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        Direction shaftSide = be.getBlockState()
                .getValue(NeuralNodeBlock.HORIZONTAL_FACING)
                .getOpposite();

        int shaftLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(shaftSide));
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer shaftHalf =
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), shaftSide);
        standardKineticRotationTransform(shaftHalf, be, shaftLight).renderInto(ms, vc);
    }
}
