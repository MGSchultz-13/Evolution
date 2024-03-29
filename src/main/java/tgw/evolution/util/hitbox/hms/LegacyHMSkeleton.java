package tgw.evolution.util.hitbox.hms;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tgw.evolution.util.ArmPose;

public interface LegacyHMSkeleton<T extends Mob & RangedAttackMob> extends HMHumanoid<T> {

    @Override
    default void prepare(T entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.setRightArmPose(ArmPose.EMPTY);
        this.setLeftArmPose(ArmPose.EMPTY);
        ItemStack stack = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.is(Items.BOW) && entity.isAggressive()) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.setRightArmPose(ArmPose.BOW_AND_ARROW);
            }
            else {
                this.setLeftArmPose(ArmPose.BOW_AND_ARROW);
            }
        }
        HMHumanoid.super.prepare(entity, limbSwing, limbSwingAmount, partialTicks);
    }

    @Override
    default void setup(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        HMHumanoid.super.setup(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ItemStack stack = entity.getMainHandItem();
        if (entity.isAggressive() && (stack.isEmpty() || !stack.is(Items.BOW))) {
            float f = Mth.sin(this.attackTime() * Mth.PI);
            float f1 = Mth.sin((1.0F - (1.0F - this.attackTime()) * (1.0F - this.attackTime())) * Mth.PI);
            this.armR().setRotationZ(0);
            this.armL().setRotationZ(0);
            this.armR().setRotationY(-(0.1F - f * 0.6F));
            this.armL().setRotationY(0.1F - f * 0.6F);
            this.armR().setRotationX(Mth.HALF_PI);
            this.armL().setRotationX(Mth.HALF_PI);
            this.armR().addRotationX(-f * 1.2F + f1 * 0.4F);
            this.armL().addRotationX(-f * 1.2F + f1 * 0.4F);
            AnimationUtils.bobArms(this.armR(), this.armL(), ageInTicks);
        }
    }
}
