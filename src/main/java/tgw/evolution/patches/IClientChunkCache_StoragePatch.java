package tgw.evolution.patches;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.world.level.chunk.LevelChunk;

public interface IClientChunkCache_StoragePatch {

    @CanIgnoreReturnValue
    LevelChunk cameraReplace(int index, LevelChunk oldChunk, LevelChunk newChunk);

    void cameraReplace(int index, LevelChunk chunk);

    int getCamViewCenterX();

    int getCamViewCenterZ();

    LevelChunk getCameraChunk(int index);

    int getCameraChunksLength();

    int getCameraIndex(int x, int z);

    boolean inCameraRange(int x, int z);

    void setCamViewCenter(int x, int z);
}