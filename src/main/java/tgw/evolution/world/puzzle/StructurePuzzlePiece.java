//package tgw.evolution.world.puzzle;
//
//import com.google.common.collect.Lists;
//import com.mojang.serialization.Dynamic;
//import net.minecraft.nbt.CompoundNBT;
//import net.minecraft.nbt.ListNBT;
//import net.minecraft.nbt.NBTDynamicOps;
//import net.minecraft.util.Rotation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.util.math.MutableBoundingBox;
//import net.minecraft.world.ISeedReader;
//import net.minecraft.world.gen.ChunkGenerator;
//import net.minecraft.world.gen.feature.structure.IStructurePieceType;
//import net.minecraft.world.gen.feature.structure.StructureManager;
//import net.minecraft.world.gen.feature.structure.StructurePiece;
//import net.minecraft.world.gen.feature.template.TemplateManager;
//import tgw.evolution.world.puzzle.pieces.EmptyPuzzlePiece;
//
//import java.util.List;
//import java.util.Random;
//
//public abstract class StructurePuzzlePiece extends StructurePiece {
//
//    protected final PuzzlePiece puzzlePiece;
//    protected final Rotation rotation;
//    private final int groundLevelDelta;
//    private final List<PuzzleJunction> junctions = Lists.newArrayList();
//    private final TemplateManager templateManager;
//    protected BlockPos pos;
//
//    public StructurePuzzlePiece(IStructurePieceType pieceType,
//                                TemplateManager manager,
//                                PuzzlePiece puzzlePiece,
//                                BlockPos pos,
//                                int groundLevelDelta,
//                                Rotation rotation,
//                                MutableBoundingBox boundingBox) {
//        super(pieceType, 0);
//        this.templateManager = manager;
//        this.puzzlePiece = puzzlePiece;
//        this.pos = pos;
//        this.groundLevelDelta = groundLevelDelta;
//        this.rotation = rotation;
//        this.boundingBox = boundingBox;
//    }
//
//    public StructurePuzzlePiece(TemplateManager manager, CompoundNBT nbt, IStructurePieceType pieceType) {
//        super(pieceType, nbt);
//        this.templateManager = manager;
//        this.pos = new BlockPos(nbt.getInt("PosX"), nbt.getInt("PosY"), nbt.getInt("PosZ"));
//        this.groundLevelDelta = nbt.getInt("GroundLevelDelta");
//        this.puzzlePiece = PuzzleDeserializerHelper.deserialize(nbt.getCompound("Elements"), "PieceType", EmptyPuzzlePiece.INSTANCE);
//        this.rotation = Rotation.valueOf(nbt.getString("Rot"));
//        this.boundingBox = this.puzzlePiece.getBoundingBox(manager, this.pos, this.rotation);
//        ListNBT listnbt = nbt.getList("Junc", 10);
//        this.junctions.clear();
//        listnbt.forEach(inbt -> this.junctions.add(PuzzleJunction.func_236819_a_(new Dynamic<>(NBTDynamicOps.INSTANCE, inbt))));
//    }
//
//    public void addJunction(PuzzleJunction junction) {
//        this.junctions.add(junction);
//    }
//
//    @Override
//    public boolean func_230383_a_(ISeedReader world,
//                                  StructureManager structureManager,
//                                  ChunkGenerator generator,
//                                  Random random,
//                                  MutableBoundingBox bb,
//                                  ChunkPos chunkPos,
//                                  BlockPos pos) {
//        return this.puzzlePiece.place(this.templateManager, world, this.pos, this.rotation, bb, random);
//    }
//
//    public int getGroundLevelDelta() {
//        return this.groundLevelDelta;
//    }
//
//    public List<PuzzleJunction> getJunctions() {
//        return this.junctions;
//    }
//
//    public BlockPos getPos() {
//        return this.pos;
//    }
//
//    public PuzzlePiece getPuzzlePiece() {
//        return this.puzzlePiece;
//    }
//
//    @Override
//    public Rotation getRotation() {
//        return this.rotation;
//    }
//
//    @Override
//    public void offset(int x, int y, int z) {
//        super.offset(x, y, z);
//        this.pos = this.pos.add(x, y, z);
//    }
//
//    @Override
//    protected void readAdditional(CompoundNBT tagCompound) {
//        tagCompound.putInt("PosX", this.pos.getX());
//        tagCompound.putInt("PosY", this.pos.getY());
//        tagCompound.putInt("PosZ", this.pos.getZ());
//        tagCompound.putInt("GroundLevelDelta", this.groundLevelDelta);
//        tagCompound.put("Elements", this.puzzlePiece.serialize());
//        tagCompound.putString("Rot", this.rotation.name());
//        ListNBT junctionList = new ListNBT();
//        for (PuzzleJunction puzzleJunction : this.junctions) {
//            junctionList.add(puzzleJunction.serialize(NBTDynamicOps.INSTANCE).getValue());
//        }
//        tagCompound.put("Junc", junctionList);
//    }
//
//    @Override
//    public String toString() {
//        return String.format("<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.pos, this.rotation, this.puzzlePiece);
//    }
//}
