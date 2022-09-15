package tgw.evolution.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

import static tgw.evolution.init.EvolutionBStates.SNOWY;

public abstract class BlockGenericSnowable extends BlockGravity implements ICollisionBlock {

    protected BlockGenericSnowable(Block.Properties builder, int mass) {
        super(builder, mass);
        this.registerDefaultState(this.defaultBlockState().setValue(SNOWY, false));
    }

    @Override
    public boolean collision(Level level, BlockPos pos, Entity entity, double speed, double mass, @Nullable Direction.Axis axis) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNOWY);
    }

    @Override
    public float getSlowdownSide(BlockState state) {
        return 1.0f;
    }

    @Override
    public float getSlowdownTop(BlockState state) {
        if (state.getValue(SNOWY)) {
            return 0.95f;
        }
        return 1.0f;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Block block = context.getLevel().getBlockState(context.getClickedPos().above()).getBlock();
        //TODO proper snow
        return this.defaultBlockState().setValue(SNOWY, block == Blocks.SNOW_BLOCK || block == Blocks.SNOW);
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction facing,
                                  BlockState facingState,
                                  LevelAccessor level,
                                  BlockPos currentPos,
                                  BlockPos facingPos) {
        if (facing != Direction.UP) {
            return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        }
        Block block = facingState.getBlock();
        //TODO proper snow
        return state.setValue(SNOWY, block == Blocks.SNOW_BLOCK || block == Blocks.SNOW);
    }
}