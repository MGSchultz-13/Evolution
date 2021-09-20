package tgw.evolution.items;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;
import tgw.evolution.Evolution;
import tgw.evolution.entities.projectiles.EntityGenericProjectile;
import tgw.evolution.entities.projectiles.EntitySpear;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionSounds;

import javax.annotation.Nonnull;
import java.util.Set;

public class ItemJavelin extends ItemGenericTool implements IThrowable, ISpear, IBackWeapon {

    private static final Set<Block> EFFECTIVE_ON = Sets.newHashSet();
    private static final Set<Material> EFFECTIVE_MATS = Sets.newHashSet();
    private final float damage;
    private final double mass;
    private final ResourceLocation modelTexture;

    public ItemJavelin(float attackSpeed, IItemTier tier, Properties builder, float damage, double mass, String name) {
        super(attackSpeed, tier, EFFECTIVE_ON, EFFECTIVE_MATS, builder, ToolTypeEv.SPEAR);
        this.damage = damage;
        this.mass = mass;
        this.modelTexture = Evolution.getResource("textures/entity/javelin/javelin_" + name + ".png");
    }

    @Override
    public float baseDamage() {
        return 0;
    }

    @Override
    public int blockDurabilityDamage() {
        return 2;
    }

    @Override
    public int entityDurabilityDamage() {
        return 1;
    }

    @Nonnull
    @Override
    public EvolutionDamage.Type getDamageType() {
        return EvolutionDamage.Type.PIERCING;
    }

    @Override
    public float getEfficiency() {
        return 0;
    }

    @Override
    public double getMass() {
        return this.mass;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public ResourceLocation getTexture() {
        return this.modelTexture;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public float reach() {
        return 5.0f;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            int i = this.getUseDuration(stack) - timeLeft;
            if (i >= 10) {
                if (!world.isClientSide) {
                    EntitySpear spear = new EntitySpear(world, player, stack, this.damage, this.mass);
                    spear.shoot(player, player.xRot, player.yRot, 0.825f, 2.5F);
                    if (player.abilities.instabuild) {
                        spear.pickupStatus = EntityGenericProjectile.PickupStatus.CREATIVE_ONLY;
                    }
                    world.addFreshEntity(spear);
                    world.playSound(null, spear, EvolutionSounds.JAVELIN_THROW.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                    if (!player.abilities.instabuild) {
                        player.inventory.removeItem(stack);
                    }
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                this.addStat(player);
            }
        }
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() || hand == Hand.OFF_HAND) {
            return new ActionResult<>(ActionResultType.FAIL, stack);
        }
        player.startUsingItem(hand);
        return new ActionResult<>(ActionResultType.CONSUME, stack);
    }
}
