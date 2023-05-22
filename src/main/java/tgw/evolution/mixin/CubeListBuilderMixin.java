package tgw.evolution.mixin;

import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tgw.evolution.util.collection.RArrayList;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(CubeListBuilder.class)
public abstract class CubeListBuilderMixin {

    @Mutable
    @Shadow
    @Final
    protected List<CubeDefinition> cubes;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;" +
                                                                    "cubes:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    private void onInit(CubeListBuilder instance, List<CubeDefinition> value) {
        this.cubes = new RArrayList<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;"))
    private @Nullable ArrayList onInit() {
        return null;
    }
}
