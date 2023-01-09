package tgw.evolution.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import tgw.evolution.capabilities.SerializableCapabilityProvider;
import tgw.evolution.capabilities.chunkstorage.CapabilityChunkStorage;
import tgw.evolution.capabilities.chunkstorage.ChunkStorage;
import tgw.evolution.entities.misc.EntityPlayerCorpse;

public class WorldEvents {

    @Nullable
    public static BlockPos findSpawn(ServerLevel level, int posX, int posZ) {
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
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int k = i + 1; k >= level.getMinBuildHeight(); --k) {
            mutablePos.set(posX, k, posZ);
            BlockState blockstate = level.getBlockState(mutablePos);
            if (!blockstate.getFluidState().isEmpty()) {
                break;
            }
            if (Block.isFaceFull(blockstate.getCollisionShape(level, mutablePos), Direction.UP)) {
                return mutablePos.above().immutable();
            }
        }
        return null;
    }

    @SubscribeEvent
    public void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getLevel().isClientSide) {
            event.addCapability(CapabilityChunkStorage.LOC,
                                new SerializableCapabilityProvider<>(CapabilityChunkStorage.INSTANCE, new ChunkStorage()));
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerLevel level) {
            MinecraftServer server = level.getServer();
            GameRules gameRules = level.getGameRules();
            if (server.getWorldData().getGameType() != GameType.CREATIVE) {
                gameRules.getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(true, server);
            }
            gameRules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
            gameRules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
            gameRules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        }
    }

    @SubscribeEvent
    public void serverStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        EntityPlayerCorpse.setProfileCache(server.getProfileCache());
        EntityPlayerCorpse.setSessionService(server.getSessionService());
    }
}
