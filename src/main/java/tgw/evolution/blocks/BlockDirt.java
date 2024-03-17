package tgw.evolution.blocks;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionSounds;
import tgw.evolution.util.constants.RockVariant;

public class BlockDirt extends BlockPhysics implements IRockVariant, ISloppable, IFillable {

    private final RockVariant variant;

    public BlockDirt(RockVariant variant) {
        super(Properties.of(Material.DIRT).strength(2.0F, 0.5F).sound(SoundType.GRAVEL));
        this.variant = variant;
    }

    @Override
    public boolean canSlope(BlockGetter level, int x, int y, int z) {
        Block blockUp = level.getBlockState_(x, y + 1, z).getBlock();
        if (blockUp == this.variant.get(EvolutionBlocks.GRASSES)) {
            return false;
        }
        return blockUp != this || level.getBlockState_(x, y + 2, z).getBlock() != this.variant.get(EvolutionBlocks.GRASSES);
    }

    @Override
    public @Nullable SoundEvent fallingSound() {
        return EvolutionSounds.SOIL_COLLAPSE;
    }

    @Override
    public float getFrictionCoefficient(BlockState state) {
        return 0.63f;
    }

    @Override
    public RockVariant rockVariant() {
        return this.variant;
    }

    @Override
    public float slopeChance() {
        return 1;
    }
}
