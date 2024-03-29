package tgw.evolution.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import tgw.evolution.patches.PatchStorage;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkCache.Storage.class)
public abstract class MixinClientChunkCache_Storage implements PatchStorage {

    @Shadow @Final public int chunkRadius;
    @Shadow @Final public AtomicReferenceArray<LevelChunk> chunks;
    @Unique int cameraChunkCount;
    @Nullable @Unique AtomicReferenceArray<LevelChunk> cameraChunks;
    @Unique volatile int cameraViewCenterX;
    @Unique volatile int cameraViewCenterZ;
    @Shadow int chunkCount;
    @Shadow(aliases = "this$0") @Final ClientChunkCache field_16254;
    @Shadow @Final private int viewRange;

    @Override
    public LevelChunk cameraReplace(int index, LevelChunk oldChunk, @Nullable LevelChunk newChunk) {
        if (this.cameraChunks == null) {
            this.cameraChunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
        }
        if (this.cameraChunks.compareAndSet(index, oldChunk, newChunk) && newChunk == null) {
            this.chunkCount--;
            this.cameraChunkCount--;
            if (this.cameraChunkCount == 0) {
                this.cameraChunks = null;
            }
        }
        this.field_16254.level.unload(oldChunk);
        return oldChunk;
    }

    @Override
    public void cameraReplace(int index, LevelChunk chunk) {
        if (this.cameraChunks == null) {
            this.cameraChunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
        }
        LevelChunk oldChunk = this.cameraChunks.getAndSet(index, chunk);
        if (oldChunk != null) {
            this.cameraChunkCount--;
            if (!this.inRange(oldChunk.getPos().x, oldChunk.getPos().z)) {
                this.field_16254.level.unload(oldChunk);
            }
        }
//        if (chunk != null) {
//            this.cameraChunkCount++;
//        }
    }

    @Override
    public int getCamViewCenterX() {
        return this.cameraViewCenterX;
    }

    @Override
    public int getCamViewCenterZ() {
        return this.cameraViewCenterZ;
    }

    @Override
    public @Nullable LevelChunk getCameraChunk(int index) {
        if (this.cameraChunks != null) {
            return this.cameraChunks.get(index);
        }
        return null;
    }

    @Override
    public int getCameraChunksLength() {
        return this.cameraChunks == null ? 0 : this.cameraChunks.length();
    }

    @Override
    public int getCameraIndex(int x, int z) {
        return Math.floorMod(z, this.viewRange) * this.viewRange + Math.floorMod(x, this.viewRange);
    }

    @Override
    public boolean inCameraRange(int x, int z) {
        return Math.abs(x - this.cameraViewCenterX) <= this.chunkRadius && Math.abs(z - this.cameraViewCenterZ) <= this.chunkRadius;
    }

    @Shadow
    public abstract boolean inRange(int pX, int pZ);

    /**
     * @author TheGreatWolf
     * @reason Prevent player from unloading camera loaded chunks
     */
    @Overwrite
    public void replace(int index, @Nullable LevelChunk newChunk) {
        LevelChunk oldChunk = this.chunks.getAndSet(index, newChunk);
        if (oldChunk != null) {
            --this.chunkCount;
            if (!this.inCameraRange(oldChunk.getPos().x, oldChunk.getPos().z)) {
                this.field_16254.level.unload(oldChunk);
            }
        }
        //noinspection VariableNotUsedInsideIf
        if (newChunk != null) {
            ++this.chunkCount;
        }
    }

    @Override
    public void setCamViewCenter(int x, int z) {
        this.cameraViewCenterX = x;
        this.cameraViewCenterZ = z;
    }
}
