package tgw.evolution.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.VibrationSignalParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tgw.evolution.client.renderer.ambient.DynamicLights;

@Mixin(VibrationSignalParticle.class)
public abstract class MixinVibrationSignalParticle extends TextureSheetParticle {

    public MixinVibrationSignalParticle(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }

    /**
     * @reason _
     * @author TheGreatWolf
     */
    @Override
    @Overwrite
    public int getLightColor(float partialTicks) {
        return DynamicLights.FULL_LIGHTMAP_NO_SKY;
    }
}
