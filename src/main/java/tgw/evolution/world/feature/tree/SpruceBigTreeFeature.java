//package tgw.evolution.world.feature.tree;
//
//import com.mojang.datafixers.Dynamic;
//import net.minecraft.block.BlockState;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.util.math.MutableBoundingBox;
//import net.minecraft.world.IBlockReader;
//import net.minecraft.world.IWorld;
//import net.minecraft.world.gen.IWorldGenerationReader;
//import net.minecraft.world.gen.feature.HugeTreesFeature;
//import net.minecraft.world.gen.feature.NoFeatureConfig;
//import tgw.evolution.blocks.BlockLeaves;
//import tgw.evolution.blocks.BlockSapling;
//import tgw.evolution.blocks.util.BlockUtils;
//import tgw.evolution.init.EvolutionBlocks;
//import tgw.evolution.util.TreeUtils;
//
//import java.util.Random;
//import java.util.Set;
//import java.util.function.Function;
//
//import static tgw.evolution.init.EvolutionBStates.TREE;
//
//public class SpruceBigTreeFeature extends HugeTreesFeature<NoFeatureConfig> {
//
//    private static final BlockState TRUNK = EvolutionBlocks.LOG_SPRUCE.get().getDefaultState().with(TREE, true);
//    private static final BlockState LEAF = EvolutionBlocks.LEAVES_SPRUCE.get().getDefaultState();
//    private final boolean useBaseHeight;
//
//    public SpruceBigTreeFeature(Function<Dynamic<?>, ? extends NoFeatureConfig> config, boolean notify, boolean p_i45457_2_) {
//        super(config, notify, 13, 15, TRUNK, LEAF);
//        this.useBaseHeight = p_i45457_2_;
//    }
//
//    private static boolean check(IWorldGenerationReader worldIn, BlockPos pos) {
//        BlockPos blockpos = pos.down();
//        boolean isSoil = BlockUtils.canSustainSapling(((IBlockReader) worldIn).getBlockState(blockpos), EvolutionBlocks.SAPLING_SPRUCE.get());
//        if (isSoil && pos.getY() >= 2) {
//            TreeUtils.setDirtAt(worldIn, blockpos);
//            TreeUtils.setDirtAt(worldIn, blockpos.east());
//            TreeUtils.setDirtAt(worldIn, blockpos.south());
//            TreeUtils.setDirtAt(worldIn, blockpos.south().east());
//            return true;
//        }
//        return false;
//    }
//
//    private static boolean isSpaceAt(IWorldGenerationReader worldIn, BlockPos leavesPos, int height) {
//        int worldHeight = worldIn instanceof IWorld ? ((IWorld) worldIn).getWorld().getHeight() : 256;
//        if (leavesPos.getY() >= 1 && leavesPos.getY() + height + 1 <= worldHeight) {
//            boolean flag = true;
//            for (int i = 0; i <= 1 + height; ++i) {
//                int j = 2;
//                if (i == 0) {
//                    j = 1;
//                }
//                for (int k = -j; k <= j && flag; ++k) {
//                    for (int l = -j; l <= j && flag; ++l) {
//                        if (leavesPos.getY() + i < 0 || leavesPos.getY() + i >= 256 || !BlockSapling.canGrowInto(worldIn, leavesPos.add(k, i, l))) {
//                            flag = false;
//                        }
//                    }
//                }
//            }
//            return flag;
//        }
//        return false;
//    }
//
//    public boolean allChecks(IWorldGenerationReader worldIn, BlockPos pos, int p_203427_3_) {
//        return isSpaceAt(worldIn, pos, p_203427_3_) && check(worldIn, pos);
//    }
//
//    private void createCrown(IWorldGenerationReader worldIn, int x, int z, int y, int p_150541_5_, Random rand) {
//        int i = rand.nextInt(5) + (this.useBaseHeight ? this.baseHeight : 3);
//        int j = 0;
//        BlockPos.MutableBlockPos leafPos = new BlockPos.MutableBlockPos();
//        for (int k = y - i; k <= y; ++k) {
//            int l = y - k;
//            int i1 = p_150541_5_ + MathHelper.floor(l / (float) i * 3.5F);
//            this.growLeavesLayerStrict(worldIn, leafPos.setPos(x, k, z), i1 + (l > 0 && i1 == j && (k & 1) == 0 ? 1 : 0));
//            j = i1;
//        }
//    }
//
//    protected void growLeavesLayerStrict(IWorldGenerationReader worldIn, BlockPos layerCenter, int width) {
//        int i = width * width;
//        for (int j = -width; j <= width + 1; ++j) {
//            for (int k = -width; k <= width + 1; ++k) {
//                int l = Math.min(Math.abs(j), Math.abs(j - 1));
//                int i1 = Math.min(Math.abs(k), Math.abs(k - 1));
//                if (l + i1 < 7 && l * l + i1 * i1 <= i) {
//                    BlockPos blockpos = layerCenter.add(j, 0, k);
//                    BlockState iblockstate = ((IBlockReader) worldIn).getBlockState(blockpos);
//                    if (iblockstate.isAir((IBlockReader) worldIn, blockpos) || iblockstate.getBlock() instanceof BlockLeaves) {
//                        this.setBlockState(worldIn, blockpos, LEAF);
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    public boolean place(Set<BlockPos> changedBlocks, IWorldGenerationReader world, Random rand, BlockPos position, MutableBoundingBox box) {
//        int i = this.getHeight(rand);
//        if (!this.allChecks(world, position, i)) {
//            return false;
//        }
//        this.createCrown(world, position.getX(), position.getZ(), position.getY() + i, 0, rand);
//        for (int j = 0; j < i; ++j) {
//            BlockState iblockstate = ((IBlockReader) world).getBlockState(position.up(j));
//            if (iblockstate.isAir((IBlockReader) world, position.up(j)) || iblockstate.getBlock() instanceof BlockLeaves) {
//                this.setLogState(changedBlocks, world, position.up(j), TRUNK, box);
//            }
//            if (j < i - 1) {
//                iblockstate = ((IBlockReader) world).getBlockState(position.add(1, j, 0));
//                if (iblockstate.isAir((IBlockReader) world, position.add(1, j, 0)) || iblockstate.getBlock() instanceof BlockLeaves) {
//                    this.setLogState(changedBlocks, world, position.add(1, j, 0), TRUNK, box);
//                }
//                iblockstate = ((IBlockReader) world).getBlockState(position.add(1, j, 1));
//                if (iblockstate.isAir((IBlockReader) world, position.add(1, j, 1)) || iblockstate.getBlock() instanceof BlockLeaves) {
//                    this.setLogState(changedBlocks, world, position.add(1, j, 1), TRUNK, box);
//                }
//                iblockstate = ((IBlockReader) world).getBlockState(position.add(0, j, 1));
//                if (iblockstate.isAir((IBlockReader) world, position.add(0, j, 1)) || iblockstate.getBlock() instanceof BlockLeaves) {
//                    this.setLogState(changedBlocks, world, position.add(0, j, 1), TRUNK, box);
//                }
//            }
//        }
//        return true;
//    }
//}
