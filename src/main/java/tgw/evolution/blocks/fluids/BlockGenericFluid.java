package tgw.evolution.blocks.fluids;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;
import tgw.evolution.Evolution;
import tgw.evolution.blocks.*;
import tgw.evolution.blocks.tileentities.TELiquid;
import tgw.evolution.capabilities.chunkstorage.CapabilityChunkStorage;
import tgw.evolution.capabilities.chunkstorage.EnumStorage;
import tgw.evolution.init.EvolutionBStates;
import tgw.evolution.util.math.DirectionList;
import tgw.evolution.util.math.DirectionUtil;
import tgw.evolution.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static tgw.evolution.init.EvolutionBStates.LEVEL_1_8;

public abstract class BlockGenericFluid extends BlockMass implements IBlockFluidContainer, IReplaceable, EntityBlock {
    public static final IntegerProperty LEVEL = LEVEL_1_8;
    public static final BooleanProperty FULL = EvolutionBStates.FULL;
    private final List<FluidState> fluidStateCache;
    private final Supplier<? extends FluidGeneric> supplier;
    private boolean fluidStateCacheInitialized;

    public BlockGenericFluid(Supplier<? extends FluidGeneric> fluid, Properties properties, int mass) {
        super(properties, mass);
        this.fluidStateCache = Lists.newArrayList();
        this.registerDefaultState(this.defaultBlockState().setValue(LEVEL, 8).setValue(FULL, true));
        this.supplier = fluid;
    }

    public static int getFluidAmount(Level level, BlockPos pos, BlockState state, FluidState fluid) {
        if (fluid.isEmpty()) {
            return 0;
        }
        Block block = state.getBlock();
        if (block instanceof BlockGenericFluid) {
            int layers = fluid.getValue(LEVEL_1_8);
            int amount = 12_500 * layers;
            if (!fluid.getValue(EvolutionBStates.FALL)) {
                BlockEntity tile = level.getBlockEntity(pos);
                if (tile instanceof TELiquid teLiquid) {
                    amount -= teLiquid.getMissingLiquid();
                }
                else {
                    Evolution.warn("Invalid tile entity for block at {}: {}", pos, tile);
                }
            }
            return amount;
        }
        if (block instanceof IFluidLoggable fluidLoggable) {
            return fluidLoggable.getCurrentAmount(level, pos, state);
        }
        return 0;
    }

    public static void place(Level level, BlockPos pos, FluidGeneric fluid, int amount) {
        fluid.setBlockState(level, pos, amount);
    }

    private static void triggerMixEffects(LevelAccessor level, BlockPos pos) {
        level.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
    }

    @Override
    public boolean canBeReplacedByFluid(BlockState state) {
        if (!state.getValue(FULL)) {
            return true;
        }
        return state.getValue(LEVEL) < 8;
    }

    @Override
    public boolean canBeReplacedByRope(BlockState state) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FULL);
    }

    @Override
    public int getAmountRemoved(Level level, BlockPos pos, int maxAmount) {
        FluidState stateAtPos = level.getFluidState(pos);
        int amountAtPos = FluidGeneric.getFluidAmount(level, pos, stateAtPos);
        int amountRemoved = Math.min(amountAtPos, maxAmount);
        amountAtPos -= amountRemoved;
        this.getFluid().setBlockState(level, pos, amountAtPos);
        for (Direction dir : DirectionUtil.ALL) {
            BlockPos offsetPos = pos.relative(dir);
            BlockState stateAtOffset = level.getBlockState(offsetPos);
            if (stateAtOffset.getBlock() instanceof IFluidLoggable) {
                BlockUtils.scheduleBlockTick(level, offsetPos, 2);
            }
        }
        return amountRemoved;
    }

    @Override
    public NonNullList<ItemStack> getDrops(Level level, BlockPos pos, BlockState state) {
        return NonNullList.of(ItemStack.EMPTY);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public FluidGeneric getFluid(BlockGetter level, BlockPos pos) {
        return this.supplier.get();
    }

    public FluidGeneric getFluid() {
        return this.supplier.get();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (!this.fluidStateCacheInitialized) {
            this.initFluidStateCache();
        }
        int level = state.getValue(LEVEL) - 1;
        if (state.getValue(FULL)) {
            level += 8;
        }
        return this.fluidStateCache.get(level);
    }

    @Override
    public int getMass(Level level, BlockPos pos, BlockState state) {
        return this.getMass(state);
    }

    @Override
    public int getMass(BlockState state) {
        return state.getValue(LEVEL) * this.getBaseMass() / 8;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    protected synchronized void initFluidStateCache() {
        if (!this.fluidStateCacheInitialized) {
            for (int i = 1; i <= 8; i++) {
                this.fluidStateCache.add(this.getFluid().getFlowing(i, false));
            }
            for (int i = 1; i <= 8; i++) {
                this.fluidStateCache.add(this.getFluid().getFlowing(i, true));
            }
            this.fluidStateCacheInitialized = true;
        }
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return !this.getFluid().is(FluidTags.LAVA);
    }

    @Override
    public boolean isReplaceable(BlockState state) {
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (this.reactWithNeighbors(level, pos)) {
            BlockUtils.scheduleFluidTick(level, pos);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TELiquid(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (this.reactWithNeighbors(level, pos)) {
            BlockUtils.scheduleFluidTick(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (isMoving) {
            return;
        }
        Block newBlock = newState.getBlock();
        if (newBlock instanceof BlockGenericFluid || newBlock == Blocks.AIR) {
            return;
        }
        FluidState fluidState;
        if (!(state.getBlock() instanceof BlockGenericFluid)) {
            fluidState = level.getFluidState(pos);
        }
        else {
            fluidState = state.getFluidState();
        }
        int currentAmount = getFluidAmount(level, pos, state, fluidState);
        if (currentAmount == 0) {
            return;
        }
        if (newBlock instanceof IFluidLoggable fluidLoggable) {
            int maxAmount = fluidLoggable.getFluidCapacity(newState);
            int placed = Math.min(currentAmount, maxAmount);
            ((IFluidLoggable) newBlock).setBlockState(level, pos, newState, this.getFluid(), placed);
            currentAmount -= placed;
        }
        if (currentAmount == 0) {
            return;
        }
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        DirectionList list = new DirectionList();
        list.fillHorizontal();
        //Try placing liquid to the sides
        while (!list.isEmpty()) {
            currentAmount = this.tryDisplaceToDirection(level, pos, list.getRandomAndRemove(MathHelper.RANDOM), currentAmount, 16);
            if (currentAmount == 0) {
                return;
            }
        }
        mutablePos.set(pos);
        //try placing liquid up
        currentAmount = this.tryDisplaceToDirection(level, pos, Direction.UP, currentAmount, 255 - pos.getY());
        if (currentAmount == 0) {
            return;
        }
        CapabilityChunkStorage.add(level.getChunkAt(pos), EnumStorage.WATER, currentAmount);
    }

    @Override
    public void onReplaced(BlockState state, Level level, BlockPos pos) {
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        level.getFluidState(pos).randomTick(level, pos, random);
    }

    public boolean reactWithNeighbors(Level level, BlockPos pos) {
        if (this.getFluid().is(FluidTags.LAVA)) {
            boolean flag = false;
            for (Direction direction : DirectionUtil.ALL) {
                if (direction != Direction.DOWN && level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                FluidState ifluidstate = level.getFluidState(pos);
                if (ifluidstate.isSource()) {
                    level.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.OBSIDIAN.defaultBlockState()));
                    triggerMixEffects(level, pos);
                    return false;
                }
                if (ifluidstate.getHeight(level, pos) >= 0.444_444_45F) {
                    level.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.COBBLESTONE.defaultBlockState()));
                    triggerMixEffects(level, pos);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int receiveFluid(Level level, BlockPos pos, BlockState originalState, FluidGeneric fluid, int amount) {
        FluidState stateAtPos = originalState.getFluidState();
        FluidGeneric fluidAtPos = this.getFluid();
        if (fluidAtPos == fluid) {
            int amountAtPos = FluidGeneric.getFluidAmount(level, pos, stateAtPos);
            int amountToReplace = Math.min(amountAtPos + amount, 100_000);
            fluid.setBlockState(level, pos, amountToReplace);
            return amountToReplace - amountAtPos;
        }
        Evolution.warn("Fluids are different, handle them!");
        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getFluidState().getType().isSame(this.getFluid()) || state.canOcclude();
    }

    public abstract int tryDisplaceIn(Level level, BlockPos pos, BlockState stateAtPos, FluidGeneric otherFluid, int amount);

    public int tryDisplaceToDirection(Level level, BlockPos origin, Direction direction, int amount, int maxDistance) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        mutablePos.set(origin);
        for (int offset = 1; offset <= maxDistance; offset++) {
            BlockState stateAtPos = level.getBlockState(mutablePos);
            if (!FluidGeneric.canSendToOrReceiveFrom(stateAtPos, direction)) {
                return amount;
            }
            mutablePos.move(direction);
            stateAtPos = level.getBlockState(mutablePos);
            if (!FluidGeneric.canSendToOrReceiveFrom(stateAtPos, direction.getOpposite())) {
                return amount;
            }
            if (FluidGeneric.isFull(level, mutablePos)) {
                continue;
            }
            if (!BlockUtils.canBeReplacedByFluid(stateAtPos)) {
                return amount;
            }
            if (this.getFluid().isEquivalentOrEmpty(level, mutablePos)) {
                int amountAlreadyAtPos = FluidGeneric.getFluidAmount(level, mutablePos, level.getFluidState(mutablePos));
                int capacity = FluidGeneric.getCapacity(stateAtPos);
                int placed = Math.min(amountAlreadyAtPos + amount, capacity);
                this.getFluid().setBlockState(level, mutablePos, placed);
                FluidGeneric.onReplace(level, mutablePos, stateAtPos);
                amount = amount - placed + amountAlreadyAtPos;
                if (amount == 0) {
                    return 0;
                }
                continue;
            }
            Fluid otherFluid = level.getFluidState(mutablePos).getType();
            if (otherFluid instanceof FluidGeneric fluidGeneric) {
                amount = this.tryDisplaceIn(level, mutablePos, stateAtPos, fluidGeneric, amount);
                if (amount == 0) {
                    return 0;
                }
                continue;
            }
            Evolution.warn("Invalid fluid at {}: {}", mutablePos, otherFluid);
        }
        return amount;
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction facing,
                                  BlockState facingState,
                                  LevelAccessor level,
                                  BlockPos currentPos,
                                  BlockPos facingPos) {
        BlockUtils.scheduleFluidTick(level, currentPos);
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }
}
