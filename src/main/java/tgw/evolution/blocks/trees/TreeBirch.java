package tgw.evolution.blocks.trees;

import net.minecraft.block.trees.Tree;
import net.minecraft.world.gen.feature.BaseTreeFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import tgw.evolution.init.EvolutionConfiguredFeatures;

import javax.annotation.Nullable;
import java.util.Random;

public class TreeBirch extends Tree {

    @Override
    @Nullable
    protected ConfiguredFeature<BaseTreeFeatureConfig, ?> getConfiguredFeature(Random random, boolean notify) {
        return EvolutionConfiguredFeatures.TREE_BIRCH;
    }
}