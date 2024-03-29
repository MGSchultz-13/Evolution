//package tgw.evolution.world.puzzle.pieces;
//
//import net.minecraft.nbt.INBT;
//import net.minecraft.util.Rotation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MutableBoundingBox;
//import net.minecraft.world.IWorld;
//import net.minecraft.world.gen.feature.template.Template;
//import net.minecraft.world.gen.feature.template.TemplateManager;
//import tgw.evolution.util.constants.NBTHelper;
//import tgw.evolution.world.puzzle.EnumPuzzleType;
//import tgw.evolution.world.puzzle.PuzzlePiece;
//import tgw.evolution.world.puzzle.pieces.config.PlacementType;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Random;
//
//public class EmptyPuzzlePiece extends PuzzlePiece {
//
//    public static final EmptyPuzzlePiece INSTANCE = new EmptyPuzzlePiece();
//
//    public EmptyPuzzlePiece() {
//        super(PlacementType.TERRAIN_MATCHING);
//    }
//
//    @Override
//    public MutableBoundingBox getBoundingBox(TemplateManager templateManagerIn, BlockPos pos, Rotation rotationIn) {
//        return MutableBoundingBox.getUnknownBox();
//    }
//
//    @Override
//    public List<Template.BlockInfo> getPuzzleBlocks(TemplateManager templateManagerIn, BlockPos pos, Rotation rotationIn, Random rand) {
//        return Collections.emptyList();
//    }
//
//    @Override
//    public EnumPuzzleType getType() {
//        return EnumPuzzleType.EMPTY;
//    }
//
//    @Override
//    public boolean place(TemplateManager templateManagerIn,
//                         IWorld worldIn,
//                         BlockPos pos,
//                         Rotation rotationIn,
//                         MutableBoundingBox boundsIn,
//                         Random rand) {
//        return true;
//    }
//
//    @Override
//    protected INBT serialize0() {
//        return NBTHelper.emptyMap();
//    }
//
//    @Override
//    public String toString() {
//        return "Empty";
//    }
//}
