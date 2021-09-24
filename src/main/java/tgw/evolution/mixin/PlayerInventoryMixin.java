package tgw.evolution.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tgw.evolution.patches.IMinecraftPatch;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @OnlyIn(Dist.CLIENT)
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void onSwapPaint(double scrollAmount, CallbackInfo ci) {
        if (((IMinecraftPatch) Minecraft.getInstance()).isMultiplayerPaused()) {
            ci.cancel();
        }
    }
}
