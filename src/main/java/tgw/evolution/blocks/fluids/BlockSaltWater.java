package tgw.evolution.blocks.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import tgw.evolution.Evolution;
import tgw.evolution.init.EvolutionFluids;

public class BlockSaltWater extends BlockGenericFluid {

    public BlockSaltWater() {
        super(EvolutionFluids.SALT_WATER, Block.Properties.of(Material.WATER).noCollission().noDrops(), 1_023);
    }

    @Override
    public int tryDisplaceIn(Level level, BlockPos pos, BlockState state, FluidGeneric otherFluid, int amount) {
        switch (otherFluid.getId()) {
            case FluidGeneric.FRESH_WATER -> {
                int amountAlreadyAtPos = FluidGeneric.getFluidAmount(level, pos, level.getFluidState(pos));
                int capacity = FluidGeneric.getCapacity(state);
                int placed = Math.min(amountAlreadyAtPos + amount, capacity);
                this.getFluid().setBlockState(level, pos, placed);
                FluidGeneric.onReplace(level, pos, state);
                amount = amount - placed + amountAlreadyAtPos;
                return amount;
            }
        }
        Evolution.warn("Try displace of " + this.getRegistryName() + " with " + otherFluid.getRegistryName() + " is not yet implemented!");
        return amount;
    }
}
