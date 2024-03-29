//package tgw.evolution.entities;
//
//import net.minecraft.core.Direction;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.PathfinderMob;
//import net.minecraft.world.entity.Pose;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import tgw.evolution.patches.PatchEntity;
//
//public abstract class EntityGenericCreature extends PathfinderMob implements PatchEntity<EntityGenericCreature> {
//
//    protected static final EntityDataAccessor<Boolean> DEAD = SynchedEntityData.defineId(EntityGenericCreature.class, EntityDataSerializers
//    .BOOLEAN);
//    protected static final EntityDataAccessor<Boolean> SKELETON = SynchedEntityData.defineId(EntityGenericCreature.class,
//                                                                                             EntityDataSerializers.BOOLEAN);
//    protected int deathTimer;
//
//    protected EntityGenericCreature(EntityType<? extends EntityGenericCreature> type, Level level) {
//        super(type, level);
//    }
//
//    @Override
//    public void addAdditionalSaveData(CompoundTag tag) {
//        super.addAdditionalSaveData(tag);
//        tag.putInt("DeathTimer", this.deathTimer);
//        tag.putBoolean("Dead", this.entityData.get(DEAD));
//    }
//
//    @Override
//    public void aiStep() {
//        super.aiStep();
//        if (this.isDead()) {
//            this.deathTimer++;
//            if (this.deathTimer == 1) {
//                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
//                this.navigation.stop();
//            }
//            if (!this.isSkeleton() && this.skeletonTime() > 0) {
//                if (this.deathTimer >= this.skeletonTime()) {
//                    if (this.becomesSkeleton()) {
//                        this.entityData.set(SKELETON, true);
//                    }
//                    else {
//                        this.discard();
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * @return Whether this entity becomes a skeleton after {@link EntityGenericCreature#skeletonTime()} ticks or simply disappears.
//     */
//    public abstract boolean becomesSkeleton();
//
//    @Override
//    public boolean canBeCollidedWith() {
//        return true;
//    }
//
//    @Override
//    protected void defineSynchedData() {
//        super.defineSynchedData();
//        this.entityData.define(DEAD, false);
//        this.entityData.define(SKELETON, false);
//    }
//
//    @Override
//    public abstract double getBaseHealth();
//
//    @Override
//    public abstract double getBaseWalkForce();
//
//    @Override
//    public Direction getBedOrientation() {
//        return Direction.UP;
//    }
//
//    /**
//     * Gets the time the entity has been dead for, in ticks.
//     */
//    public int getDeathTime() {
//        return this.deathTimer;
//    }
//
//    @Override
//    protected int getExperienceReward(Player player) {
//        return 0;
//    }
//
//    @Override
//    public abstract float getFrictionModifier();
//
//    /**
//     * @return The leg height of the entity in m.
//     */
//    public abstract double getLegHeight();
//
//    @Override
//    public boolean isAttackable() {
//        return !this.entityData.get(DEAD);
//    }
//
//    /**
//     * @return Whether this entity is in the 'dead state'.
//     */
//    public boolean isDead() {
//        return this.entityData.get(DEAD);
//    }
//
//    @Override
//    public boolean isInvulnerableTo(DamageSource source) {
//        return this.isInvulnerable() && !source.isCreativePlayer();
//    }
//
//    @Override
//    public boolean isPushable() {
//        return true;
//    }
//
//    public boolean isSkeleton() {
//        return this.entityData.get(SKELETON);
//    }
//
//    /**
//     * Kills this entity.
//     */
//    public void onKilled() {
//        this.entityData.set(DEAD, true);
//        this.setInvulnerable(true);
//        this.setHealth(this.getMaxHealth());
//        this.setPose(Pose.DYING);
//        for (int i = 0; i < 20; i++) {
//            double dx = this.random.nextGaussian() * 0.02;
//            double dy = this.random.nextGaussian() * 0.02;
//            double dz = this.random.nextGaussian() * 0.02;
//            this.level.addParticle(ParticleTypes.POOF, this.getRandomX(1), this.getRandomY(), this.getRandomZ(1), dx, dy, dz);
//        }
//    }
//
//    @Override
//    public void readAdditionalSaveData(CompoundTag tag) {
//        super.readAdditionalSaveData(tag);
//        this.deathTimer = tag.getInt("DeathTimer");
//        this.entityData.set(DEAD, tag.getBoolean("Dead"));
//    }
//
//    @Override
//    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
//        return false;
//    }
//
//    @Override
//    public boolean requiresCustomPersistence() {
//        return true;
//    }
//
//    /**
//     * @return The time it takes for the entity's body to become a 'skeleton', in ticks.
//     * If {@code 0}, the entity does not turn into skeleton.
//     */
//    public abstract int skeletonTime();
//}
