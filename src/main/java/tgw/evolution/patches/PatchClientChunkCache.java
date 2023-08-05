package tgw.evolution.patches;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.patches.obj.IBlockEntityTagOutput;

import java.util.function.Consumer;

public interface PatchClientChunkCache {

    default @Nullable LevelChunk replaceWithPacketData_(int x,
                                                        int z,
                                                        FriendlyByteBuf buf,
                                                        CompoundTag tag,
                                                        Consumer<IBlockEntityTagOutput> consumer) {
        throw new AbstractMethodError();
    }

    default void updateCameraViewCenter(int x, int z) {
        throw new AbstractMethodError();
    }
}
