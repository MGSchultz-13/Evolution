package tgw.evolution.entities.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import tgw.evolution.blocks.BlockPeat;
import tgw.evolution.blocks.IReplaceable;
import tgw.evolution.entities.IEvolutionEntity;
import tgw.evolution.init.EvolutionBStates;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionEntities;
import tgw.evolution.util.constants.NBTTypes;
import tgw.evolution.util.earth.Gravity;
import tgw.evolution.util.hitbox.HitboxEntity;

import javax.annotation.Nullable;

public class EntityFallingPeat extends Entity implements IEntityAdditionalSpawnData, IEvolutionEntity<EntityFallingPeat> {

    public static final EntityDimensions[] DIMENSIONS = {EntityDimensions.scalable(1.0f, 0.25f),
                                                         EntityDimensions.scalable(1.0f, 0.5f),
                                                         EntityDimensions.scalable(1.0f, 0.75f),
                                                         EntityDimensions.scalable(1.0f, 1.0f)};
    public int fallTime;
    private boolean isSizeCorrect;
    private int layers;
    private int mass = 289;
    private BlockPos prevPos;

    public EntityFallingPeat(EntityType<EntityFallingPeat> type, Level level) {
        super(type, level);
    }

    public EntityFallingPeat(Level level, double x, double y, double z, int layers) {
        super(EvolutionEntities.FALLING_PEAT.get(), level);
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.layers = layers;
        this.mass = 289 * this.layers;
        this.prevPos = this.blockPosition();
    }

    public EntityFallingPeat(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(EvolutionEntities.FALLING_PEAT.get(), level);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Time", this.fallTime);
        tag.putByte("Layers", (byte) this.layers);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean displayFireAnimation() {
        return false;
    }

    //TODO
//    @Override
//    public boolean func_241845_aY() {
//        return this.isAlive();
//    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public BlockState getBlockState() {
        return EvolutionBlocks.PEAT.get().defaultBlockState().setValue(EvolutionBStates.LAYERS_1_4, this.layers);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return DIMENSIONS[this.layers - 1];
    }

    @Nullable
    @Override
    public HitboxEntity<EntityFallingPeat> getHitbox() {
        return null;
    }

    @Override
    public boolean hasHitboxes() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

//    @Override
//    protected boolean isMovementNoisy() {
//        return false;
//    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.fallTime = tag.getInt("Time");
        if (tag.contains("Layers", NBTTypes.BYTE)) {
            this.layers = tag.getByte("Layers");
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.layers = buffer.readByte();
    }

    @Override
    public void tick() {
        if (!this.isSizeCorrect) {
            this.refreshDimensions();
            this.isSizeCorrect = true;
        }
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        ++this.fallTime;
        Vec3 motion = this.getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        double gravity = 0;
        if (!this.isNoGravity()) {
            gravity = Gravity.gravity(this.level.dimensionType());
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
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.move(MoverType.SELF, this.getDeltaMovement());
        BlockPos pos = this.blockPosition();
        if (!this.level.isClientSide) {
            if (!this.onGround) {
                if (this.fallTime > 100 && (pos.getY() < 1 || pos.getY() > 256) || this.fallTime > 600) {
                    this.discard();
                }
                else if (!pos.equals(this.prevPos)) {
                    this.prevPos = pos;
                }
            }
            else {
                BlockState state = this.level.getBlockState(pos);
                if (state.getBlock() != Blocks.MOVING_PISTON) {
                    BlockPeat.placeLayersOn(this.level, pos, this.layers);
                    if (state.getBlock() instanceof IReplaceable && state.getBlock() != this.getBlockState().getBlock()) {
                        for (ItemStack stack : ((IReplaceable) state.getBlock()).getDrops(this.level, pos, state)) {
                            this.spawnAtLocation(stack);
                        }
                    }
                    this.discard();
                }
            }
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeByte(this.layers);
    }
}
