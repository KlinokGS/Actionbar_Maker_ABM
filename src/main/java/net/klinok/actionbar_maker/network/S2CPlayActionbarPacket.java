package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CPlayActionbarPacket {
    public final ActionbarDefinition definition;
    public final int fadeIn;
    public final int stay;
    public final int fadeOut;

    public S2CPlayActionbarPacket(ActionbarDefinition definition, int fadeIn, int stay, int fadeOut) {
        this.definition = definition;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public static void encode(S2CPlayActionbarPacket packet, FriendlyByteBuf buf) {
        ActionbarDefinition.write(buf, packet.definition);
        buf.writeInt(packet.fadeIn);
        buf.writeInt(packet.stay);
        buf.writeInt(packet.fadeOut);
    }

    public static S2CPlayActionbarPacket decode(FriendlyByteBuf buf) {
        return new S2CPlayActionbarPacket(ActionbarDefinition.read(buf), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(S2CPlayActionbarPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ActionbarMaker.LOGGER.debug("[ABM PACKET] S2CPlayActionbarPacket received on {}", context.getDirection().getReceptionSide());
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.play(packet.definition, packet.fadeIn, packet.stay, packet.fadeOut));
            } else {
                ActionbarMaker.LOGGER.warn("[ABM PACKET] S2CPlayActionbarPacket ignored because reception side is not CLIENT");
            }
        });
        context.setPacketHandled(true);
    }
}
