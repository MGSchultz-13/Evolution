package tgw.evolution.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

public enum ArmPose {
    EMPTY,
    ITEM,
    BLOCK,
    BOW_AND_ARROW,
    THROW_SPEAR,
    CROSSBOW_CHARGE,
    CROSSBOW_HOLD,
    SPYGLASS;

    public static ArmPose getArmPose(LivingEntity living, InteractionHand hand) {
        ItemStack stack = living.getItemInHand(hand);
        if (stack.isEmpty()) {
            return EMPTY;
        }
        if (living.getUsedItemHand() == hand && living.getUseItemRemainingTicks() > 0) {
            UseAnim useAnim = stack.getUseAnimation();
            switch (useAnim) {
                case BLOCK -> {
                    return BLOCK;
                }
                case BOW -> {
                    return BOW_AND_ARROW;
                }
                case SPEAR -> {
                    return THROW_SPEAR;
                }
                case SPYGLASS -> {
                    return SPYGLASS;
                }
                case CROSSBOW -> {
                    if (hand == living.getUsedItemHand()) {
                        return CROSSBOW_CHARGE;
                    }
                }
            }
        }
        else if (!living.swinging && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return CROSSBOW_HOLD;
        }
        return ITEM;
    }

    public boolean isTwoHanded() {
        return switch (this) {
            case CROSSBOW_CHARGE, BOW_AND_ARROW, CROSSBOW_HOLD -> true;
            default -> false;
        };
    }
}
