package studio.akuro.simulacra.content.fabricator;

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
 * Draws the Fabricator's input shaft. The block model is only the casing: like the Neural Node, the
 * turning half-shaft on the back is a block-entity render, so without this the machine looks like it
 * takes no rotation at all.
 *
 * <p>It used to also drive a borrowed Mechanical Press head on each strike. That is gone: the head was
 * a press's geometry bolted onto a block that is not shaped like a press, and it read as an object
 * floating in the machine rather than part of it. The lit front and the stamp sound already say the
 * machine is working.
 */
public class LootFabricatorRenderer extends KineticBlockEntityRenderer<LootFabricatorBlockEntity> {

    public LootFabricatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LootFabricatorBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        Direction shaftSide = be.getBlockState()
                .getValue(LootFabricatorBlock.HORIZONTAL_FACING)
                .getOpposite();

        int shaftLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(shaftSide));
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer shaftHalf =
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), shaftSide);
        standardKineticRotationTransform(shaftHalf, be, shaftLight).renderInto(ms, vc);

    }
}
