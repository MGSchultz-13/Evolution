package tgw.evolution.util.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tgw.evolution.patches.IAABBPatch;

@SuppressWarnings("EqualsAndHashcode")
public class AABBMutable extends AABB {

    public AABBMutable() {
        this(0, 0, 0, 0, 0, 0);
    }

    public AABBMutable(AABB bb) {
        this(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    public AABBMutable(Vec3 start, Vec3 end) {
        this(start.x, start.y, start.z, end.x, end.y, end.z);
    }

    public AABBMutable(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    public static AABBMutable block(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABBMutable(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    public AABBMutable deflateMutable(double value) {
        return this.inflateMutable(-value);
    }

    @Override
    public int hashCode() {
        throw new IllegalStateException("Cannot hash mutable object");
    }

    public AABBMutable inflateMutable(double value) {
        return this.inflateMutable(value, value, value);
    }

    public AABBMutable inflateMutable(double x, double y, double z) {
        return this.set(this.minX - x, this.minY - y, this.minZ - z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    @Override
    public AABBMutable minmax(AABB other) {
        double x0 = Math.min(this.minX, other.minX);
        double y0 = Math.min(this.minY, other.minY);
        double z0 = Math.min(this.minZ, other.minZ);
        double x1 = Math.max(this.maxX, other.maxX);
        double y1 = Math.max(this.maxY, other.maxY);
        double z1 = Math.max(this.maxZ, other.maxZ);
        return this.set(x0, y0, z0, x1, y1, z1);
    }

    public AABBMutable set(double x1, double y1, double z1, double x2, double y2, double z2) {
        ((IAABBPatch) this).setMinX(Math.min(x1, x2));
        ((IAABBPatch) this).setMinY(Math.min(y1, y2));
        ((IAABBPatch) this).setMinZ(Math.min(z1, z2));
        ((IAABBPatch) this).setMaxX(Math.max(x1, x2));
        ((IAABBPatch) this).setMaxY(Math.max(y1, y2));
        ((IAABBPatch) this).setMaxZ(Math.max(z1, z2));
        return this;
    }

    public AABBMutable set(AABB aabb) {
        return this.set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public AABBMutable setX(double x1, double x2) {
        ((IAABBPatch) this).setMinX(Math.min(x1, x2));
        ((IAABBPatch) this).setMaxX(Math.max(x1, x2));
        return this;
    }
}
