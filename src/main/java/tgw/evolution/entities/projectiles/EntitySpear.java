package tgw.evolution.entities.projectiles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.capabilities.modular.IModularTool;
import tgw.evolution.events.ItemEvents;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionEntities;
import tgw.evolution.init.EvolutionSounds;
import tgw.evolution.items.IProjectile;
import tgw.evolution.items.modular.ItemModular;
import tgw.evolution.patches.IBlockPatch;
import tgw.evolution.patches.IEntityPatch;
import tgw.evolution.util.damage.DamageSourceEv;
import tgw.evolution.util.hitbox.IHitboxArmed;
import tgw.evolution.util.hitbox.hitboxes.HitboxEntity;
import tgw.evolution.util.math.Vec3d;
import tgw.evolution.util.physics.SI;

public class EntitySpear extends EntityGenericProjectile<EntitySpear> implements IAerodynamicEntity {

    @Nullable
    private IProjectile cachedProjectile;
    private boolean isStone;
    private ItemStack stack = ItemStack.EMPTY;

    public EntitySpear(Level level, LivingEntity thrower, ItemStack thrownStack) {
        super(EvolutionEntities.SPEAR.get(), thrower, level, IModularTool.get(thrownStack).getMass());
        this.setStack(thrownStack.copy());
        this.setDamage((float) (thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) *
                                IModularTool.get(thrownStack).getDmgMultiplier(this.cachedProjectile.projectileDamageType())));
    }

    public EntitySpear(EntityType<EntitySpear> type, Level level) {
        super(type, level);
    }

    public EntitySpear(@SuppressWarnings("unused") PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(EvolutionEntities.SPEAR.get(), level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", this.stack.serializeNBT());
    }

    @Override
    protected void adjustPos(LivingEntity shooter, HumanoidArm arm) {
        HitboxEntity<LivingEntity> hitboxes = (HitboxEntity<LivingEntity>) ((IEntityPatch) shooter).getHitboxes();
        if (hitboxes instanceof IHitboxArmed armed) {
            Vec3d offset = armed.getOffsetForArm(shooter, 1.0f, arm);
            this.setPos(shooter.getX() + offset.x(), shooter.getY() + offset.y(), shooter.getZ() + offset.z());
            return;
        }
        super.adjustPos(shooter, arm);
    }

    @Override
    protected DamageSourceEv createDamageSource() {
        LivingEntity shooter = this.getShooter();
        return EvolutionDamage.causeSpearDamage(this, shooter == null ? this : shooter);
    }

    @Override
    protected boolean damagesEntities() {
        return true;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getArrowStack() {
        return this.stack.copy();
    }

    @Override
    public float getFrictionModifier() {
        return 0;
    }

    @Override
    protected SoundEvent getHitBlockSound() {
        return this.isStone ? EvolutionSounds.STONE_WEAPON_HIT_BLOCK.get() : EvolutionSounds.METAL_WEAPON_HIT_BLOCK.get();
    }

    @Override
    public @Nullable HitboxEntity<EntitySpear> getHitboxes() {
        return null;
    }

    @Override
    public double getLegSlowdown() {
        return 0;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected @Nullable IProjectile getProjectile() {
        return this.cachedProjectile;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public double getVolume() {
        //TODO
        return 50 * SI.CUBIC_CENTIMETER;
    }

    @Override
    protected void modifyMovementOnCollision() {
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.x * -0.1, velocity.y * -0.1, velocity.z * -0.1);
    }

    @Override
    protected void onBlockHit(BlockState state) {
        LivingEntity shooter = this.getShooter();
        if (shooter != null) {
            ItemEvents.damageItem(this.stack, shooter, ItemModular.DamageCause.HIT_BLOCK, null,
                                  ((IBlockPatch) state.getBlock()).getHarvestLevel(state, this.level, null));
        }
    }

    @Override
    protected void playHitEntitySound() {
        this.playSound(this.isStone ? EvolutionSounds.STONE_SPEAR_HIT_ENTITY.get() : EvolutionSounds.METAL_SPEAR_HIT_ENTITY.get(), 1.0F, 1.0F);
    }

    @Override
    protected void postHitLogic(boolean attackSuccessful) {
        LivingEntity shooter = this.getShooter();
        if (attackSuccessful && shooter != null) {
            ItemEvents.damageItem(this.stack, shooter, ItemModular.DamageCause.HIT_ENTITY, null);
        }
        if (this.stack.isEmpty()) {
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.discard();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear", Tag.TAG_COMPOUND)) {
            this.setStack(ItemStack.of(tag.getCompound("Spear")));
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        CompoundTag tag = buffer.readNbt();
        if (tag != null) {
            this.setStack(ItemStack.of(tag));
        }
    }

    private void setStack(ItemStack stack) {
        this.stack = stack.copy();
        this.cachedProjectile = (IProjectile) stack.getItem();
        this.isStone = IModularTool.get(this.stack).getHead().getMaterialInstance().getMaterial().isStone();
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.stack.isEmpty() && this.timeInGround > 10) {
            this.discard();
        }
        if (this.inGround) {
            this.setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    protected void tryDespawn() {
        if (this.pickupStatus != PickupStatus.ALLOWED) {
            super.tryDespawn();
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeNbt(this.stack.serializeNBT());
    }
}
