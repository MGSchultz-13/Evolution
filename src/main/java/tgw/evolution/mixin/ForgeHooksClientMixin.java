package tgw.evolution.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ForgeHooksClient.class)
public abstract class ForgeHooksClientMixin {

    private static final ThreadLocal<PoseStack> STACK = ThreadLocal.withInitial(PoseStack::new);
    @Shadow
    @Final
    private static Matrix4f flipX;
    @Shadow
    @Final
    private static Matrix3f flipXNormal;

    /**
     * @author MGSchultz
     * <p>
     * Avoid allocations.
     */
    @Overwrite
    public static BakedModel handleCameraTransforms(PoseStack matrices,
                                                    BakedModel model,
                                                    ItemTransforms.TransformType cameraTransformType,
                                                    boolean leftHandHackery) {
        PoseStack stack = STACK.get();
        while (!stack.clear()) {
            stack.popPose();
        }
        stack.setIdentity();
        model = model.handlePerspective(cameraTransformType, stack);
        // If the stack is not empty, the code has added a matrix for us to use.
        if (!stack.clear()) {
            // Apply the transformation to the real matrix stack, flipping for left hand
            Matrix4f tMat = stack.last().pose();
            Matrix3f nMat = stack.last().normal();
            if (leftHandHackery) {
                tMat.multiplyBackward(flipX);
                tMat.multiply(flipX);
                nMat.multiplyBackward(flipXNormal);
                nMat.mul(flipXNormal);
            }
            matrices.last().pose().multiply(tMat);
            matrices.last().normal().mul(nMat);
        }
        return model;
    }
}