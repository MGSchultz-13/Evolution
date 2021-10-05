package tgw.evolution.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldLoadProgressScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tgw.evolution.util.ColorABGR;
import tgw.evolution.util.ColorARGB;

@Mixin(WorldLoadProgressScreen.class)
public abstract class WorldLoadProgressScreenMixin extends Screen {

    private static final int NULL_STATUS_COLOR = ColorABGR.pack(0, 0, 0, 0xFF);
    @Shadow
    @Final
    private static Object2IntMap<ChunkStatus> COLORS;
    private static Reference2IntOpenHashMap<ChunkStatus> STATUS_TO_COLOR_FAST;

    protected WorldLoadProgressScreenMixin(ITextComponent text) {
        super(text);
    }

    /**
     * @author MGSchultz
     */
    @Overwrite
    public static void renderChunks(MatrixStack matrices, TrackingChunkStatusListener tracker, int mapX, int mapY, int mapScale, int mapPadding) {
        if (STATUS_TO_COLOR_FAST == null) {
            STATUS_TO_COLOR_FAST = new Reference2IntOpenHashMap<>(COLORS.size());
            STATUS_TO_COLOR_FAST.put(null, NULL_STATUS_COLOR);
            COLORS.object2IntEntrySet().forEach(entry -> STATUS_TO_COLOR_FAST.put(entry.getKey(), ColorARGB.toABGR(entry.getIntValue(), 0xFF)));
        }
        int tileSize = mapScale + mapPadding;
        int centerSize = tracker.getFullDiameter();
        int size = tracker.getDiameter();
        if (mapPadding != 0) {
            int mapRenderCenterSize = centerSize * tileSize - mapPadding;
            int radius = mapRenderCenterSize / 2 + 1;
            fill(matrices, mapX - radius, mapY - radius, mapX - radius + 1, mapY + radius, 0xff00_11ff);
            fill(matrices, mapX + radius - 1, mapY - radius, mapX + radius, mapY + radius, 0xff00_11ff);
            fill(matrices, mapX - radius, mapY - radius, mapX + radius, mapY - radius + 1, 0xff00_11ff);
            fill(matrices, mapX - radius, mapY + radius - 1, mapX + radius, mapY + radius, 0xff00_11ff);
        }
        int mapRenderSize = size * tileSize - mapPadding;
        int mapStartX = mapX - mapRenderSize / 2;
        int mapStartY = mapY - mapRenderSize / 2;
        ChunkStatus prevStatus = null;
        int prevColor = NULL_STATUS_COLOR;
        for (int x = 0; x < size; x++) {
            int tileX = mapStartX + x * tileSize;
            for (int y = 0; y < size; y++) {
                int tileY = mapStartY + y * tileSize;
                ChunkStatus status = tracker.getStatus(x, y);
                int color;
                if (prevStatus == status) {
                    color = prevColor;
                }
                else {
                    color = STATUS_TO_COLOR_FAST.getInt(status);
                    prevStatus = status;
                    prevColor = color;
                }
                fill(matrices, tileX, tileY, tileX + mapScale, tileY + mapScale, color);
            }
        }
    }
}