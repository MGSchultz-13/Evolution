package tgw.evolution.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.SoundEvent;
import tgw.evolution.init.EvolutionSounds;
import tgw.evolution.util.RockVariant;

public class BlockGravel extends BlockGravity implements IStoneVariant {

    private final RockVariant variant;

    public BlockGravel(RockVariant variant) {
        super(Block.Properties.create(Material.SAND).hardnessAndResistance(2.0F, 0.6F).sound(SoundType.GROUND), variant.getMass() / 2);
        this.variant = variant;
    }

    @Override
    public boolean canSlope() {
        return true;
    }

    @Override
    public SoundEvent fallSound() {
        return EvolutionSounds.SOIL_COLLAPSE.get();
    }

    @Override
    public float getFrictionCoefficient(BlockState state) {
        return 0.5f;
    }

    @Override
    public RockVariant getVariant() {
        return this.variant;
    }
}
