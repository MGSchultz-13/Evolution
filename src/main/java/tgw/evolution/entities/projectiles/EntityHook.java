package tgw.evolution.entities.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.Evolution;
import tgw.evolution.blocks.BlockClimbingStake;
import tgw.evolution.blocks.IReplaceable;
import tgw.evolution.blocks.util.BlockUtils;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionEntities;
import tgw.evolution.init.EvolutionItems;
import tgw.evolution.items.IProjectile;
import tgw.evolution.util.damage.DamageSourceEv;
import tgw.evolution.util.physics.SI;

import static tgw.evolution.init.EvolutionBStates.ATTACHED;
import static tgw.evolution.init.EvolutionBStates.DIRECTION_HORIZONTAL;

public class EntityHook extends EntityGenericProjectile {

    private Direction facing = Direction.NORTH;

    public EntityHook(Level level, LivingEntity thrower) {
        super(EvolutionEntities.HOOK, thrower, level, 1);
        this.facing = thrower.getDirection();
    }

    public EntityHook(EntityType<EntityHook> type, Level level) {
        super(type, level);
    }

    public static int tryPlaceRopes(Level level, BlockPos pos, Direction support, int count) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        mutablePos.set(pos);
        Direction currentMovement = support.getOpposite();
        int ropeCount = 0;
        for (int distance = 1; distance <= 5; distance++) {
            if (ropeCount == count) {
                return count;
            }
            mutablePos.move(currentMovement);
            BlockState stateTemp = level.getBlockState(mutablePos);
            if (!BlockUtils.isReplaceable(stateTemp)) {
                return ropeCount;
            }
            if (currentMovement == Direction.DOWN && stateTemp.getBlock() == EvolutionBlocks.ROPE) {
                if (stateTemp.getValue(DIRECTION_HORIZONTAL) == support) {
                    continue;
                }
                return ropeCount;
            }
            if (stateTemp.getBlock() instanceof IReplaceable replaceable) {
                if (!replaceable.canBeReplacedByRope(stateTemp)) {
                    return ropeCount;
                }
            }
            if (currentMovement != Direction.DOWN && stateTemp.getBlock() == EvolutionBlocks.ROPE_GROUND) {
                if (stateTemp.getValue(DIRECTION_HORIZONTAL) == support) {
                    continue;
                }
                return ropeCount;
            }
            if (currentMovement != Direction.DOWN && BlockClimbingStake.canGoDown(level, mutablePos)) {
                currentMovement = Direction.DOWN;
                stateTemp = level.getBlockState(mutablePos.move(Direction.DOWN));
                if (stateTemp.getBlock() == EvolutionBlocks.ROPE) {
                    if (stateTemp.getValue(DIRECTION_HORIZONTAL) == support) {
                        continue;
                    }
                    return ropeCount;
                }
                if (stateTemp.getBlock() == EvolutionBlocks.ROPE_GROUND) {
                    return ropeCount;
                }
//                if (stateTemp.getBlock() instanceof IReplaceable replaceable) {
//                    for (ItemStack stack : replaceable.getDrops(level, mutablePos, stateTemp)) {
//                        BlockUtils.dropItemStack(level, mutablePos, stack);
//                    }
//                }
                level.setBlockAndUpdate(mutablePos, EvolutionBlocks.ROPE.defaultBlockState().setValue(DIRECTION_HORIZONTAL, support));
                ropeCount++;
                continue;
            }
//            if (stateTemp.getBlock() instanceof IReplaceable replaceable) {
//                for (ItemStack stack : replaceable.getDrops(level, mutablePos, stateTemp)) {
//                    BlockUtils.dropItemStack(level, mutablePos, stack);
//                }
//            }
            if (currentMovement == Direction.DOWN) {
                level.setBlockAndUpdate(mutablePos, EvolutionBlocks.ROPE.defaultBlockState().setValue(DIRECTION_HORIZONTAL, support));
                ropeCount++;
                continue;
            }
            level.setBlockAndUpdate(mutablePos, EvolutionBlocks.ROPE_GROUND.defaultBlockState().setValue(DIRECTION_HORIZONTAL, support));
            ropeCount++;
        }
        return ropeCount;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Facing", (byte) this.facing.get3DDataValue());
    }

    @Override
    protected DamageSourceEv createDamageSource() {
        LivingEntity shooter = this.getShooter();
        return EvolutionDamage.causeHookDamage(this, shooter == null ? this : shooter);
    }

    @Override
    protected boolean damagesEntities() {
        return true;
    }

    @Override
    protected ItemStack getArrowStack() {
        return new ItemStack(EvolutionItems.CLIMBING_HOOK);
    }

    @Override
    protected SoundEvent getHitBlockSound() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected @Nullable IProjectile getProjectile() {
        return null;
    }

    @Override
    public double getVolume() {
        //TODO
        return 150 * SI.CUBIC_CENTIMETER;
    }

    @Override
    protected void modifyMovementOnCollision() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.1, -0.1, -0.1));
    }

    @Override
    protected void onBlockHit(BlockState state, int x, int y, int z) {
    }

    @Override
    protected void playHitEntitySound() {
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
        Player player = this.level.getNearestPlayer(this, 128);
        if (player != null) {
            Evolution.usingPlaceholder(player, "sound");
        }
    }

    @Override
    protected void postHitLogic(boolean attackSuccessful) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.facing = Direction.from3DDataValue(tag.getByte("Facing"));
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeInGround > 0) {
            this.tryPlaceBlock();
            this.discard();
        }
    }

    @Override
    protected void tryDespawn() {
    }

    public void tryPlaceBlock() {
        BlockPos pos = this.blockPosition();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.level.isEmptyBlock_(x, y, z) && BlockUtils.hasSolidFace(this.level, x, y - 1, z, Direction.UP)) {
            this.level.setBlockAndUpdate(pos, EvolutionBlocks.CLIMBING_HOOK.defaultBlockState()
                                                                           .setValue(DIRECTION_HORIZONTAL, this.facing.getOpposite()));
            LivingEntity shooter = this.getShooter();
            if (shooter instanceof Player) {
                ItemStack stack = shooter.getOffhandItem();
                if (stack.getItem() == EvolutionItems.ROPE) {
                    int count = stack.getCount();
                    int placed = tryPlaceRopes(this.level, pos, this.facing, count);
                    if (placed > 0) {
                        stack.shrink(placed);
                        this.level.setBlockAndUpdate(pos, EvolutionBlocks.CLIMBING_HOOK.defaultBlockState()
                                                                                       .setValue(DIRECTION_HORIZONTAL, this.facing.getOpposite())
                                                                                       .setValue(ATTACHED, true));
                    }
                }
            }
        }
        else {
            BlockUtils.dropItemStack(this.level, x, y, z, this.getArrowStack());
        }
    }
}
