//package tgw.evolution.blocks;
//
//import net.minecraft.block.Block;
//import net.minecraft.block.BlockState;
//import net.minecraft.block.SoundType;
//import net.minecraft.block.material.Material;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.BlockItem;
//import net.minecraft.item.ItemStack;
//import net.minecraft.tileentity.TileEntity;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.BlockRayTraceResult;
//import net.minecraft.util.math.shapes.ISelectionContext;
//import net.minecraft.util.math.shapes.VoxelShape;
//import net.minecraft.util.math.shapes.VoxelShapes;
//import net.minecraft.world.IBlockReader;
//import net.minecraft.world.World;
//import tgw.evolution.blocks.tileentities.FancyTile;
//
//import javax.annotation.Nullable;
//
//public class FancyBlock extends Block {
//
//    private final VoxelShape shape = VoxelShapes.create(.2, .2, .2, .8, .8, .8);
//
//    public FancyBlock() {
//        super(Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(2.0f));
//    }
//
//    @Override
//    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
//        return this.shape;
//    }
//
//    @Override
//    public boolean hasTileEntity(BlockState state) {
//        return true;
//    }
//
//    @Nullable
//    @Override
//    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
//        return new FancyTile();
//    }
//
//    @Override
//    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
//        ItemStack item = player.getHeldItem(hand);
//        if (!item.isEmpty() && item.getItem() instanceof BlockItem) {
//            if (!world.isRemote) {
//                TileEntity te = world.getTileEntity(pos);
//                if (te instanceof FancyTile) {
//                    BlockState mimicState = ((BlockItem) item.getItem()).getBlock().getDefaultState();
//                    ((FancyTile) te).setMimic(mimicState);
//                }
//            }
//            return true;
//        }
//        return super.onBlockActivated(state, world, pos, player, hand, result);
//    }
//}
