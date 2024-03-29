package tgw.evolution.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tgw.evolution.Evolution;
import tgw.evolution.patches.PatchBiomeManager;

@Mixin(BiomeManager.class)
public abstract class MixinBiomeManager implements PatchBiomeManager {

    @Shadow @Final private long biomeZoomSeed;
    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;

    @Shadow
    private static double getFiddledDistance(long pSeed, int pX, int pY, int pZ, double pXNoise, double pYNoise, double pZNoise) {
        throw new AbstractMethodError();
    }

    /**
     * @author TheGreatWolf
     * @reason Use non-BlockPos version
     */
    @Overwrite
    public Holder<Biome> getBiome(BlockPos pos) {
        Evolution.deprecatedMethod();
        return this.getBiome_(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Holder<Biome> getBiome_(int x, int y, int z) {
        int x0 = x - 2;
        int y0 = y - 2;
        int z0 = z - 2;
        int l = x0 >> 2;
        int i1 = y0 >> 2;
        int j1 = z0 >> 2;
        double d0 = (x0 & 3) / 4.0;
        double d1 = (y0 & 3) / 4.0;
        double d2 = (z0 & 3) / 4.0;
        int k1 = 0;
        double d3 = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 8; ++i) {
            boolean flag = (i & 4) == 0;
            boolean flag1 = (i & 2) == 0;
            boolean flag2 = (i & 1) == 0;
            int i2 = flag ? l : l + 1;
            int j2 = flag1 ? i1 : i1 + 1;
            int k2 = flag2 ? j1 : j1 + 1;
            double d4 = flag ? d0 : d0 - 1.0;
            double d5 = flag1 ? d1 : d1 - 1.0;
            double d6 = flag2 ? d2 : d2 - 1.0;
            double d7 = getFiddledDistance(this.biomeZoomSeed, i2, j2, k2, d4, d5, d6);
            if (d3 > d7) {
                k1 = i;
                d3 = d7;
            }
        }
        int qx = (k1 & 4) == 0 ? l : l + 1;
        int qy = (k1 & 2) == 0 ? i1 : i1 + 1;
        int qz = (k1 & 1) == 0 ? j1 : j1 + 1;
        return this.noiseBiomeSource.getNoiseBiome(qx, qy, qz);
    }
}
