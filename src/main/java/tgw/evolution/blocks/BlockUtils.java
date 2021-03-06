package tgw.evolution.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import tgw.evolution.util.DirectionToIntMap;
import tgw.evolution.util.OriginMutableBlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static tgw.evolution.init.EvolutionBStates.TREE;

public final class BlockUtils {

    private BlockUtils() {
    }

    public static boolean canBeReplacedByFluid(BlockState state) {
        if (state.getBlock() instanceof IReplaceable) {
            if (!((IReplaceable) state.getBlock()).canBeReplacedByFluid(state)) {
                return false;
            }
        }
        return isReplaceable(state);
    }

    public static boolean canSustainSapling(BlockState state, IPlantable plantable) {
        return plantable instanceof BlockBush && BlockBush.isValidGround(state);
    }

    public static boolean compareVanillaBlockStates(BlockState vanilla, BlockState evolution) {
        if (vanilla.getBlock() == Blocks.GRASS_BLOCK) {
            return evolution.getBlock() instanceof BlockGrass;
        }
        return false;
    }

    public static void dropItemStack(World world, BlockPos pos, @Nonnull ItemStack stack) {
        if (world.isRemote || stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5f, pos.getY() + 0.3f, pos.getZ() + 0.5f, stack);
        Vec3d motion = entity.getMotion();
        entity.addVelocity(-motion.x, -motion.y, -motion.z);
        world.addEntity(entity);
    }

    /**
     * @param state The BlockState of the ladder
     * @return The ladder up movement speed, in m/t.
     */
    public static double getLadderUpSpeed(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof IClimbable) {
            return ((IClimbable) block).getUpSpeed();
        }
        return 0.1;
    }

    @Nullable
    public static Axis getSmallestBeam(DirectionToIntMap beams) {
        int x = beams.getBeamSize(Axis.X);
        int z = beams.getBeamSize(Axis.Z);
        if (x == 0) {
            if (z == 0) {
                return null;
            }
            return Axis.Z;
        }
        if (z == 0) {
            return Axis.X;
        }
        return x < z ? Axis.X : Axis.Z;
    }

    public static boolean hasMass(BlockState state) {
        return state.getBlock() instanceof BlockMass;
    }

    public static boolean hasSolidSide(World world, BlockPos pos, Direction side) {
        return Block.hasSolidSide(world.getBlockState(pos), world, pos, side);
    }

    /**
     * Returns whether the blockstate is considered replaceable.
     */
    public static boolean isReplaceable(BlockState state) {
        return state.getMaterial().isReplaceable() ||
               state.getBlock() instanceof IReplaceable && ((IReplaceable) state.getBlock()).isReplaceable(state);
    }

    public static boolean isTouchingWater(IWorld world, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            mutablePos.setPos(pos).move(direction);
            BlockState stateAtPos = world.getBlockState(mutablePos);
            if (stateAtPos.getFluidState().isTagged(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the tree trunk is supported.
     */
    public static boolean isTrunkSustained(World world, OriginMutableBlockPos pos) {
        BlockState state = world.getBlockState(pos.down().getPos());
        if (!isReplaceable(state)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().north().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().south().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().west().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().east().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().north().east().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().north().west().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().west().south().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().down().east().south().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().north().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().south().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().east().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().west().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().north().west().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().south().west().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().north().east().getPos());
        if (state.getBlock() instanceof BlockLog && state.get(TREE)) {
            return true;
        }
        state = world.getBlockState(pos.reset().south().east().getPos());
        return state.getBlock() instanceof BlockLog && state.get(TREE);
    }

    public static void scheduleBlockTick(World world, BlockPos pos, int tickrate) {
        world.getPendingBlockTicks().scheduleTick(pos, world.getBlockState(pos).getBlock(), tickrate);
    }

    public static void scheduleFluidTick(IWorld world, BlockPos pos) {
        IFluidState fluidState = world.getFluidState(pos);
        if (fluidState.isEmpty()) {
            return;
        }
        Fluid fluid = fluidState.getFluid();
        world.getPendingFluidTicks().scheduleTick(pos, fluid, fluid.getTickRate(world));
    }
}
