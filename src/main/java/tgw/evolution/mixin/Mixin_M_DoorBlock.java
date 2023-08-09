package tgw.evolution.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tgw.evolution.hooks.asm.DeleteMethod;

@Mixin(DoorBlock.class)
public abstract class Mixin_M_DoorBlock extends Block {

    @Shadow @Final public static DirectionProperty FACING;
    @Shadow @Final public static BooleanProperty OPEN;
    @Shadow @Final public static EnumProperty<DoorHingeSide> HINGE;
    @Shadow @Final public static EnumProperty<DoubleBlockHalf> HALF;
    @Shadow @Final public static BooleanProperty POWERED;
    @Shadow @Final protected static VoxelShape EAST_AABB;
    @Shadow @Final protected static VoxelShape SOUTH_AABB;
    @Shadow @Final protected static VoxelShape WEST_AABB;
    @Shadow @Final protected static VoxelShape NORTH_AABB;

    public Mixin_M_DoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Overwrite
    @DeleteMethod
    public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean canSurvive_(BlockState state, LevelReader level, int x, int y, int z) {
        BlockState stateBelow = level.getBlockState_(x, y - 1, z);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ?
               stateBelow.isFaceSturdy_(level, x, y - 1, z, Direction.UP) :
               stateBelow.is(this);
    }

    @Shadow
    protected abstract int getCloseSound();

    @Shadow
    protected abstract int getOpenSound();

    @Override
    @Overwrite
    @DeleteMethod
    public long getSeed(BlockState blockState, BlockPos blockPos) {
        throw new AbstractMethodError();
    }

    @Override
    public long getSeed_(BlockState state, int x, int y, int z) {
        return Mth.getSeed(x, state.getValue(HALF) == DoubleBlockHalf.LOWER ? y : y - 1, z);
    }

    @Override
    @Overwrite
    @DeleteMethod
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        throw new AbstractMethodError();
    }

    @Override
    public VoxelShape getShape_(BlockState state, BlockGetter level, int x, int y, int z, @Nullable Entity entity) {
        Direction direction = state.getValue(FACING);
        boolean open = !state.getValue(OPEN);
        boolean rightHinge = state.getValue(HINGE) == DoorHingeSide.RIGHT;
        return switch (direction) {
            case SOUTH -> open ? SOUTH_AABB : rightHinge ? EAST_AABB : WEST_AABB;
            case WEST -> open ? WEST_AABB : rightHinge ? SOUTH_AABB : NORTH_AABB;
            case NORTH -> open ? NORTH_AABB : rightHinge ? WEST_AABB : EAST_AABB;
            default -> open ? EAST_AABB : rightHinge ? NORTH_AABB : SOUTH_AABB;
        };
    }

    @Shadow
    public abstract boolean isOpen(BlockState blockState);

    @Override
    @Overwrite
    @DeleteMethod
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
        throw new AbstractMethodError();
    }

    @Override
    public void neighborChanged_(BlockState state,
                                 Level level,
                                 int x,
                                 int y,
                                 int z,
                                 Block oldBlock,
                                 int fromX,
                                 int fromY,
                                 int fromZ,
                                 boolean isMoving) {
        BlockPos pos = new BlockPos(x, y, z);
        boolean hasSignal = level.hasNeighborSignal(pos) ||
                            level.hasNeighborSignal(pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
        if (!this.defaultBlockState().is(oldBlock) && hasSignal != state.getValue(POWERED)) {
            if (hasSignal != state.getValue(OPEN)) {
                this.playSound(level, pos, hasSignal);
                level.gameEvent(hasSignal ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            level.setBlock(pos, state.setValue(POWERED, hasSignal).setValue(OPEN, hasSignal), 2);
        }
    }

    @Shadow
    protected abstract void playSound(Level level, BlockPos blockPos, boolean bl);

    @Override
    @Overwrite
    @DeleteMethod
    public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
        throw new AbstractMethodError();
    }

    @Override
    public void playerWillDestroy_(Level level, int x, int y, int z, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            DoublePlantBlock.preventCreativeDropFromBottomPart(level, new BlockPos(x, y, z), state, player);
        }
        super.playerWillDestroy_(level, x, y, z, state, player);
    }

    @Override
    @Overwrite
    @DeleteMethod
    public BlockState updateShape(BlockState blockState,
                                  Direction direction,
                                  BlockState blockState2,
                                  LevelAccessor levelAccessor,
                                  BlockPos blockPos,
                                  BlockPos blockPos2) {
        throw new AbstractMethodError();
    }

    @Override
    public BlockState updateShape_(BlockState state,
                                   Direction from,
                                   BlockState fromState,
                                   LevelAccessor level,
                                   int x,
                                   int y,
                                   int z,
                                   int fromX,
                                   int fromY,
                                   int fromZ) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (from.getAxis() == Direction.Axis.Y && doubleBlockHalf == DoubleBlockHalf.LOWER == (from == Direction.UP)) {
            return fromState.is(this) && fromState.getValue(HALF) != doubleBlockHalf ?
                   state.setValue(FACING, fromState.getValue(FACING))
                        .setValue(OPEN, fromState.getValue(OPEN))
                        .setValue(HINGE, fromState.getValue(HINGE))
                        .setValue(POWERED, fromState.getValue(POWERED)) :
                   Blocks.AIR.defaultBlockState();
        }
        return doubleBlockHalf == DoubleBlockHalf.LOWER && from == Direction.DOWN && !state.canSurvive_(level, x, y, z) ?
               Blocks.AIR.defaultBlockState() :
               super.updateShape_(state, from, fromState, level, x, y, z, fromX, fromY, fromZ);
    }

    @Override
    @Overwrite
    @DeleteMethod
    public InteractionResult use(BlockState blockState,
                                 Level level,
                                 BlockPos blockPos,
                                 Player player,
                                 InteractionHand interactionHand,
                                 BlockHitResult blockHitResult) {
        throw new AbstractMethodError();
    }

    @Override
    public InteractionResult use_(BlockState state, Level level, int x, int y, int z, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (this.material == Material.METAL) {
            return InteractionResult.PASS;
        }
        state = state.cycle(OPEN);
        BlockPos pos = new BlockPos(x, y, z);
        level.setBlock(pos, state, 10);
        boolean isOpen = this.isOpen(state);
        level.levelEvent(player, isOpen ? this.getOpenSound() : this.getCloseSound(), pos, 0);
        level.gameEvent(player, isOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
