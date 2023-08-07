package tgw.evolution.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tgw.evolution.client.renderer.StringSink;
import tgw.evolution.patches.PatchFont;

import java.util.List;

@Mixin(Font.class)
public abstract class MixinFont implements PatchFont {

    @Shadow @Final private static Vector3f SHADOW_OFFSET;
    @Mutable @Shadow @Final public int lineHeight;
    @Shadow @Final private StringSplitter splitter;

    @Shadow
    private static int adjustColor(int pColor) {
        throw new AbstractMethodError();
    }

    @Shadow
    public abstract String bidirectionalShaping(String pText);

    @Shadow
    protected abstract int drawInternal(FormattedCharSequence pReorderingProcessor,
                                        float pX,
                                        float pY,
                                        int pColor,
                                        Matrix4f pMatrix,
                                        boolean pDrawShadow);

    /**
     * @author TheGreatWolf
     * @reason Avoid unnecessary allocations
     */
    @Overwrite
    private int drawInternal(String text,
                             float x,
                             float y,
                             int color,
                             boolean dropShadow,
                             Matrix4f matrix,
                             MultiBufferSource buffer,
                             boolean transparent,
                             int colorBackground,
                             int packedLight,
                             boolean bidiFlag) {
        if (bidiFlag) {
            text = this.bidirectionalShaping(text);
        }
        color = adjustColor(color);
        Matrix4f matForText;
        if (dropShadow) {
            matForText = matrix.copy();
            this.renderText(text, x, y, color, true, matrix, buffer, transparent, colorBackground, packedLight);
            matForText.translate(SHADOW_OFFSET);
        }
        else {
            matForText = matrix;
        }
        x = this.renderText(text, x, y, color, false, matForText, buffer, transparent, colorBackground, packedLight);
        return (int) x + (dropShadow ? 1 : 0);
    }

    /**
     * @author TheGreatWolf
     * @reason Avoid unnecessary allocations
     */
    @Overwrite
    private int drawInternal(FormattedCharSequence processor,
                             float x,
                             float y,
                             int color,
                             boolean drawShadow,
                             Matrix4f matrix,
                             MultiBufferSource buffer,
                             boolean transparent,
                             int colorBackground,
                             int packedLight) {
        color = adjustColor(color);
        Matrix4f matForText;
        if (drawShadow) {
            matForText = matrix.copy();
            this.renderText(processor, x, y, color, true, matrix, buffer, transparent, colorBackground, packedLight);
            matForText.translate(SHADOW_OFFSET);
        }
        else {
            matForText = matrix;
        }
        x = this.renderText(processor, x, y, color, false, matForText, buffer, transparent, colorBackground, packedLight);
        return (int) x + (drawShadow ? 1 : 0);
    }

    /**
     * @author TheGreatWolf
     * @reason Make the lineHeight actually matter
     */
    @Overwrite
    public void drawWordWrap(FormattedText formattedText, int i, int j, int k, int l) {
        Matrix4f matrix4f = Transformation.identity().getMatrix();
        for (FormattedCharSequence formattedCharSequence : this.split(formattedText, k)) {
            this.drawInternal(formattedCharSequence, i, j, l, matrix4f, false);
            j += this.lineHeight;
        }
    }

    @Override
    public void drawWordWrap(PoseStack matrices, FormattedText text, float x, float y, int maxWidth, int color, boolean shadow) {
        Matrix4f mat = matrices.last().pose();
        for (FormattedCharSequence charSequence : this.split(text, maxWidth)) {
            this.drawInternal(charSequence, x, y, color, mat, shadow);
            y += this.lineHeight;
        }
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Font;lineHeight:I", opcode = Opcodes.PUTFIELD))
    private void onInit(Font instance, int value) {
        this.lineHeight = 10;
    }

    /**
     * @author TheGreatWolf
     * @reason Avoid allocations
     */
    @Overwrite
    private float renderText(String text,
                             float x,
                             float y,
                             int color,
                             boolean shadow,
                             Matrix4f matrix,
                             MultiBufferSource buffer,
                             boolean transparent,
                             int bgColor,
                             int packedLight) {
        StringSink sink = StringSink.INSTANCE;
        sink.set((Font) (Object) this, buffer, x, y, color, shadow, matrix, transparent, packedLight);
        StringDecomposer.iterateFormatted(text, Style.EMPTY, sink);
        return sink.finish(bgColor, x);
    }

    @Shadow
    protected abstract float renderText(FormattedCharSequence p_92927_,
                                        float p_92928_,
                                        float p_92929_,
                                        int p_92930_,
                                        boolean p_92931_,
                                        Matrix4f p_92932_,
                                        MultiBufferSource p_92933_,
                                        boolean p_92934_,
                                        int p_92935_,
                                        int p_92936_);

    @Shadow
    public abstract List<FormattedCharSequence> split(FormattedText pText, int pMaxWidth);

    /**
     * @author TheGreatWolf
     * @reason Make the lineHeight actually matter
     */
    @Overwrite
    public int wordWrapHeight(String string, int i) {
        return this.lineHeight * this.splitter.splitLines(string, i, Style.EMPTY).size();
    }
}