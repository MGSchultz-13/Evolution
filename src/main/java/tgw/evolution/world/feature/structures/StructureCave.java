//package tgw.evolution.world.feature.structures;
//
//import com.mojang.datafixers.Dynamic;
//import net.minecraft.util.SharedSeedRandom;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.util.math.MutableBoundingBox;
//import net.minecraft.world.biome.Biome;
//import net.minecraft.world.gen.ChunkGenerator;
//import net.minecraft.world.gen.Heightmap;
//import net.minecraft.world.gen.feature.NoFeatureConfig;
//import net.minecraft.world.gen.feature.structure.Structure;
//import net.minecraft.world.gen.feature.structure.StructureStart;
//import net.minecraft.world.gen.feature.template.TemplateManager;
//import tgw.evolution.world.feature.structures.config.ConfigStructCave;
//
//import java.util.Random;
//import java.util.function.Function;
//
//public class StructureCave extends Structure<NoFeatureConfig> {
//
//    public StructureCave(Function<Dynamic<?>, ? extends NoFeatureConfig> configFactoryIn) {
//        super(configFactoryIn);
//    }
//
//    protected int getSeedModifier() {
//        return 11_150_548;
//    }
//
//    @Override
//    public int getSize() {
//        return 0;
//    }
//
//    @Override
//    public IStartFactory getStartFactory() {
//        return StructureCave.Start::new;
//    }
//
//    @Override
//    protected ChunkPos getStartPositionForPosition(ChunkGenerator<?> chunkGenerator,
//                                                   Random random,
//                                                   int x,
//                                                   int z,
//                                                   int spacingOffsetsX,
//                                                   int spacingOffsetsZ) {
//        int maxDistance = 8;
//        int xTemp = x + maxDistance * spacingOffsetsX;
//        int ztemp = z + maxDistance * spacingOffsetsZ;
//        int xTemp2 = xTemp < 0 ? xTemp - maxDistance + 1 : xTemp;
//        int zTemp2 = ztemp < 0 ? ztemp - maxDistance + 1 : ztemp;
//        int validChunkX = xTemp2 / maxDistance;
//        int validChunkZ = zTemp2 / maxDistance;
//        ((SharedSeedRandom) random).setLargeFeatureSeedWithSalt(chunkGenerator.getSeed(), validChunkX, validChunkZ, this.getSeedModifier());
//        validChunkX = validChunkX * maxDistance;
//        validChunkZ = validChunkZ * maxDistance;
//        int minDistance = 6;
//        validChunkX = validChunkX + random.nextInt(maxDistance - minDistance);
//        validChunkZ = validChunkZ + random.nextInt(maxDistance - minDistance);
//        return new ChunkPos(validChunkX, validChunkZ);
//    }
//
//    @Override
//    public String getStructureName() {
//        return "evolution:cave";
//    }
//
//    @Override
//    public boolean hasStartAt(ChunkGenerator<?> chunkGen, Random rand, int chunkPosX, int chunkPosZ) {
//        ChunkPos chunkpos = this.getStartPositionForPosition(chunkGen, rand, chunkPosX, chunkPosZ, 0, 0);
//        return chunkPosX == chunkpos.x && chunkPosZ == chunkpos.z;
//    }
//
//    private static class Start extends StructureStart {
//
//        public Start(Structure<?> structureIn, int chunkX, int chunkZ, Biome biomeIn, MutableBoundingBox boundsIn, int referenceIn, long seed) {
//            super(structureIn, chunkX, chunkZ, biomeIn, boundsIn, referenceIn, seed);
//        }
//
//        @Override
//        public void init(ChunkGenerator<?> generator, TemplateManager templateManagerIn, int chunkX, int chunkZ, Biome biomeIn) {
//            ConfigStructCave caveConfig = new ConfigStructCave(generator.getSeed(), chunkX, chunkZ);
//            //
//            int x = (chunkX << 4) + 8;
//            int z = (chunkZ << 4) + 8;
//            //
//            int surfaceY = caveConfig.hasEntrance() ?
//                           generator.func_222531_c(x, z, Heightmap.Type.WORLD_SURFACE_WG) :
//                           16 + caveConfig.getRandom().nextInt(12);
//            //
//            BlockPos pos = new BlockPos(x, surfaceY + 1, z);
//            StructureCavePieces.start(generator, templateManagerIn, pos, this.components, this.rand, caveConfig);
//            this.recalculateStructureSize();
//        }
//    }
//}
