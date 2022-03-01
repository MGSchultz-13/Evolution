package tgw.evolution.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tgw.evolution.patches.IMinecraftPatch;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin extends GuiComponent {

    @Shadow
    private RecipeBookTabButton selectedTab;

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;" +
                                                                         "updateCollections(Z)V", ordinal = 1))
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        this.selectedTab.playDownSound(Minecraft.getInstance().getSoundManager());
    }

//    @Inject(method = "renderGhostRecipeTooltip", at = @At(value = "TAIL"))
//    private void onRenderGhostRecipeTooltipPost(MatrixStack p_238925_1_,
//                                                int p_238925_2_,
//                                                int p_238925_3_,
//                                                int p_238925_4_,
//                                                int p_238925_5_,
//                                                CallbackInfo ci) {
//        GuiUtils.postItemToolTip();
//    }

//    @Inject(method = "renderGhostRecipeTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;" +
//                                                                                     "renderComponentTooltip" +
//                                                                                     "(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/util/List;II)
//                                                                                     V"), locals = LocalCapture.CAPTURE_FAILHARD)
//    private void onRenderGhostRecipeTooltipPre(MatrixStack matrices,
//                                               int p_238925_2_,
//                                               int p_238925_3_,
//                                               int p_238925_4_,
//                                               int p_238925_5_,
//                                               CallbackInfo ci,
//                                               ItemStack stack) {
//        GuiUtils.preItemToolTip(stack);
//    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (((IMinecraftPatch) Minecraft.getInstance()).isMultiplayerPaused()) {
            ci.cancel();
        }
    }
}
