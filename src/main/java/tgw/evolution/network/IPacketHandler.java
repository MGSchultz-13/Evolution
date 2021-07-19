package tgw.evolution.network;

import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public interface IPacketHandler {

    void handlePlaySoundEntityEmitted(PacketSCPlaySoundEntityEmitted packet, Supplier<NetworkEvent.Context> context);
}
