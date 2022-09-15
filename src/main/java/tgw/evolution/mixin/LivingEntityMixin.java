package tgw.evolution.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tgw.evolution.blocks.BlockUtils;
import tgw.evolution.blocks.IClimbable;
import tgw.evolution.blocks.ICollisionBlock;
import tgw.evolution.blocks.IFallSufixBlock;
import tgw.evolution.capabilities.modular.IModularTool;
import tgw.evolution.entities.EffectHelper;
import tgw.evolution.events.EntityEvents;
import tgw.evolution.hooks.LivingEntityHooks;
import tgw.evolution.init.EvolutionAttributes;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.init.EvolutionSounds;
import tgw.evolution.inventory.AdditionalSlotType;
import tgw.evolution.items.IEvolutionItem;
import tgw.evolution.items.IMelee;
import tgw.evolution.network.PacketCSCollision;
import tgw.evolution.network.PacketCSImpactDamage;
import tgw.evolution.patches.*;
import tgw.evolution.util.constants.EntityStates;
import tgw.evolution.util.damage.DamageSourceEntity;
import tgw.evolution.util.damage.EvolutionCombatTracker;
import tgw.evolution.util.earth.Gravity;
import tgw.evolution.util.math.MathHelper;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ILivingEntityPatch, IEntityPatch<LivingEntity> {

    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
    @Shadow
    @Final
    private static EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID;
    private final EffectHelper effectHelper = new EffectHelper();
    @Shadow
    public float animationSpeed;
    @Shadow
    public float attackAnim;
    @Shadow
    public float flyingSpeed;
    @Shadow
    public float hurtDir;
    @Shadow
    public int hurtDuration;
    @Shadow
    public int hurtTime;
    @Shadow
    public int removeArrowTime;
    @Shadow
    public int removeStingerTime;
    @Shadow
    public int swingTime;
    @Shadow
    public boolean swinging;
    @Shadow
    public float yBodyRot;
    @Shadow
    public float yBodyRotO;
    @Shadow
    public float yHeadRot;
    @Shadow
    public float yHeadRotO;
    @Shadow
    protected float animStep;
    @Shadow
    protected int fallFlyTicks;
    @Shadow
    protected boolean jumping;
    @Shadow
    protected float lastHurt;
    @Shadow
    @Nullable
    protected Player lastHurtByPlayer;
    @Shadow
    protected int lastHurtByPlayerTime;
    @Shadow
    protected int noActionTime;
    @Shadow
    protected float oRun;
    @Shadow
    protected float run;
    @Mutable
    @Shadow
    @Final
    private Map<MobEffect, MobEffectInstance> activeEffects;
    @Mutable
    @Final
    @Shadow
    private CombatTracker combatTracker;
    @Shadow
    private boolean effectsDirty;
    private boolean isSpecialAttacking;
    @Shadow
    private DamageSource lastDamageSource;
    @Shadow
    private long lastDamageStamp;
    @Shadow
    private int noJumpDelay;
    private byte specialAttackCooldown;
    private byte specialAttackStopTicks;
    private byte specialAttackTime;
    private @Nullable IMelee.IAttackType specialAttackType;
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    protected abstract void actuallyHurt(DamageSource p_21240_, float p_21241_);

    @Override
    public void addAbsorptionSuggestion(float amount) {
        if (amount > 0) {
            float delta = this.effectHelper.addAbsorptionSuggestion(amount);
            if (delta > 0) {
                this.setAbsorptionAmount(this.getAbsorptionAmount() + delta);
            }
        }
    }

    /**
     * @author TheGreatWolf
     * @reason When an effect is overwritten to a hidden effect, its attribute modifiers are removed and readded. However, instead of removing the
     * modifiers based on the old instance of the effect (which has a lower amplifier), the original code removes and readds the attributes based
     * on the new instance (which has a higher amplifier), resulting in weird behaviours.
     */
    @Overwrite
    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
        if (!this.canBeAffected(effectInstance)) {
            return false;
        }
        MobEffectInstance oldInstance = this.activeEffects.get(effectInstance.getEffect());
        MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionAddedEvent((LivingEntity) (Object) this, oldInstance, effectInstance, entity));
        if (oldInstance == null) {
            this.activeEffects.put(effectInstance.getEffect(), effectInstance);
            this.onEffectAdded(effectInstance, entity);
            return true;
        }
        if (((IMobEffectInstancePatch) oldInstance).updateWithEntity(effectInstance, (LivingEntity) (Object) this)) {
            this.onEffectUpdated(oldInstance, false, entity);
            return true;
        }
        return false;
    }

    @Shadow
    public abstract void aiStep();

    @Shadow
    protected abstract void blockUsingShield(LivingEntity p_190629_1_);

    /**
     * @author TheGreatWolf
     * @reason Overwrite to reset animation position.
     */
    @Overwrite
    public void calculateEntityAnimation(LivingEntity entity, boolean flies) {
        entity.animationSpeedOld = entity.animationSpeed;
        double dx = entity.getX() - entity.xo;
        double dy = flies ? entity.getY() - entity.yo : 0.0D;
        double dz = entity.getZ() - entity.zo;
        float dSSq = MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 4.0F;
        if (dSSq > 1.0F) {
            dSSq = 1.0F;
        }
        else if (dSSq <= 1E-20 && entity.animationSpeed <= 1E-2F) {
            entity.animationPosition = 0;
        }
        entity.animationSpeed += (dSSq - entity.animationSpeed) * 0.4F;
        entity.animationPosition += entity.animationSpeed;
    }

    private void calculateWallImpact(double speedX, double speedZ, double mass) {
        double motionXPost = this.getDeltaMovement().x;
        double deltaSpeedX = Math.abs(speedX) - Math.abs(motionXPost);
        deltaSpeedX *= 20;
        float damage = 0;
        if (deltaSpeedX >= 6) {
            double kineticEnergy = 0.5 * deltaSpeedX * deltaSpeedX * mass;
            AABB bb = this.getBoundingBox();
            double xCoord = speedX >= 0 ? bb.maxX + 0.01 : bb.minX - 0.01;
            int numberOfBlocks = 0;
            double slowDown = 0;
            BlockPos minPos = new BlockPos(xCoord, bb.minY, bb.minZ);
            BlockPos maxPos = new BlockPos(xCoord, bb.maxY, bb.maxZ);
            BlockPos.MutableBlockPos changingPos = new BlockPos.MutableBlockPos();
            if (this.level.hasChunksAt(minPos, maxPos)) {
                for (int j = minPos.getY(); j <= maxPos.getY(); j++) {
                    for (int k = minPos.getZ(); k <= maxPos.getZ(); k++) {
                        numberOfBlocks++;
                        changingPos.set(xCoord, j, k);
                        BlockState stateAtPos = this.level.getBlockState(changingPos);
                        Block blockAtPos = stateAtPos.getBlock();
                        if (blockAtPos instanceof ICollisionBlock collisionBlock) {
                            slowDown += collisionBlock.getSlowdownSide(stateAtPos);
                            //noinspection ObjectAllocationInLoop
                            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSCollision(changingPos, speedX, Direction.Axis.X));
                        }
                        else {
                            slowDown += 1;
                        }
                    }
                }
            }
            if (numberOfBlocks > 0) {
                slowDown /= numberOfBlocks;
            }
            if (slowDown > 0) {
                slowDown = 1.0 - slowDown;
            }
            double distanceOfSlowdown = slowDown + this.getBbWidth() / 4;
            double forceOfImpact = kineticEnergy / distanceOfSlowdown;
            float area = this.getBbHeight() * this.getBbWidth();
            double pressure = forceOfImpact / area;
            damage += (float) Math.pow(pressure, 1.6) / 1_750_000;
        }
        double motionZPost = this.getDeltaMovement().z;
        double deltaSpeedZ = Math.abs(speedZ) - Math.abs(motionZPost);
        deltaSpeedZ *= 20;
        if (deltaSpeedZ >= 6) {
            double kineticEnergy = 0.5 * deltaSpeedZ * deltaSpeedZ * mass;
            AABB bb = this.getBoundingBox();
            double zCoord = speedZ >= 0 ? bb.maxZ + 0.01 : bb.minZ - 0.01;
            int numberOfBlocks = 0;
            double slowDown = 0;
            BlockPos minPos = new BlockPos(bb.minX, bb.minY, zCoord);
            BlockPos maxPos = new BlockPos(bb.maxX, bb.maxY, zCoord);
            BlockPos.MutableBlockPos changingPos = new BlockPos.MutableBlockPos();
            if (this.level.hasChunksAt(minPos, maxPos)) {
                for (int i = minPos.getX(); i <= maxPos.getX(); i++) {
                    for (int j = minPos.getY(); j <= maxPos.getY(); j++) {
                        numberOfBlocks++;
                        changingPos.set(i, j, zCoord);
                        BlockState stateAtPos = this.level.getBlockState(changingPos);
                        Block blockAtPos = stateAtPos.getBlock();
                        if (blockAtPos instanceof ICollisionBlock collisionBlock) {
                            slowDown += collisionBlock.getSlowdownSide(stateAtPos);
                            //noinspection ObjectAllocationInLoop
                            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSCollision(changingPos, speedZ, Direction.Axis.Z));
                        }
                        else {
                            slowDown += 1;
                        }
                    }
                }
            }
            if (numberOfBlocks > 0) {
                slowDown /= numberOfBlocks;
            }
            if (slowDown > 0) {
                slowDown = 1.0 - slowDown;
            }
            double distanceOfSlowdown = slowDown + this.getBbWidth() / 4;
            double forceOfImpact = kineticEnergy / distanceOfSlowdown;
            float area = this.getBbHeight() * this.getBbWidth();
            double pressure = forceOfImpact / area;
            damage += (float) Math.pow(pressure, 1.6) / 1_500_000;
        }
        if (damage >= 1.0f) {
            if (!this.level.isClientSide) {
                this.hurt(EvolutionDamage.WALL_IMPACT, damage);
            }
            else //noinspection ConstantConditions
                if ((LivingEntity) (Object) this instanceof Player) {
                    EvolutionNetwork.INSTANCE.sendToServer(new PacketCSImpactDamage(damage));
                }
        }
    }

    @Shadow
    public abstract boolean canBeAffected(MobEffectInstance pPotioneffect);

    @Shadow
    public abstract boolean canStandOnFluid(FluidState p_204042_);

    @Shadow
    protected abstract boolean checkBedExists();

    @Shadow
    protected abstract boolean checkTotemDeathProtection(DamageSource p_21263_);

    @Shadow
    protected abstract void detectEquipmentUpdates();

    @Shadow
    public abstract void die(DamageSource p_21014_);

    private Vec3 getAbsoluteAcceleration(Vec3 direction, double magnitude) {
        double length = direction.lengthSqr();
        if (length < 1.0E-7) {
            return Vec3.ZERO;
        }
        if (this.getPose() == Pose.CROUCHING) {
            //noinspection ConstantConditions
            if (!((Object) this instanceof Player && ((Player) (Object) this).getAbilities().flying)) {
                magnitude *= 0.3;
            }
        }
        //noinspection ConstantConditions
        if ((Object) this instanceof Player) {
            if (this.getPose() == Pose.SWIMMING && !this.isInWater()) {
                magnitude *= 0.3;
            }
        }
        if (this.isUsingItem()) {
            Item activeItem = this.getUseItem().getItem();
            if (activeItem instanceof IEvolutionItem) {
                magnitude *= ((IEvolutionItem) activeItem).useItemSlowDownRate();
            }
        }
        Vec3 acceleration = direction.normalize();
        double accX = acceleration.x * magnitude;
        double accY = acceleration.y * magnitude;
        double accZ = acceleration.z * magnitude;
        float sinFacing = MathHelper.sinDeg(this.getYRot());
        float cosFacing = MathHelper.cosDeg(this.getYRot());
        return new Vec3(accX * cosFacing - accZ * sinFacing, accY, accZ * cosFacing + accX * sinFacing);
    }

    @Shadow
    public abstract float getAbsorptionAmount();

    private double getAcceleration() {
        double force = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double mass = this.getAttributeValue(EvolutionAttributes.MASS.get());
        return force / mass;
    }

    @Shadow
    public abstract int getArrowCount();

    @Shadow
    @Nullable
    public abstract AttributeInstance getAttribute(Attribute p_21052_);

    @Shadow
    public abstract double getAttributeBaseValue(Attribute p_21173_);

    @Shadow
    public abstract double getAttributeValue(Attribute p_21134_);

    @Shadow
    public abstract AttributeMap getAttributes();

    @Override
    public double getBaseMass() {
        return 70;
    }

    @Shadow
    public abstract CombatTracker getCombatTracker();

    @Shadow
    @Nullable
    protected abstract SoundEvent getDeathSound();

    @Shadow
    @Nullable
    public abstract MobEffectInstance getEffect(MobEffect p_21125_);

    @Override
    public EffectHelper getEffectHelper() {
        return this.effectHelper;
    }

    @Override
    public float getFrictionModifier() {
        return 2.0f;
    }

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot p_21127_);

    /**
     * @author TheGreatWolf
     * @reason Replace the method to handle Evolution's physics.
     * Represents the upwards acceleration of the Entity when jumping.
     */
    @Overwrite
    protected float getJumpPower() {
        return 0.25f * this.getBlockJumpFactor();
    }

    @Override
    public double getLegSlowdown() {
        return this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 2.75;
    }

    @Shadow
    public abstract ItemStack getMainHandItem();

    @Shadow
    protected abstract float getSoundVolume();

    @Override
    public float getSpecialAttackProgress(float partialTicks) {
        if (this.specialAttackStopTicks > 0) {
            partialTicks = 1.0f;
        }
        assert this.specialAttackType != null;
        return (this.specialAttackTime + partialTicks) / this.specialAttackType.getAttackTime();
    }

    @Nullable
    @Override
    public IMelee.IAttackType getSpecialAttackType() {
        return this.specialAttackType;
    }

    @Shadow
    public abstract int getStingerCount();

    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract float getVoicePitch();

    @Shadow
    protected abstract float getWaterSlowDown();

    private Vec3 handleLadderMotion(double speedX, double speedY, double speedZ) {
        //noinspection ConstantConditions
        boolean isCreativeFlying = (Object) this instanceof Player && ((Player) (Object) this).getAbilities().flying;
        //noinspection ConstantConditions
        if (this.onClimbable() && !isCreativeFlying) {
            BlockState state = this.getFeetBlockState();
            Block block = state.getBlock();
            double dx = 0;
            double dz = 0;
            if (block instanceof IClimbable climbable) {
                double climbableOffset = climbable.getXPos(state);
                if (!Double.isNaN(climbableOffset)) {
                    if (climbableOffset < 0) {
                        double temp = this.blockPosition().getX() - climbableOffset + this.getBbWidth() / 2.0;
                        if (temp < this.getX()) {
                            dx = (this.getX() - temp) / 20.0;
                        }
                    }
                    else if (climbableOffset > 0) {
                        double temp = this.blockPosition().getX() + 1 - climbableOffset - this.getBbWidth() / 2.0;
                        if (temp > this.getX()) {
                            dx = (this.getX() - temp) / 20.0;
                        }
                    }
                }
                climbableOffset = climbable.getZPos(state);
                if (!Double.isNaN(climbableOffset)) {
                    if (climbableOffset < 0) {
                        double temp = this.blockPosition().getZ() - climbableOffset + this.getBbWidth() / 2.0;
                        if (temp < this.getZ()) {
                            dz = (this.getZ() - temp) / 20.0;
                        }
                    }
                    else if (climbableOffset > 0) {
                        double temp = this.blockPosition().getZ() + 1 - climbableOffset - this.getBbWidth() / 2.0;
                        if (temp > this.getZ()) {
                            dz = (this.getZ() - temp) / 20.0;
                        }
                    }
                }
            }
            this.fallDistance = 1.0F;
            double newX;
            double newZ;
            if (!this.isOnGround()) {
                newX = MathHelper.clamp(speedX, -0.025, 0.025);
                newX *= 0.8;
                newX -= dx;
                newZ = MathHelper.clamp(speedZ, -0.025, 0.025);
                newZ *= 0.8;
                newZ -= dz;
            }
            else {
                newX = speedX;
                newZ = speedZ;
            }
            double newY = speedY < -0.3 ? speedY : Math.max(speedY, this.isCrouching() ? 0 : -0.15);
            //noinspection ConstantConditions
            if (newY < 0 && block != Blocks.SCAFFOLDING && this.isCrouching() && (Object) this instanceof Player) {
                newY = 0;
            }
            return new Vec3(newX, newY, newZ);
        }
        return new Vec3(speedX, speedY, speedZ);
    }

    private void handleNormalMovement(Vec3 travelVector, double gravityAcceleration, float slowdown) {
        AABB aabb = this.getBoundingBox();
        BlockPos.MutableBlockPos blockBelow = new BlockPos.MutableBlockPos(this.getX(), aabb.minY - 0.001, this.getZ());
        BlockState state = this.level.getBlockState(blockBelow);
        if (this.isOnGround() && (state.isAir() || state.getCollisionShape(this.level, blockBelow).isEmpty())) {
            outer:
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    blockBelow.set(i == 0 ? aabb.minX : aabb.maxX, aabb.minY - 0.001, j == 0 ? aabb.minZ : aabb.maxZ);
                    state = this.level.getBlockState(blockBelow);
                    if (!state.isAir() && !state.getCollisionShape(this.level, blockBelow).isEmpty()) {
                        break outer;
                    }
                }
            }
        }
        Block block = state.getBlock();
        float frictionCoef = 0.85F;
        if (state.isAir()) {
            frictionCoef = 0.0f;
        }
        else if (block instanceof IBlockPatch friction) {
            frictionCoef = friction.getFrictionCoefficient(state);
        }
        if (this.getFluidHeight(FluidTags.WATER) > 0) {
            frictionCoef -= 0.1f;
            if (frictionCoef < 0.01F) {
                frictionCoef = 0.01F;
            }
        }
        Vec3 acceleration = this.getAbsoluteAcceleration(travelVector, slowdown * this.jumpMovementFactor(frictionCoef));
        double accX = acceleration.x;
        double accY = acceleration.y;
        double accZ = acceleration.z;
        if (!this.isOnGround()) {
            frictionCoef = 0.0F;
        }
        Vec3 motion = this.getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        if ((this.horizontalCollision || this.jumping) && this.onClimbable()) {
            motionY = BlockUtils.getLadderUpSpeed(this.getFeetBlockState());
        }
        else if (!this.isNoGravity()) {
            if (this.isAffectedByFluids()) {
                accY -= gravityAcceleration;
            }
        }
        if (this.hasCollidedOnXAxis()) {
            accX = Math.signum(accX) * 0.001;
        }
        if (this.hasCollidedOnZAxis()) {
            accZ = Math.signum(accZ) * 0.001;
        }
        double legSlowDownX = 0;
        double legSlowDownZ = 0;
        double frictionAcc = frictionCoef * gravityAcceleration;
        if (this.isOnGround() || !this.isAffectedByFluids()) {
            double legSlowDown = this.getLegSlowdown();
            if (frictionAcc != 0) {
                legSlowDown *= frictionAcc * this.getFrictionModifier();
            }
            else {
                legSlowDown *= gravityAcceleration * 0.85 * this.getFrictionModifier();
            }
            legSlowDownX = motionX * legSlowDown;
            legSlowDownZ = motionZ * legSlowDown;
        }
        double mass = this.getAttributeValue(EvolutionAttributes.MASS.get());
        double horizontalDrag = Gravity.horizontalDrag(this) / mass;
        double verticalDrag = Gravity.verticalDrag(this) / mass;
        double frictionX = 0;
        double frictionZ = 0;
        boolean isActiveWalking = accX != 0 || accZ != 0;
        if (!isActiveWalking) {
            double norm = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (norm != 0) {
                frictionX = motionX / norm * frictionAcc;
                frictionZ = motionZ / norm * frictionAcc;
            }
            if (Math.abs(motionX) < Math.abs(frictionX)) {
                frictionX = motionX;
            }
            if (Math.abs(motionZ) < Math.abs(frictionZ)) {
                frictionZ = motionZ;
            }
        }
        double dragX = Math.signum(motionX) * motionX * motionX * horizontalDrag;
        if (Math.abs(dragX) > Math.abs(motionX)) {
            dragX = motionX;
        }
        double dragY = Math.signum(motionY) * motionY * motionY * verticalDrag;
        if (Math.abs(dragY) > Math.abs(motionY)) {
            dragY = motionY;
        }
        double dragZ = Math.signum(motionZ) * motionZ * motionZ * horizontalDrag;
        if (Math.abs(dragZ) > Math.abs(motionZ)) {
            dragZ = motionZ;
        }
        motionX += accX - legSlowDownX - frictionX - dragX;
        motionY += accY - dragY;
        motionZ += accZ - legSlowDownZ - frictionZ - dragZ;
        if (Math.abs(motionX) < 1e-6) {
            motionX = 0;
        }
        if (Math.abs(motionY) < 1e-6) {
            motionY = 0;
        }
        if (Math.abs(motionZ) < 1e-6) {
            motionZ = 0;
        }
        if (Double.isNaN(motionX)) {
            motionX = 0;
        }
        if (Double.isNaN(motionY)) {
            motionY = 0;
        }
        if (Double.isNaN(motionZ)) {
            motionZ = 0;
        }
        this.setDeltaMovement(this.handleLadderMotion(motionX, motionY, motionZ));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.calculateWallImpact(motionX, motionZ, mass);
        }
    }

    @Shadow
    public abstract boolean hasEffect(MobEffect p_21024_);

    /**
     * @author TheGreatWolf
     * @reason Overwrite to use Evolution Damage Sources.
     */
    @Override
    @Overwrite
    public boolean hurt(DamageSource source, float amount) {
        if (!ForgeHooks.onLivingAttack((LivingEntity) (Object) this, source, amount)) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.level.isClientSide) {
            return false;
        }
        if (this.isDeadOrDying()) {
            return false;
        }
        if (source.isFire() && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        }
        if (this.isSleeping() && !this.level.isClientSide) {
            this.stopSleeping();
        }
        this.noActionTime = 0;
        float f = amount;
        boolean blocked = false;
        float blockedAmount = 0.0F;
        if (amount > 0.0F && this.isDamageSourceBlocked(source)) {
            ShieldBlockEvent event = ForgeHooks.onShieldBlock((LivingEntity) (Object) this, source, amount);
            if (!event.isCanceled()) {
                if (event.shieldTakesDamage()) {
                    this.hurtCurrentlyUsedShield(amount);
                }
                blockedAmount = event.getBlockedDamage();
                amount -= event.getBlockedDamage();
                if (!source.isProjectile()) {
                    Entity entity = source.getDirectEntity();
                    if (entity instanceof LivingEntity) {
                        this.blockUsingShield((LivingEntity) entity);
                    }
                }
                blocked = true;
            }
        }
        this.animationSpeed = 1.5F;
        boolean gotHurt = true;
        if (this.invulnerableTime > 10.0F) {
            if (amount <= this.lastHurt) {
                return false;
            }
            this.actuallyHurt(source, amount - this.lastHurt);
            this.lastHurt = amount;
            gotHurt = false;
        }
        else {
            this.lastHurt = amount;
            this.invulnerableTime = 20;
            this.actuallyHurt(source, amount);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
        }
        if (source.isDamageHelmet() && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.hurtHelmet(source, amount);
            amount *= 0.75f;
        }
        this.hurtDir = 0.0F;
        Entity sourceEntity = source.getEntity();
        if (sourceEntity != null) {
            if (sourceEntity instanceof LivingEntity && !source.isNoAggro()) {
                this.setLastHurtByMob((LivingEntity) sourceEntity);
            }
            if (sourceEntity instanceof Player) {
                this.lastHurtByPlayerTime = 100;
                this.lastHurtByPlayer = (Player) sourceEntity;
            }
            else if (sourceEntity instanceof TamableAnimal tamable) {
                if (tamable.isTame()) {
                    this.lastHurtByPlayerTime = 100;
                    LivingEntity owner = tamable.getOwner();
                    if (owner != null && owner.getType() == EntityType.PLAYER) {
                        this.lastHurtByPlayer = (Player) owner;
                    }
                    else {
                        this.lastHurtByPlayer = null;
                    }
                }
            }
        }
        if (gotHurt) {
            if (blocked) {
                this.level.broadcastEntityEvent(this, EntityStates.SHIELD_BLOCK_SOUND);
            }
            else if (source instanceof EntityDamageSource entitySource && entitySource.isThorns()) {
                this.level.broadcastEntityEvent(this, EntityStates.THORNS_HIT_SOUND);
            }
            else {
                byte state;
                if (source == EvolutionDamage.DROWN) { //Replace with Evolution Damage
                    state = EntityStates.DROWN_HIT_SOUND;
                }
                else if (source.isFire()) {
                    state = EntityStates.FIRE_HIT_SOUND;
                }
                else if (source == DamageSource.SWEET_BERRY_BUSH) {
                    state = EntityStates.SWEET_BERRY_BUSH_HIT_SOUND;
                }
                else {
                    state = EntityStates.GENERIC_HIT_SOUND;
                }
                this.level.broadcastEntityEvent(this, state);
            }
            if (source instanceof DamageSourceEntity && (!blocked || amount > 0.0F)) { //Replace for Evolution Damage
                this.markHurt();
            }
            if (sourceEntity != null) {
                double dx = sourceEntity.getX() - this.getX();
                double dz;
                for (dz = sourceEntity.getZ() - this.getZ(); dx * dx + dz * dz < 1.0E-4; dz = (Math.random() - Math.random()) * 0.01) {
                    dx = (Math.random() - Math.random()) * 0.01;
                }
                this.hurtDir = (float) (MathHelper.atan2Deg(dz, dx) - this.getYRot());
                this.knockback(0.4F, dx, dz);
            }
            else {
                this.hurtDir = (int) (Math.random() * 2.0) * 180;
            }
        }
        if (this.isDeadOrDying()) {
            if (!this.checkTotemDeathProtection(source)) {
                SoundEvent soundevent = this.getDeathSound();
                if (gotHurt && soundevent != null) {
                    this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                }
                this.die(source);
            }
        }
        else if (gotHurt) {
            this.playHurtSound(source);
        }
        boolean didDamage = !blocked || amount > 0.0F;
        if (didDamage) {
            this.lastDamageSource = source;
            this.lastDamageStamp = this.level.getGameTime();
        }
        //noinspection ConstantConditions
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverPlayer, source, f, amount, blocked);
            if (blockedAmount > 0.0F && blockedAmount < 3.402_823_5E37F) {
                serverPlayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(blockedAmount * 10.0F));
            }
        }
        if (sourceEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverPlayer, this, source, f, amount, blocked);
        }
        return didDamage;
    }

    @Shadow
    protected abstract void hurtCurrentlyUsedShield(float p_184590_1_);

    @Shadow
    protected abstract void hurtHelmet(DamageSource p_147213_, float p_147214_);

    @Shadow
    protected abstract boolean isAffectedByFluids();

    @Override
    public boolean isCameraLocked() {
        if (this.shouldRenderSpecialAttack()) {
            assert this.specialAttackType != null;
            return this.specialAttackType.isCameraLocked(this.specialAttackTime);
        }
        return false;
    }

    @Shadow
    public abstract boolean isDamageSourceBlocked(DamageSource p_21276_);

    @Shadow
    public abstract boolean isDeadOrDying();

    @Shadow
    public abstract boolean isEffectiveAi();

    @Shadow
    public abstract boolean isFallFlying();

    @Override
    public boolean isInHitTicks() {
        if (!this.isSpecialAttacking) {
            return false;
        }
        assert this.specialAttackType != null;
        return this.specialAttackType.isHitTick(this.specialAttackTime);
    }

    @Override
    public boolean isInSpecialAttack() {
        if (this.isSpecialAttacking) {
            return true;
        }
        return this.specialAttackCooldown > 0 || this.specialAttackStopTicks > 0;
    }

    @Override
    public boolean isMotionLocked() {
        if (this.shouldRenderSpecialAttack()) {
            assert this.specialAttackType != null;
            return this.specialAttackType.isMotionLocked(this.specialAttackTime);
        }
        return false;
    }

    @Shadow
    public abstract boolean isSleeping();

    @Override
    public boolean isSpecialAttacking() {
        return this.isSpecialAttacking;
    }

    @Shadow
    public abstract boolean isUsingItem();

    /**
     * @author TheGreatWolf
     * @reason Replace the method to handle Evolution's physics.
     * Represents the jump force applied during a single tick for the entity to jump.
     */
    @Overwrite
    protected void jumpFromGround() {
        float upwardsBaseAcc = this.getJumpPower();
        MobEffectInstance effect = this.getEffect(MobEffects.JUMP);
        if (effect != null) {
            upwardsBaseAcc *= 1.0f + (effect.getAmplifier() + 1) / 10.0f;
        }
        double baseMass = this.getAttributeBaseValue(EvolutionAttributes.MASS.get());
        double totalMass = this.getAttributeValue(EvolutionAttributes.MASS.get());
        double upwardsForce = Math.min(baseMass * 1.25, totalMass) * upwardsBaseAcc;
        double upwardsAcc = upwardsForce / totalMass;
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, upwardsAcc, motion.z);
        this.hasImpulse = true;
        this.noJumpDelay = 10;
        ForgeHooks.onLivingJump((LivingEntity) (Object) this);
    }

    private double jumpMovementFactor(float frictionCoef) {
        //noinspection ConstantConditions
        if ((Object) this instanceof Player player) {
            if (!player.getAbilities().flying) {
                if (this.isOnGround() || this.onClimbable()) {
                    return this.getAcceleration() * frictionCoef * this.getFrictionModifier();
                }
                return this.noJumpDelay > 3 ? 0.075 * this.getAcceleration() : 0;
            }
        }
        return this.isOnGround() ? this.getAcceleration() * frictionCoef * this.getFrictionModifier() : this.flyingSpeed;
    }

    @Shadow
    public abstract void knockback(double p_147241_, double p_147242_, double p_147243_);

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.put("EffectHelper", this.effectHelper.save());
    }

    @Inject(method = "baseTick", at = @At(value = "TAIL"))
    public void onBaseTick(CallbackInfo ci) {
        if (this.specialAttackCooldown > 0) {
            this.specialAttackCooldown--;
        }
        if (this.isSpecialAttacking) {
            this.specialAttackTime++;
            assert this.specialAttackType != null;
            int totalTime = this.specialAttackType.getAttackTime();
            if (this.specialAttackTime > totalTime) {
                this.stopSpecialAttack(IMelee.StopReason.END);
            }
        }
        else {
            if (this.specialAttackStopTicks > 0) {
                this.specialAttackStopTicks--;
            }
            else {
                this.specialAttackTime = 0;
            }
        }
    }

    /**
     * @author TheGreatWolf
     * @reason Simplify the method, remove optional overhead (simply check for null if you really need)
     */
    @Overwrite
    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        }
        BlockPos pos = this.blockPosition();
        BlockState state = this.getFeetBlockState();
        return state.isLadder(this.level, pos, (LivingEntity) (Object) this);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void onConstructor(EntityType<? extends LivingEntity> type, Level level, CallbackInfo ci) {
        AttributeInstance massAtr = this.getAttribute(EvolutionAttributes.MASS.get());
        assert massAtr != null;
        massAtr.setBaseValue(this.getBaseMass());
    }

    @Shadow
    protected abstract void onEffectAdded(MobEffectInstance p_147190_, @Nullable Entity p_147191_);

    @Shadow
    protected abstract void onEffectRemoved(MobEffectInstance pEffect);

    @Shadow
    public abstract void onEffectUpdated(MobEffectInstance p_147192_, boolean p_147193_, @Nullable Entity p_147194_);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityType type, Level level, CallbackInfo ci) {
        this.activeEffects = new Reference2ObjectOpenHashMap<>();
        this.combatTracker = new EvolutionCombatTracker((LivingEntity) (Object) this);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        this.effectHelper.fromNBT(tag.getCompound("EffectHelper"));
    }

    @Inject(method = "updateSwingTime", at = @At("HEAD"), cancellable = true)
    private void onUpdateSwingTime(CallbackInfo ci) {
        if (!this.swinging && this.swingTime == 0) {
            ci.cancel();
        }
    }

    @Shadow
    protected abstract void playHurtSound(DamageSource p_21160_);

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEffectiveAi()Z", ordinal = 0))
    private boolean proxyAiStep(LivingEntity entity) {
        return true;
    }

    @Nullable
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"))
    private HashMap proxyInit() {
        return null;
    }

    @Override
    public void removeAbsorptionSuggestion(float amount) {
        if (amount > 0) {
            float max = this.effectHelper.removeAbsorptionSuggestion(amount);
            if (this.getAbsorptionAmount() > max) {
                this.setAbsorptionAmount(max);
            }
        }
    }

    @Redirect(method = "collectEquipmentChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EquipmentSlot;values()" +
                                                                                      "[Lnet/minecraft/world/entity/EquipmentSlot;"))
    private EquipmentSlot[] removeAllocation() {
        return AdditionalSlotType.SLOTS;
    }

    @Shadow
    public abstract void setAbsorptionAmount(float pAmount);

    @Shadow
    public abstract void setArrowCount(int p_85034_1_);

//    /**
//     * @author TheGreatWolf
//     * <p>
//     * Remove the annoying noise when using an item for other players.
//     */
//    @Overwrite
//    protected void playEquipSound(ItemStack stack) {
//        if (!stack.isEmpty()) {
//            SoundEvent sound = null;
//            Item item = stack.getItem();
//            if (item instanceof ArmorItem) {
//                sound = ((ArmorItem) item).getMaterial().getEquipSound();
//            }
//            else if (item == Items.ELYTRA) {
//                sound = SoundEvents.ARMOR_EQUIP_ELYTRA;
//            }
//            else if (item instanceof IAdditionalEquipment) {
//                sound = ((IAdditionalEquipment) item).getEquipSound();
//            }
//            if (sound != null) {
//                this.playSound(sound, 1.0F, 1.0F);
//            }
//        }
//    }

    @Shadow
    public abstract void setLastHurtByMob(@Nullable LivingEntity p_70604_1_);

    @Shadow
    public abstract void setStingerCount(int p_226300_1_);

    @Override
    public boolean shouldRenderSpecialAttack() {
        if (this.isSpecialAttacking) {
            return true;
        }
        return this.specialAttackStopTicks > 0;
    }

    @Override
    public void startSpecialAttack(IMelee.IAttackType type) {
        this.isSpecialAttacking = true;
        this.specialAttackType = type;
        this.specialAttackTime = 0;
    }

    @Shadow
    public abstract void stopSleeping();

    @Override
    public void stopSpecialAttack(IMelee.StopReason reason) {
        if (reason == IMelee.StopReason.HIT_BLOCK) {
            boolean isStone = IModularTool.get(this.getMainHandItem()).getHead().getMaterialInstance().getMaterial().isStone();
            this.playSound(isStone ? EvolutionSounds.STONE_WEAPON_HIT_BLOCK.get() : EvolutionSounds.METAL_WEAPON_HIT_BLOCK.get(), 0.4f,
                           0.8F + this.random.nextFloat() * 0.4F);
            this.specialAttackStopTicks = 5;
        }
        this.specialAttackCooldown = 2;
        this.isSpecialAttacking = false;
    }

    /**
     * @author TheGreatWolf
     * @reason Overwrite to rotate body
     */
    @Override
    @Overwrite
    public void tick() {
        if (ForgeHooks.onLivingUpdate((LivingEntity) (Object) this)) {
            return;
        }
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level.isClientSide) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }
                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }
            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }
                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }
            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }
            if (this.isSleeping() && !this.checkBedExists()) {
                this.stopSleeping();
            }
        }
        this.aiStep();
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        float dSSq = (float) (dx * dx + dz * dz);
        float f1 = this.yBodyRot;
        this.oRun = this.run;
        float f3 = 0.0F;
        float f2 = 0.0F;
        if (dSSq > 0.002_500_000_2F) {
            f3 = 1.0F;
            f2 = MathHelper.sqrt(dSSq) * 3.0F;
            float f4 = (float) MathHelper.atan2Deg(dz, dx) - 90.0F;
            float f5 = Math.abs(Mth.wrapDegrees(this.getYRot()) - f4);
            if (95.0F < f5 && f5 < 265.0F) {
                f1 = f4 - 180.0F;
            }
            else {
                f1 = f4;
            }
        }
        if (this.attackAnim > 0.0F || LivingEntityHooks.shouldFixRotation((LivingEntity) (Object) this)) {
            f1 = this.getYRot();
        }
        if (!this.onGround) {
            f3 = 0.0F;
        }
        this.run += (f3 - this.run) * 0.3F;
        this.level.getProfiler().push("headTurn");
        f2 = this.tickHeadTurn(f1, f2);
        this.level.getProfiler().popPush("rangeChecks");
        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        while (this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }
        while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }
        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }
        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        while (this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }
        while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }
        this.level.getProfiler().pop();
        this.animStep += f2;
        if (this.isFallFlying()) {
            ++this.fallFlyTicks;
        }
        else {
            this.fallFlyTicks = 0;
        }
        if (this.isSleeping()) {
            this.setXRot(0.0f);
        }
        if (this.getFeetBlockState().getBlock() instanceof IFallSufixBlock fallSufixBlock) {
            ((EvolutionCombatTracker) this.combatTracker).setLastSuffix(fallSufixBlock);
        }
        else if (this.isOnGround()) {
            ((EvolutionCombatTracker) this.combatTracker).setLastSuffix(null);
        }
    }

    /**
     * @author TheGreatWolf
     * @reason Implement evolution effect system, avoid allocations
     */
    @Overwrite
    protected void tickEffects() {
        try {
            boolean canSprint = true;
            boolean canRegen = true;
            float hungerMod = 0.0f;
            float thirstMod = 0.0f;
            double tempMod = 0.0;
            if (!this.activeEffects.isEmpty()) {
                for (Iterator<MobEffect> it = this.activeEffects.keySet().iterator(); it.hasNext(); ) {
                    MobEffect effect = it.next();
                    MobEffectInstance instance = this.activeEffects.get(effect);
                    //noinspection ConstantConditions
                    if (!instance.tick((LivingEntity) (Object) this, null)) {
                        //noinspection ObjectAllocationInLoop
                        if (!this.level.isClientSide &&
                            !MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionExpiryEvent((LivingEntity) (Object) this, instance))) {
                            it.remove();
                            this.onEffectRemoved(instance);
                        }
                    }
                    else if (instance.getDuration() % 600 == 0) {
                        this.onEffectUpdated(instance, false, null);
                    }
                    IMobEffectPatch patch = (IMobEffectPatch) effect;
                    if (canSprint && patch.disablesSprint()) {
                        canSprint = false;
                    }
                    if (canRegen && patch.disablesNaturalRegen()) {
                        canRegen = false;
                    }
                    int lvl = instance.getAmplifier();
                    hungerMod += patch.hungerMod(lvl);
                    thirstMod += patch.thirstMod(lvl);
                    tempMod += patch.tempMod();
                }
            }
            //noinspection ConstantConditions
            this.effectHelper.setCanSprint(canSprint);
            //noinspection ConstantConditions
            this.effectHelper.setCanRegen(canRegen);
            this.effectHelper.setHungerMod(hungerMod);
            this.effectHelper.setThirstMod(thirstMod);
            this.effectHelper.setTemperatureMod(tempMod);
        }
        catch (ConcurrentModificationException ignored) {
        }
        if (this.effectsDirty) {
            if (!this.level.isClientSide) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
            }
            this.effectsDirty = false;
        }
        int color = this.entityData.get(DATA_EFFECT_COLOR_ID);
        boolean ambient = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
        if (color > 0) {
            boolean shouldShow;
            if (this.isInvisible()) {
                shouldShow = this.random.nextInt(15) == 0;
            }
            else {
                shouldShow = this.random.nextBoolean();
            }
            if (ambient) {
                shouldShow &= this.random.nextInt(5) == 0;
            }
            if (shouldShow) {
                double r = (color >> 16 & 255) / 255.0;
                double g = (color >> 8 & 255) / 255.0;
                double b = (color & 255) / 255.0;
                this.level.addParticle(ambient ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getRandomX(0.5),
                                       this.getRandomY(), this.getRandomZ(0.5), r, g, b);
            }
        }
    }

    @Shadow
    protected abstract float tickHeadTurn(float p_110146_1_, float p_110146_2_);

    /**
     * @author TheGreatWolf
     * @reason Replace to handle Evolution's physics.
     */
    @Overwrite
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            if (this.level.getChunkAt(this.blockPosition()).isEmpty()) {
                //Prevents players from moving in unloaded chunks, gaining momentum and then taking damage when the ground finally loads.
                return;
            }
            double gravityAcceleration = Gravity.gravity(this.getLevel().dimensionType());
            FluidState fluidState = this.level.getFluidState(this.blockPosition());
            if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                //handleWaterMovement
                if (this.isOnGround() || this.noJumpDelay > 0) {
                    if (this.getFluidHeight(FluidTags.WATER) <= 0.4) {
                        int level = fluidState.getAmount();
                        float slowdown = 1.0f - 0.05f * level;
                        this.handleNormalMovement(travelVector, gravityAcceleration, slowdown);
                        return;
                    }
                }
                float waterSpeedMult = 0.04F;
                waterSpeedMult *= (float) this.getAttributeValue(ForgeMod.SWIM_SPEED.get());
                Vec3 acceleration = this.getAbsoluteAcceleration(travelVector, waterSpeedMult);
                Vec3 motion = this.getDeltaMovement();
                double motionX = motion.x;
                double motionY = motion.y;
                double motionZ = motion.z;
                double mass = this.getAttributeValue(EvolutionAttributes.MASS.get());
                double verticalDrag = Gravity.verticalWaterDrag(this) / mass;
                double horizontalDrag = this.isSwimming() ? verticalDrag : Gravity.horizontalWaterDrag(this) / mass;
                if (this.horizontalCollision && this.onClimbable()) {
                    motionY = 0.2;
                }
                if (!this.isNoGravity()) {
                    if (this.isSwimming()) {
                        motionY -= gravityAcceleration / 16;
                    }
                    else {
                        motionY -= gravityAcceleration;
                    }
                }
                if (this.horizontalCollision && this.isFree(0, motionY + 1.5, 0)) {
                    motionY = 0.2;
                    if (this.getFluidHeight(FluidTags.WATER) <= 0.4) {
                        motionY += 0.2;
                        this.noJumpDelay = 10;
                    }
                }
                double dragX = Math.signum(motionX) * motionX * motionX * horizontalDrag;
                if (Math.abs(dragX) > Math.abs(motionX / 2)) {
                    dragX = motionX / 2;
                }
                double dragY = Math.signum(motionY) * motionY * motionY * verticalDrag;
                if (Math.abs(dragY) > Math.abs(motionY / 2)) {
                    dragY = motionY / 2;
                    EntityEvents.calculateWaterFallDamage((LivingEntity) (Object) this);
                }
                double dragZ = Math.signum(motionZ) * motionZ * motionZ * horizontalDrag;
                if (Math.abs(dragZ) > Math.abs(motionZ / 2)) {
                    dragZ = motionZ / 2;
                }
                motionX += acceleration.x - dragX;
                motionY += acceleration.y - dragY;
                motionZ += acceleration.z - dragZ;
                this.setDeltaMovement(motionX, motionY, motionZ);
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
            else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                //Handle lava movement
                double posY = this.getY();
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                Vec3 motion = this.getDeltaMovement();
                double motionX = motion.x * 0.5;
                double motionY = motion.y * 0.5;
                double motionZ = motion.z * 0.5;
                if (!this.isNoGravity()) {
                    motionY -= gravityAcceleration / 4;
                }
                if (this.horizontalCollision && this.isFree(motionX, motionY + 0.6 - this.getY() + posY, motionZ)) {
                    motionY = 0.3;
                }
                this.setDeltaMovement(motionX, motionY, motionZ);
            }
            else if (this.isFallFlying()) {
                //Handle elytra movement
                Vec3 motion = this.getDeltaMovement();
                double motionX = motion.x;
                double motionY = motion.y;
                double motionZ = motion.z;
                double mass = this.getAttributeValue(EvolutionAttributes.MASS.get());
                double drag = Gravity.verticalDrag(this) / mass;
                double dragX = Math.signum(motionX) * motionX * motionX * drag;
                double dragY = Math.signum(motionY) * motionY * motionY * drag;
                double dragZ = Math.signum(motionZ) * motionZ * motionZ * drag;
                if (motionY > -0.5) {
                    this.fallDistance = 1.0F;
                }
                Vec3 lookVec = this.getLookAngle();
                float pitchInRad = Mth.DEG_TO_RAD * this.getXRot();
                double horizLookVecLength = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                double horizontalSpeed = motion.horizontalDistance();
                float cosPitch = Mth.cos(pitchInRad);
                cosPitch = (float) (cosPitch * cosPitch * Math.min(1, lookVec.length() / 0.4));
                motionY += gravityAcceleration * (-1 + cosPitch * 0.75);
                if (motionY < 0 && horizLookVecLength > 0) {
                    double d3 = motionY * -0.1 * cosPitch;
                    motionX += lookVec.x * d3 / horizLookVecLength;
                    motionY += d3;
                    motionZ += lookVec.z * d3 / horizLookVecLength;
                }
                if (pitchInRad < 0.0F && horizLookVecLength > 0) {
                    double d13 = horizontalSpeed * -Mth.sin(pitchInRad) * 0.04;
                    motionX += -lookVec.x * d13 / horizLookVecLength;
                    motionY += d13 * 3.2;
                    motionZ += -lookVec.z * d13 / horizLookVecLength;
                }
                if (horizLookVecLength > 0) {
                    motionX += (lookVec.x / horizLookVecLength * horizontalSpeed - motion.x) * 0.1;
                    motionZ += (lookVec.z / horizLookVecLength * horizontalSpeed - motion.z) * 0.1;
                }
                motionX -= dragX;
                motionY -= dragY;
                motionZ -= dragZ;
                this.setDeltaMovement(motionX, motionY, motionZ);
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    this.calculateWallImpact(motionX, motionZ, mass);
                }
                if (this.isOnGround() && !this.level.isClientSide) {
                    this.setSharedFlag(FLAG_FALL_FLYING, false);
                }
            }
            else {
                //handle normal movement
                this.handleNormalMovement(travelVector, gravityAcceleration, 1.0f);
            }
        }
        this.calculateEntityAnimation((LivingEntity) (Object) this, this instanceof FlyingAnimal);
    }

    @Shadow
    protected abstract void updateGlowingStatus();

    @Shadow
    protected abstract void updateInvisibilityStatus();

    @Shadow
    protected abstract void updateSwimAmount();

    @Shadow
    protected abstract void updatingUsingItem();
}
