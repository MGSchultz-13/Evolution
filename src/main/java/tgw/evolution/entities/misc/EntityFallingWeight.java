package tgw.evolution.entities.misc;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;
import tgw.evolution.Evolution;
import tgw.evolution.blocks.*;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionEntities;
import tgw.evolution.util.Gravity;

import javax.annotation.Nullable;
import java.util.List;

public class EntityFallingWeight extends Entity implements IEntityAdditionalSpawnData {

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    public int fallTime;
    private int mass = 500;
    private BlockState state = EvolutionBlocks.DESTROY_9.get().getDefaultState();

    public EntityFallingWeight(@SuppressWarnings("unused") FMLPlayMessages.SpawnEntity spawn, World world) {
        this(EvolutionEntities.FALLING_WEIGHT.get(), world);
    }

    public EntityFallingWeight(EntityType<EntityFallingWeight> type, World world) {
        super(type, world);
    }

    public EntityFallingWeight(World world, double x, double y, double z, BlockState state) {
        this(EvolutionEntities.FALLING_WEIGHT.get(), world);
        this.state = state;
        this.mass = this.state.getBlock() instanceof BlockMass ? ((BlockMass) this.state.getBlock()).getMass(this.state) : 500;
        this.preventEntitySpawning = true;
        this.setPosition(x, y, z);
        this.setMotion(Vec3d.ZERO);
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
    }

    @Override
    public void applyEntityCollision(Entity entity) {
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (this.world.isRemote) {
            return;
        }
        boolean isRock = this.state.getMaterial() == Material.ROCK;
        boolean isWood = this.state.getMaterial() == Material.WOOD;
        boolean isSoil = this.state.getMaterial() == Material.EARTH ||
                         this.state.getMaterial() == Material.CLAY ||
                         this.state.getMaterial() == Material.SAND;
        boolean isMetal = this.state.getMaterial() == Material.IRON;
        DamageSource source = null;
        if (isRock) {
            source = EvolutionDamage.FALLING_ROCK;
        }
        else if (isSoil) {
            source = EvolutionDamage.FALLING_SOIL;
        }
        else if (isWood) {
            source = EvolutionDamage.FALLING_WOOD;
        }
        else if (isMetal) {
            source = EvolutionDamage.FALLING_METAL;
        }
        if (source == null) {
            Evolution.LOGGER.warn("No falling damage for block {}", this.state);
        }
        float motionY = 20.0F * (float) this.getMotion().y;
        List<Entity> list = Lists.newArrayList(this.world.getEntitiesWithinAABBExcludingEntity(this, this.getBoundingBox()));
        float kinecticEnergy = this.mass * motionY * motionY / 2;
        for (Entity entity : list) {
            float forceOfImpact = kinecticEnergy / entity.getHeight();
            float area = entity.getWidth() * entity.getWidth();
            float pressure = forceOfImpact / area;
            pressure += this.mass * Gravity.gravity(this.world.dimension) / area;
            float damage = pressure / 344_738.0F * 100.0F;
            entity.attackEntityFrom(source, damage);
        }
    }

    @Override
    public void fillCrashReport(CrashReportCategory category) {
        super.fillCrashReport(category);
        category.func_71507_a("Immitating BlockState", this.state.toString());
    }

    /**
     * Returns the {@code BlockState} this entity is immitating.
     */
    public BlockState getBlockState() {
        return this.state;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox() {
        return this.isAlive() ? this.getBoundingBox() : null;
    }

    @Override
    protected void readAdditional(CompoundNBT compound) {
        this.state = NBTUtil.readBlockState(compound.getCompound("BlockState"));
        this.fallTime = compound.getInt("Time");
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        this.state = NBTUtil.readBlockState(buffer.readCompoundTag());
    }

    @Override
    protected void registerData() {
    }

    @Override
    public void tick() {
        if (this.state.isAir()) {
            this.remove();
            return;
        }
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        Block carryingBlock = this.state.getBlock();
        if (this.fallTime++ == 0) {
            BlockPos pos = new BlockPos(this);
            if (this.world.getBlockState(pos).getBlock() == carryingBlock) {
                this.world.removeBlock(pos, false);
            }
            else if (!this.world.isRemote) {
                this.remove();
                return;
            }
        }
        Vec3d motion = this.getMotion();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        double gravity = 0;
        if (!this.hasNoGravity()) {
            gravity = Gravity.gravity(this.world.dimension);
        }
        double horizontalDrag = this.isInWater() ? Gravity.horizontalWaterDrag(this) / this.mass : Gravity.horizontalDrag(this) / this.mass;
        double verticalDrag = this.isInWater() ? Gravity.verticalWaterDrag(this) / this.mass : Gravity.verticalDrag(this) / this.mass;
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
        motionX -= dragX;
        motionY += -gravity - dragY;
        motionZ -= dragZ;
        if (Math.abs(motionX) < 1e-6) {
            motionX = 0;
        }
        if (Math.abs(motionY) < 1e-6) {
            motionY = 0;
        }
        if (Math.abs(motionZ) < 1e-6) {
            motionZ = 0;
        }
        this.setMotion(motionX, motionY, motionZ);
        this.move(MoverType.SELF, this.getMotion());
        this.mutablePos.setPos(this.posX, this.posY, this.posZ);
        if (this.world.getBlockState(this.mutablePos.down()).getBlock() instanceof BlockLeaves) {
            if (this.state.getBlock() instanceof BlockLeaves) {
                this.world.setBlockState(this.mutablePos, this.state);
                this.remove();
            }
        }
        if (this.world.getBlockState(this.mutablePos).getBlock() instanceof BlockLeaves) {
            if (!(this.state.getBlock() instanceof BlockLeaves)) {
                this.world.setBlockState(this.mutablePos, Blocks.AIR.getDefaultState());
                this.playSound(SoundEvents.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
            }
        }
        this.mutablePos.setPos(this);
        boolean isInWater = this.world.getFluidState(this.mutablePos).isTagged(FluidTags.WATER);
        double d0 = this.getMotion().lengthSquared();
        if (d0 > 1.0D) {
            BlockRayTraceResult raytraceresult = this.world.rayTraceBlocks(new RayTraceContext(new Vec3d(this.prevPosX, this.prevPosY, this.prevPosZ),
                                                                                               new Vec3d(this.posX, this.posY, this.posZ),
                                                                                               RayTraceContext.BlockMode.COLLIDER,
                                                                                               RayTraceContext.FluidMode.SOURCE_ONLY,
                                                                                               this));
            if (raytraceresult.getType() != RayTraceResult.Type.MISS && this.world.getFluidState(raytraceresult.getPos()).isTagged(FluidTags.WATER)) {
                this.mutablePos.setPos(raytraceresult.getPos());
                isInWater = true;
            }
        }
        if (!this.onGround && !isInWater) {
            if (this.fallTime > 100 && !this.world.isRemote && (this.mutablePos.getY() < 1 || this.mutablePos.getY() > 256) ||
                this.fallTime > 6_000) {
                if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                    if (carryingBlock instanceof BlockCobblestone) {
                        this.entityDropItem(new ItemStack(((BlockCobblestone) carryingBlock).getVariant().getRock(), 4));
                    }
                    else {
                        this.entityDropItem(carryingBlock);
                    }
                }
                this.remove();
            }
        }
        else {
            BlockState state = this.world.getBlockState(this.mutablePos);
            BlockPos posDown = new BlockPos(this.posX, this.posY - 0.01, this.posZ);
            if (this.world.isAirBlock(posDown)) {
                if (!isInWater && FallingBlock.canFallThrough(this.world.getBlockState(posDown))) {
                    this.onGround = false;
                    return;
                }
            }
            if ((!isInWater || this.onGround) && state.getBlock() != Blocks.MOVING_PISTON) {
                this.remove();
                if (BlockUtils.isReplaceable(state)) {
                    ItemStack stack;
                    if (state.getBlock() instanceof IReplaceable) {
                        stack = ((IReplaceable) state.getBlock()).getDrops(this.world, this.mutablePos, state);
                    }
                    else {
                        stack = new ItemStack(state.getBlock());
                    }
                    if (this.world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
                        this.entityDropItem(stack);
                    }
                    this.world.setBlockState(this.mutablePos, this.state, 3);
                }
                else if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                    if (carryingBlock instanceof BlockCobblestone) {
                        this.entityDropItem(new ItemStack(((BlockCobblestone) carryingBlock).getVariant().getRock(), 4));
                    }
                    else {
                        this.entityDropItem(carryingBlock);
                    }
                }
            }
        }
    }

    @Override
    protected void writeAdditional(CompoundNBT compound) {
        compound.put("BlockState", NBTUtil.writeBlockState(this.state));
        compound.putInt("Time", this.fallTime);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeCompoundTag(NBTUtil.writeBlockState(this.state));
    }
}