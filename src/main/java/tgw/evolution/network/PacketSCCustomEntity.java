package tgw.evolution.network;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import tgw.evolution.entities.IEntitySpawnData;
import tgw.evolution.init.EvolutionNetwork;

import java.util.UUID;

public class PacketSCCustomEntity<T extends Entity & IEntitySpawnData> implements Packet<ClientGamePacketListener> {

    public final IEntitySpawnData.EntityData<T> data;
    public final int id;
    public final EntityType<T> type;
    public final UUID uuid;
    public final double vx;
    public final double vy;
    public final double vz;
    public final double x;
    public final float xRot;
    public final double y;
    public final float yRot;
    public final double z;

    public PacketSCCustomEntity(T entity) {
        this.id = entity.getId();
        this.uuid = entity.getUUID();
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        this.xRot = entity.getXRot();
        this.yRot = entity.getYRot();
        this.type = (EntityType<T>) entity.getType();
        Vec3 velocity = entity.getDeltaMovement();
        this.vx = velocity.x;
        this.vy = velocity.y;
        this.vz = velocity.z;
        this.data = entity.getSpawnData();
    }

    public PacketSCCustomEntity(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.type = (EntityType<T>) Registry.ENTITY_TYPE.byId(buf.readVarInt());
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.xRot = buf.readByte() * 360.0f / 256;
        this.yRot = buf.readByte() * 360.0f / 256;
        this.vx = buf.readShort() / 8_000.0;
        this.vy = buf.readShort() / 8_000.0;
        this.vz = buf.readShort() / 8_000.0;
        this.data = EvolutionNetwork.readData(buf.readVarInt(), buf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleCustomEntity(this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(Registry.ENTITY_TYPE.getId(this.type));
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(Mth.floor(this.xRot * 256.0F / 360.0F));
        buf.writeByte(Mth.floor(this.yRot * 256.0F / 360.0F));
        buf.writeShort((int) (Mth.clamp(this.vx, -4, 4) * 8_000));
        buf.writeShort((int) (Mth.clamp(this.vy, -4, 4) * 8_000));
        buf.writeShort((int) (Mth.clamp(this.vz, -4, 4) * 8_000));
        buf.writeVarInt(EvolutionNetwork.getId(this.data));
        this.data.writeToBuffer(buf);
    }
}
