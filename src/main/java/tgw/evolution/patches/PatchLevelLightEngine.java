package tgw.evolution.patches;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import tgw.evolution.util.constants.Lightlayer;
import tgw.evolution.world.lighting.StarLightInterface;

public interface PatchLevelLightEngine {

    default void clientChunkLoad(final ChunkPos pos, final LevelChunk chunk) {
        throw new AbstractMethodError();
    }

    default void clientRemoveLightData(int chunkX, int chunkZ) {
        throw new AbstractMethodError();
    }

    default void clientUpdateLight(LightLayer lightType, int secX, int secY, int secZ, DataLayer nibble, boolean trustEdges) {
        throw new AbstractMethodError();
    }

    default int getColoredBrightness(@Lightlayer int layer, long pos) {
        throw new AbstractMethodError();
    }

    default StarLightInterface getLightEngine() {
        throw new AbstractMethodError();
    }

    default int getRawBrightness_(long pos, int i) {
        throw new AbstractMethodError();
    }
}
