package tgw.evolution.mixin;

import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import tgw.evolution.patches.obj.PixelDirection;
import tgw.evolution.util.collection.lists.OArrayList;
import tgw.evolution.util.collection.lists.OList;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(ItemModelGenerator.class)
public abstract class MixinItemModelGenerator {

    @Unique
    private static boolean doesPixelHaveEdge(TextureAtlasSprite sprite, int[] frames, int x, int y, PixelDirection direction) {
        int x1 = x + direction.getOffsetX();
        int y1 = y + direction.getOffsetY();
        if (isPixelOutsideSprite(sprite, x1, y1)) {
            return true;
        }
        for (int frame : frames) {
            if (!isPixelTransparent(sprite, frame, x, y) && isPixelTransparent(sprite, frame, x1, y1)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean isPixelAlwaysTransparent(TextureAtlasSprite sprite, int[] frames, int x, int y) {
        for (int frame : frames) {
            if (!isPixelTransparent(sprite, frame, x, y)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private static boolean isPixelOutsideSprite(TextureAtlasSprite sprite, int x, int y) {
        return x < 0 || y < 0 || x >= sprite.getWidth() || y >= sprite.getHeight();
    }

    @Unique
    private static boolean isPixelTransparent(TextureAtlasSprite sprite, int frame, int x, int y) {
        return isPixelOutsideSprite(sprite, x, y) || sprite.isTransparent(frame, x, y);
    }

    /**
     * @author TheGreatWolf
     * @reason Improve method
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    @Overwrite
    private List<BlockElement> processFrames(int tintIndex, String texture, TextureAtlasSprite sprite) {
        OList<BlockElement> elements = new OArrayList<>();
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        float xFactor = width / 16.0F;
        float yFactor = height / 16.0F;
        int[] frames = sprite.getUniqueFrames().toArray();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                if (!isPixelAlwaysTransparent(sprite, frames, x, y)) {
                    Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
                    BlockElementFace face = new BlockElementFace(null, tintIndex, texture, new BlockFaceUV(
                            new float[]{x / xFactor, y / yFactor, (x + 1) / xFactor, (y + 1) / yFactor}, 0));
                    BlockElementFace flippedFace = new BlockElementFace(null, tintIndex, texture, new BlockFaceUV(
                            new float[]{(x + 1) / xFactor, y / yFactor, x / xFactor, (y + 1) / yFactor}, 0));
                    faces.put(Direction.SOUTH, face);
                    faces.put(Direction.NORTH, flippedFace);
                    for (PixelDirection pixelDirection : PixelDirection.VALUES) {
                        if (doesPixelHaveEdge(sprite, frames, x, y, pixelDirection)) {
                            faces.put(pixelDirection.getDirection(), pixelDirection.isVertical() ? face : flippedFace);
                        }
                    }
                    elements.add(new BlockElement(new Vector3f(x / xFactor, (height - (y + 1)) / yFactor, 7.5F),
                                                  new Vector3f((x + 1) / xFactor, (height - y) / yFactor, 8.5F), faces, null, true));
                }
            }
        }
        return elements;
    }
}
