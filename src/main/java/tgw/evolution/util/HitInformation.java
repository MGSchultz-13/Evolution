package tgw.evolution.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import tgw.evolution.Evolution;
import tgw.evolution.events.ClientEvents;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.items.IMelee;
import tgw.evolution.network.PacketCSHitInformation;
import tgw.evolution.network.PacketCSSpecialHit;
import tgw.evolution.patches.IEntityPatch;
import tgw.evolution.util.collection.I2OMap;
import tgw.evolution.util.collection.I2OOpenHashMap;
import tgw.evolution.util.hitbox.Hitbox;
import tgw.evolution.util.hitbox.HitboxEntity;
import tgw.evolution.util.hitbox.HitboxType;
import tgw.evolution.util.hitbox.Matrix4d;
import tgw.evolution.util.math.ClipContextMutable;
import tgw.evolution.util.math.Vec3d;

import java.util.EnumSet;
import java.util.Set;

public class HitInformation {

    private final ClipContextMutable clipContext = new ClipContextMutable(Vec3.ZERO, Vec3.ZERO, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                                                                          null);
    private final I2OMap<Set<HitboxType>> data = new I2OOpenHashMap<>();
    private final Vec3d[] vertices = new Vec3d[8];
    private final Vec3d[] verticesInHBVS = new Vec3d[8];
    private final Vec3d[] verticesInHBVSPartial = new Vec3d[8];
    private @Nullable Hitbox collider;
    private @Nullable Matrix4d colliderTransform;
    private @Nullable HitboxEntity<?> hitboxes;
    private double hitterX;
    private double hitterY;
    private double hitterZ;
    private boolean prepared;
    private boolean preparedInHBVS;
    private @Nullable Matrix4d transform;
    private double victimX;
    private double victimY;
    private double victimZ;

    public HitInformation() {
        for (int i = 0, l = this.vertices.length; i < l; i++) {
            //noinspection ObjectAllocationInLoop
            this.vertices[i] = new Vec3d(Vec3d.NULL);
        }
        for (int i = 0, l = this.verticesInHBVSPartial.length; i < l; i++) {
            //noinspection ObjectAllocationInLoop
            this.verticesInHBVSPartial[i] = new Vec3d(Vec3d.NULL);
        }
        for (int i = 0, l = this.verticesInHBVS.length; i < l; i++) {
            //noinspection ObjectAllocationInLoop
            this.verticesInHBVS[i] = new Vec3d(Vec3d.NULL);
        }
    }

    private static double getByIndex(@Range(from = 0, to = 7) int index, Hitbox collider, Direction.Axis axis) {
        return switch (axis) {
            case X -> index < 4 ? collider.minX() : collider.maxX();
            case Y -> index % 4 < 2 ? collider.minY() : collider.maxY();
            case Z -> index % 2 == 0 ? collider.minZ() : collider.maxZ();
        };
    }

    public void addHitbox(Entity entity, HitboxType hitbox) {
        ClientEvents.getInstance().getRenderer().updateHitmarkers(false);
        Evolution.info("Collided with {} on {}", entity, hitbox);
        Set<HitboxType> set = this.data.get(entity.getId());
        if (set == null) {
            EvolutionNetwork.INSTANCE.sendToServer(new PacketCSHitInformation(entity));
            set = EnumSet.noneOf(HitboxType.class);
            this.data.put(entity.getId(), set);
        }
        set.add(hitbox);
    }

    public boolean areAllChecked(Entity entity) {
        Set<HitboxType> set = this.data.get(entity.getId());
        if (set == null) {
            return false;
        }
        HitboxEntity<? extends Entity> hitboxes = ((IEntityPatch) entity).getHitboxes();
        if (hitboxes == null) {
            //If the entity does not have hitboxes, its only hitbox should be HitboxType.ALL
            return set.size() >= 1;
        }
        return set.size() >= hitboxes.getBoxes().size();
    }

    public void clear() {
        this.data.clear();
    }

    public void clearMemory() {
        this.data.reset();
    }

    public boolean contains(Entity entity, HitboxType hitbox) {
        Set<HitboxType> set = this.data.get(entity.getId());
        if (set == null) {
            return false;
        }
        return set.contains(hitbox);
    }

    public ClipContext getClipContext(@Range(from = 0, to = 11) int edge) {
        this.clipContext.set(this.getOrMakeEdge(edge, true), this.getOrMakeEdge(edge, false), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                             null);
        return this.clipContext;
    }

    public Vec3d getOrMakeEdge(@Range(from = 0, to = 11) int edge, boolean start) {
        @Range(from = 0, to = 7) int index;
        if (start) {
            switch (edge) {
                case 0, 1, 2 -> index = 0;
                case 3, 4, 5 -> index = 3;
                case 6, 7, 8 -> index = 5;
                case 9, 10, 11 -> index = 6;
                default -> throw new IndexOutOfBoundsException("Edge is out of bounds: " + edge);
            }
        }
        else {
            switch (edge) {
                case 0, 3, 6 -> index = 1;
                case 1, 4, 9 -> index = 2;
                case 2, 7, 10 -> index = 4;
                case 5, 8, 11 -> index = 7;
                default -> throw new IndexOutOfBoundsException("Edge is out of bounds: " + edge);
            }
        }
        return this.getOrMakeVertex(index);
    }

    public Vec3d getOrMakeEdgeInHBVS(@Range(from = 0, to = 11) int edge, boolean start, Matrix4d transform) {
        @Range(from = 0, to = 7) int index;
        if (start) {
            switch (edge) {
                case 0, 1, 2 -> index = 0;
                case 3, 4, 5 -> index = 3;
                case 6, 7, 8 -> index = 5;
                case 9, 10, 11 -> index = 6;
                default -> throw new IndexOutOfBoundsException("Edge is out of bounds: " + edge);
            }
        }
        else {
            switch (edge) {
                case 0, 3, 6 -> index = 1;
                case 1, 4, 9 -> index = 2;
                case 2, 7, 10 -> index = 4;
                case 5, 8, 11 -> index = 7;
                default -> throw new IndexOutOfBoundsException("Edge is out of bounds: " + edge);
            }
        }
        return this.getOrMakeVertexInHBVS(index, transform);
    }

    public Vec3d getOrMakeVertex(@Range(from = 0, to = 7) int index) {
        if (!this.prepared) {
            throw new IllegalStateException("Should prepare first!");
        }
        Vec3d vertex = this.vertices[index];
        if (!vertex.isNull()) {
            return vertex;
        }
        assert this.collider != null;
        double x = getByIndex(index, this.collider, Direction.Axis.X);
        double y = getByIndex(index, this.collider, Direction.Axis.Y);
        double z = getByIndex(index, this.collider, Direction.Axis.Z);
        assert this.colliderTransform != null;
        double x0 = this.colliderTransform.transformX(x, y, z);
        double y0 = this.colliderTransform.transformY(x, y, z);
        double z0 = this.colliderTransform.transformZ(x, y, z);
        assert this.transform != null;
        return vertex.set(this.transform.transformX(x0, y0, z0) + this.hitterX,
                          this.transform.transformY(x0, y0, z0) + this.hitterY,
                          this.transform.transformZ(x0, y0, z0) + this.hitterZ);
    }

    public Vec3d getOrMakeVertexInHBVS(@Range(from = 0, to = 7) int index, Matrix4d transform) {
        if (!this.prepared) {
            throw new IllegalStateException("Should prepare first!");
        }
        if (!this.preparedInHBVS) {
            throw new IllegalStateException("Should prepare in Hitbox Vector Space first!");
        }
        Vec3d vertexHBVS = this.verticesInHBVS[index];
        if (!vertexHBVS.isNull()) {
            return vertexHBVS;
        }
        Vec3d partialVertex = this.verticesInHBVSPartial[index];
        if (partialVertex.isNull()) {
            partialVertex.set(this.getOrMakeVertex(index)).subMutable(this.victimX, this.victimY, this.victimZ);
            assert this.hitboxes != null;
            this.hitboxes.untransform(partialVertex);
        }
        vertexHBVS.set(partialVertex);
        return transform.untransform(vertexHBVS);
    }

    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    public void prepare(Hitbox collider, Matrix4d colliderTransform, Matrix4d transform, double hitterX, double hitterY, double hitterZ) {
        this.collider = collider;
        this.colliderTransform = colliderTransform;
        this.transform = transform;
        this.hitterX = hitterX;
        this.hitterY = hitterY;
        this.hitterZ = hitterZ;
        for (Vec3d vertex : this.vertices) {
            vertex.set(Vec3d.NULL);
        }
        this.prepared = true;
    }

    public void prepareInHBVS(HitboxEntity<?> hitboxes, double victimX, double victimY, double victimZ) {
        this.hitboxes = hitboxes;
        this.victimX = victimX;
        this.victimY = victimY;
        this.victimZ = victimZ;
        for (Vec3d vertex : this.verticesInHBVSPartial) {
            vertex.set(Vec3d.NULL);
        }
        for (Vec3d vertex : this.verticesInHBVS) {
            vertex.set(Vec3d.NULL);
        }
        this.preparedInHBVS = true;
    }

    public void release() {
        this.prepared = false;
        this.collider = null;
        this.colliderTransform = null;
        this.transform = null;
    }

    public void releaseInHBVS() {
        this.preparedInHBVS = false;
        this.hitboxes = null;
    }

    public void sendHits(IMelee.IAttackType type) {
        for (Int2ObjectMap.Entry<Set<HitboxType>> entry : this.data.int2ObjectEntrySet()) {
            //noinspection ObjectAllocationInLoop
            EvolutionNetwork.INSTANCE.sendToServer(
                    new PacketCSSpecialHit(entry.getIntKey(), type, entry.getValue().toArray(new HitboxType[this.data.size()])));
        }
    }

    public void softRelease() {
        for (Vec3d vertexInHBVS : this.verticesInHBVS) {
            vertexInHBVS.set(Vec3d.NULL);
        }
    }
}
