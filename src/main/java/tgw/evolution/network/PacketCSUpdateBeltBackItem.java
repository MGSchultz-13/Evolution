package tgw.evolution.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import tgw.evolution.init.EvolutionNetwork;

import java.util.function.Supplier;

public class PacketCSUpdateBeltBackItem implements IPacket {

    private final boolean back;
    private final ItemStack stack;

    public PacketCSUpdateBeltBackItem(ItemStack stack, boolean back) {
        this.stack = stack;
        this.back = back;
    }

    public static PacketCSUpdateBeltBackItem decode(FriendlyByteBuf buffer) {
        return new PacketCSUpdateBeltBackItem(buffer.readItem(), buffer.readBoolean());
    }

    public static void encode(PacketCSUpdateBeltBackItem packet, FriendlyByteBuf buffer) {
        buffer.writeItemStack(packet.stack, false);
        buffer.writeBoolean(packet.back);
    }

    public static void handle(PacketCSUpdateBeltBackItem packet, Supplier<NetworkEvent.Context> context) {
        if (IPacket.checkSide(packet, context)) {
            context.get().enqueueWork(() -> {
                ServerPlayer player = context.get().getSender();
                EvolutionNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player),
                                               new PacketSCUpdateBeltBackItem(player.getId(), packet.back, packet.stack));
            });
            context.get().setPacketHandled(true);
        }
    }

    @Override
    public LogicalSide getDestinationSide() {
        return LogicalSide.SERVER;
    }
}
