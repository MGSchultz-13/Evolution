//package tgw.evolution.world.feature.structures;
//
//import com.google.common.collect.ImmutableList;
//import com.mojang.datafixers.util.Pair;
//import net.minecraft.nbt.CompoundNBT;
//import net.minecraft.util.ResourceLocation;
//import net.minecraft.util.Rotation;
//import net.minecraft.util.SharedSeedRandom;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MutableBoundingBox;
//import net.minecraft.world.gen.ChunkGenerator;
//import net.minecraft.world.gen.feature.structure.StructurePiece;
//import net.minecraft.world.gen.feature.template.TemplateManager;
//import tgw.evolution.Evolution;
//import tgw.evolution.world.feature.EvolutionFeatures;
//import tgw.evolution.world.feature.structures.config.ConfigStructCave;
//import tgw.evolution.world.feature.structures.config.IConfigStruct;
//import tgw.evolution.world.puzzle.PuzzleManager;
//import tgw.evolution.world.puzzle.PuzzlePattern;
//import tgw.evolution.world.puzzle.PuzzlePiece;
//import tgw.evolution.world.puzzle.StructurePuzzlePiece;
//import tgw.evolution.world.puzzle.pieces.CavePuzzlePiece;
//import tgw.evolution.world.puzzle.pieces.SinglePuzzlePiece;
//import tgw.evolution.world.puzzle.pieces.config.CaveConfigPuzzle;
//import tgw.evolution.world.puzzle.pieces.config.CivilizationType;
//import tgw.evolution.world.puzzle.pieces.config.PlacementType;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public final class StructureCavePieces {
//
//    public static final ResourceLocation GEN_SUP = Evolution.getResource("gen_sup");
//    public static final ResourceLocation GEN_UND = Evolution.getResource("gen_und");
//    public static final ResourceLocation SUP = Evolution.getResource("sup");
//    public static final ResourceLocation CAV = Evolution.getResource("cav");
//    public static final ResourceLocation TERM = Evolution.getResource("term");
//    //
//    private static final List<Pair<PuzzlePiece, Integer>> CAV_LIST = new ArrayList<>();
//    private static final List<Pair<PuzzlePiece, Integer>> TERM_LIST = new ArrayList<>();
//    private static final List<Pair<PuzzlePiece, Integer>> SUP_LIST = new ArrayList<>();
//
//    static {
//        add(CAV_LIST, "caves/cav/cav_mini_2hol1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_bif1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_bif2", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_bif3", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_cor1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_corn1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_holent1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_holpause1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_u1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_mini_uver1", 10, new CaveConfigPuzzle().underground());
//        add(CAV_LIST, "caves/cav/cav_stair1", 10, new CaveConfigPuzzle().underground());
//        //--//--//
//        add(CAV_LIST, "caves/cav/tribal/tribal_mega_stair1", 4, new CaveConfigPuzzle().underground().mega().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_2hol1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_bif1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_bif2", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_bif3", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_cor1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_corn1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_holent1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_holpause1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_u1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_mini_uver1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        add(CAV_LIST, "caves/cav/tribal/tribal_stair1", 10, new CaveConfigPuzzle().underground().civ(CivilizationType.TRIBAL));
//        //--//--//--//
//        add(SUP_LIST, "caves/sup/cav_mega_sup_fenda1", 4, new CaveConfigPuzzle().mega());
//        add(SUP_LIST, "caves/sup/cav_mega_sup_florest1", 4, new CaveConfigPuzzle().mega());
//        add(SUP_LIST, "caves/sup/cav_sup_tree1", 10, new CaveConfigPuzzle());
//        //--//--//xd
//        add(SUP_LIST, "caves/sup/tribal/tribal_sup_building1", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(SUP_LIST, "caves/sup/tribal/tribal_sup_temple1", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        //--//--//--//
//        add(TERM_LIST, "caves/term/cav_fim_ent1", 10, new CaveConfigPuzzle().underground());
//        add(TERM_LIST, "caves/term/cav_fim_ent2", 10, new CaveConfigPuzzle().underground());
//        add(TERM_LIST, "caves/term/cav_fim_holu1", 10, new CaveConfigPuzzle().underground());
//        add(TERM_LIST, "caves/term/cav_fim_holu2", 10, new CaveConfigPuzzle().underground());
//        add(TERM_LIST, "caves/term/cav_fim_hold1", 10, new CaveConfigPuzzle().underground());
//        add(TERM_LIST, "caves/term/cav_fim_hold2", 10, new CaveConfigPuzzle().underground());
//        //--//--//
//        add(TERM_LIST, "caves/term/blight_fim_ent1", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        add(TERM_LIST, "caves/term/blight_fim_ent2", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        add(TERM_LIST, "caves/term/blight_fim_holu1", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        add(TERM_LIST, "caves/term/blight_fim_holu2", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        add(TERM_LIST, "caves/term/blight_fim_hold1", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        add(TERM_LIST, "caves/term/blight_fim_hold2", 10, new CaveConfigPuzzle().civ(CivilizationType.BLIGHTTOWN));
//        //--//--//
//        add(TERM_LIST, "caves/term/tribal_fim_ent1", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(TERM_LIST, "caves/term/tribal_fim_ent2", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(TERM_LIST, "caves/term/tribal_fim_holu1", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(TERM_LIST, "caves/term/tribal_fim_holu2", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(TERM_LIST, "caves/term/tribal_fim_hold1", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        add(TERM_LIST, "caves/term/tribal_fim_hold2", 10, new CaveConfigPuzzle().civ(CivilizationType.TRIBAL));
//        //--//--//--//
//        for (Pair<PuzzlePiece, Integer> piece : TERM_LIST) {
//            add(CAV_LIST, piece.getFirst(), 4);
//        }
//        //--//--//--//
//        PuzzleManager.REGISTRY.register(new PuzzlePattern(GEN_SUP,
//                                                          Evolution.getResource("empty"),
//                                                          ImmutableList.of(Pair.of(new SinglePuzzlePiece("evolution:caves/gen_sup/gen_sup"), 1)),
//                                                          PlacementType.RIGID));
//        PuzzleManager.REGISTRY.register(new PuzzlePattern(GEN_UND,
//                                                          Evolution.getResource("empty"),
//                                                          ImmutableList.of(Pair.of(new SinglePuzzlePiece("evolution:caves/gen_und/gen_und"), 1)),
//                                                          PlacementType.RIGID));
//        PuzzleManager.REGISTRY.register(new PuzzlePattern(SUP, Evolution.getResource("empty"), SUP_LIST, PlacementType.RIGID));
//        PuzzleManager.REGISTRY.register(new PuzzlePattern(CAV, TERM, CAV_LIST, PlacementType.RIGID));
//        PuzzleManager.REGISTRY.register(new PuzzlePattern(TERM, Evolution.getResource("empty"), TERM_LIST, PlacementType.RIGID));
//    }
//
//    private StructureCavePieces() {
//    }
//
//    private static void add(List<Pair<PuzzlePiece, Integer>> list, String local, int weight, CaveConfigPuzzle caveConfig) {
//        list.add(Pair.of(new CavePuzzlePiece("evolution:" + local, caveConfig), weight));
//    }
//
//    private static void add(List<Pair<PuzzlePiece, Integer>> list, PuzzlePiece piece, int weight) {
//        list.add(Pair.of(piece, weight));
//    }
//
//    public static void start(ChunkGenerator<?> generator,
//                             TemplateManager manager,
//                             BlockPos pos,
//                             List<StructurePiece> pieces,
//                             SharedSeedRandom random,
//                             IConfigStruct config) {
//        int size = 10;
//        if (((ConfigStructCave) config).hasEntrance()) {
//            PuzzleManager.startGeneration(GEN_SUP, size, StructureCavePieces.Piece::new, generator, manager, pos, pieces, random, config);
//        }
//        else {
//            PuzzleManager.startGeneration(GEN_UND, size, StructureCavePieces.Piece::new, generator, manager, pos, pieces, random, config);
//        }
//    }
//
//    public static class Piece extends StructurePuzzlePiece {
//
//        public Piece(TemplateManager templateManagerIn,
//                     PuzzlePiece jigsawPieceIn,
//                     BlockPos posIn,
//                     int groundLevelDelta,
//                     Rotation rotationIn,
//                     MutableBoundingBox boundsIn) {
//            super(EvolutionFeatures.PIECE_CAVE, templateManagerIn, jigsawPieceIn, posIn, groundLevelDelta, rotationIn, boundsIn);
//        }
//
//        public Piece(TemplateManager manager, CompoundNBT nbt) {
//            super(manager, nbt, EvolutionFeatures.PIECE_CAVE);
//        }
//    }
//}
