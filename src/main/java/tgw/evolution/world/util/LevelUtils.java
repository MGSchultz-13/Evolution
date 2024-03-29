package tgw.evolution.world.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.util.OptionalMutableBlockPos;
import tgw.evolution.util.math.BlockPosUtil;

public final class LevelUtils {

    private LevelUtils() {
    }

    public static boolean collidesWithSuffocatingBlock(CollisionGetter level, @Nullable Entity entity,
                                                       double minX, double minY, double minZ,
                                                       double maxX, double maxY, double maxZ) {
        try (CollisionShapeCalculator calculator = CollisionShapeCalculator.getInstance(level, entity, minX, minY, minZ, maxX, maxY, maxZ, true)) {
            for (VoxelShape shape : calculator) {
                if (!shape.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsAnyLiquid(LevelReader level, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return containsAnyLiquid(level, Mth.floor(minX), Mth.floor(minY), Mth.floor(minZ), Mth.ceil(maxX), Mth.ceil(maxY), Mth.ceil(maxZ));
    }

    public static boolean containsAnyLiquid(LevelReader level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int cachedX = Integer.MAX_VALUE;
        int cachedZ = Integer.MAX_VALUE;
        ChunkAccess cachedChunk = null;
        for (int x = minX; x < maxX; ++x) {
            int chunkX = SectionPos.blockToSectionCoord(x);
            for (int z = minZ; z < maxZ; ++z) {
                int chunkZ = SectionPos.blockToSectionCoord(z);
                if (chunkX != cachedX || chunkZ != cachedZ) {
                    cachedX = chunkX;
                    cachedZ = chunkZ;
                    cachedChunk = level.getChunk(chunkX, chunkZ);
                }
                assert cachedChunk != null;
                for (int y = minY; y < maxY; ++y) {
                    if (!cachedChunk.getFluidState_(x, y, z).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static @Nullable BlockPos findSpawn(ServerLevel level, int posX, int posZ) {
        boolean flag = level.dimensionType().hasCeiling();
        LevelChunk levelchunk = level.getChunk(SectionPos.blockToSectionCoord(posX), SectionPos.blockToSectionCoord(posZ));
        int i = flag ?
                level.getChunkSource().getGenerator().getSpawnHeight(level) :
                levelchunk.getHeight(Heightmap.Types.MOTION_BLOCKING, posX & 15, posZ & 15);
        if (i < level.getMinBuildHeight()) {
            return null;
        }
        int j = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, posX & 15, posZ & 15);
        if (j <= i && j > levelchunk.getHeight(Heightmap.Types.OCEAN_FLOOR, posX & 15, posZ & 15)) {
            return null;
        }
        for (int k = i + 1; k >= level.getMinBuildHeight(); --k) {
            BlockState blockstate = level.getBlockState_(posX, k, posZ);
            if (!blockstate.getFluidState().isEmpty()) {
                break;
            }
            if (Block.isFaceFull(blockstate.getCollisionShape_(level, posX, k, posZ), Direction.UP)) {
                return new BlockPos(posX, k + 1, posZ);
            }
        }
        return null;
    }

    @CanIgnoreReturnValue
    public static @Nullable BlockPos findSupportingBlockPos(CollisionGetter level,
                                                            Entity entity,
                                                            double minX,
                                                            double minY,
                                                            double minZ,
                                                            double maxX,
                                                            double maxY,
                                                            double maxZ,
                                                            @Nullable OptionalMutableBlockPos pos) {
        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int z = Integer.MAX_VALUE;
        try (CollisionPosCalculator calculator = CollisionPosCalculator.getInstance(level, entity, minX, minY, minZ, maxX, maxY, maxZ, false)) {
            double minDist = Double.MAX_VALUE;
            for (BlockPos blockPos : calculator) {
                assert blockPos != null;
                double dist = blockPos.distToCenterSqr(entity.position());
                if (dist < minDist || dist == minDist && (x == Integer.MAX_VALUE || BlockPosUtil.compare(x, y, z, blockPos) < 0)) {
                    x = blockPos.getX();
                    y = blockPos.getY();
                    z = blockPos.getZ();
                    minDist = dist;
                }
            }
        }
        if (x == Integer.MAX_VALUE) {
            if (pos != null) {
                pos.remove();
            }
            return null;
        }
        if (pos != null) {
            pos.set(x, y, z);
            return pos.get();
        }
        return new BlockPos(x, y, z);
    }

    private static boolean forceHasChunk(LevelAccessor level, int secX, int secZ) {
        return level.getChunkSource().hasChunk(secX, secZ);
    }

    public static boolean forceHasChunksAt(LevelAccessor level, int minX, int minZ, int maxX, int maxZ) {
        int secX0 = SectionPos.blockToSectionCoord(minX);
        int secX1 = SectionPos.blockToSectionCoord(maxX);
        int secZ0 = SectionPos.blockToSectionCoord(minZ);
        int secZ1 = SectionPos.blockToSectionCoord(maxZ);
        for (int x = secX0; x <= secX1; ++x) {
            for (int z = secZ0; z <= secZ1; ++z) {
                if (!forceHasChunk(level, x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int getLightBlockInto(BlockGetter level,
                                        BlockState state,
                                        int x,
                                        int y,
                                        int z,
                                        BlockState blockingState,
                                        int bx,
                                        int by,
                                        int bz,
                                        Direction direction,
                                        int lightBlock) {
        boolean occludes = state.canOcclude() && state.useShapeForLightOcclusion();
        boolean blockingOccludes = blockingState.canOcclude() && blockingState.useShapeForLightOcclusion();
        if (!occludes && !blockingOccludes) {
            return lightBlock;
        }
        VoxelShape shape = occludes ? state.getOcclusionShape_(level, x, y, z) : Shapes.empty();
        VoxelShape blockingShape = blockingOccludes ? blockingState.getOcclusionShape_(level, bx, by, bz) : Shapes.empty();
        return Shapes.mergedFaceOccludes(shape, blockingShape, direction) ? 16 : lightBlock;
    }
}
