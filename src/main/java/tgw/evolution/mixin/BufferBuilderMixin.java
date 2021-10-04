package tgw.evolution.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultColorVertexBuilder;
import com.mojang.blaze3d.vertex.IVertexConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin extends DefaultColorVertexBuilder implements IVertexConsumer {

    @Shadow
    @Nullable
    private VertexFormatElement currentElement;
    @Shadow
    private int elementIndex;
    @Shadow
    private VertexFormat format;
    @Shadow
    private int nextElementByte;

    /**
     * @author JellySquid
     * @reason Remove modulo operations and recursion
     */
    @Override
    @Overwrite
    public void nextElement() {
        ImmutableList<VertexFormatElement> elements = this.format.getElements();
        do {
            this.nextElementByte += this.currentElement.getByteSize();
            // Wrap around the element pointer without using modulo
            if (++this.elementIndex >= elements.size()) {
                this.elementIndex -= elements.size();
            }
            this.currentElement = elements.get(this.elementIndex);
        } while (this.currentElement.getUsage() == VertexFormatElement.Usage.PADDING);
        if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
            IVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }
    }
}
