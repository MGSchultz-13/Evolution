package tgw.evolution.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import tgw.evolution.init.EvolutionDamage;

import java.util.function.Supplier;

public class PacketCSImpactDamage implements IPacket {

    private final float damage;

    public PacketCSImpactDamage(float damage) {
        this.damage = damage;
    }

    public static PacketCSImpactDamage decode(FriendlyByteBuf buffer) {
        return new PacketCSImpactDamage(buffer.readFloat());
    }

    public static void encode(PacketCSImpactDamage packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.damage);
    }

    public static void handle(PacketCSImpactDamage packet, Supplier<NetworkEvent.Context> context) {
        if (IPacket.checkSide(packet, context)) {
            context.get().enqueueWork(() -> context.get().getSender().hurt(EvolutionDamage.WALL_IMPACT, packet.damage));
            context.get().setPacketHandled(true);
        }
    }

    @Override
    public LogicalSide getDestinationSide() {
        return LogicalSide.SERVER;
    }
}
