package tgw.evolution.blocks.fluids;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import tgw.evolution.Evolution;
import tgw.evolution.init.EvolutionFluids;
import tgw.evolution.util.HarvestLevel;
import tgw.evolution.util.MathHelper;

public class BlockSaltWater extends BlockGenericFluid {

    public BlockSaltWater() {
        super(EvolutionFluids.SALT_WATER,
              Block.Properties.create(Material.WATER).doesNotBlockMovement().hardnessAndResistance(HarvestLevel.UNBREAKABLE).noDrops(),
              1_023);
    }

    @Override
    public int tryDisplaceIn(World world, BlockPos pos, BlockState state, FluidGeneric otherFluid, int amount) {
        switch (otherFluid.getId()) {
            case FluidGeneric.FRESH_WATER:
                int amountAlreadyAtPos = FluidGeneric.getFluidAmount(world, pos, world.getFluidState(pos));
                int capacity = FluidGeneric.getCapacity(state);
                int placed = MathHelper.clampMax(amountAlreadyAtPos + amount, capacity);
                this.getFluid().setBlockState(world, pos, placed);
                FluidGeneric.onReplace(world, pos, state);
                amount = amount - placed + amountAlreadyAtPos;
                return amount;
        }
        Evolution.LOGGER.warn("Try displace of " + this.getRegistryName() + " with " + otherFluid.getRegistryName() + " is not yet implemented!");
        return amount;
    }
}
