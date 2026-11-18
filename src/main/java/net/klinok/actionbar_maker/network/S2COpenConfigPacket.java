package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2COpenConfigPacket {

    public S2COpenConfigPacket() {
    }

    public static void encode(S2COpenConfigPacket packet, FriendlyByteBuf buf) {
    }

    public static S2COpenConfigPacket decode(FriendlyByteBuf buf) {
        return new S2COpenConfigPacket();
    }

    public static void handle(S2COpenConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ActionbarMaker.LOGGER.info("[ABM PACKET] S2COpenConfigPacket received on {}", context.getDirection().getReceptionSide());
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openConfig());
            } else {
                ActionbarMaker.LOGGER.warn("[ABM PACKET] S2COpenConfigPacket ignored because reception side is not CLIENT");
            }
        });
        context.setPacketHandled(true);
    }
}
