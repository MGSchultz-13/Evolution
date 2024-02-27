package tgw.evolution.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SimpleAnimatedParticle.class)
public abstract class MixinSimpleAnimatedParticle extends TextureSheetParticle {

    public MixinSimpleAnimatedParticle(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }

    @Override
    @Overwrite
    public int getLightColor(float partialTicks) {
        return 0xFF_00FF;
    }
}
