package tgw.evolution.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import tgw.evolution.Evolution;

import java.util.function.Supplier;

public class PacketSCUpdateCameraTilt implements IPacket {

    private final float attackedAtYaw;

    private PacketSCUpdateCameraTilt(float attackedAtYaw) {
        this.attackedAtYaw = attackedAtYaw;
    }

    public PacketSCUpdateCameraTilt(PlayerEntity player) {
        this.attackedAtYaw = player.attackedAtYaw;
    }

    public static PacketSCUpdateCameraTilt decode(PacketBuffer buffer) {
        return new PacketSCUpdateCameraTilt(buffer.readFloat());
    }

    public static void encode(PacketSCUpdateCameraTilt packet, PacketBuffer buffer) {
        buffer.writeFloat(packet.attackedAtYaw);
    }

    public static void handle(PacketSCUpdateCameraTilt packet, Supplier<NetworkEvent.Context> context) {
        if (IPacket.checkSide(packet, context)) {
            context.get().enqueueWork(() -> Evolution.PROXY.getClientPlayer().attackedAtYaw = packet.attackedAtYaw);
            context.get().setPacketHandled(true);
        }
    }

    @Override
    public LogicalSide getDestinationSide() {
        return LogicalSide.CLIENT;
    }
}
