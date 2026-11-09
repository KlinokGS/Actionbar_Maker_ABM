package net.klinok.actionbar_maker.network;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;
public class S2CStopActionbarPacket {
    public static void encode(S2CStopActionbarPacket p,FriendlyByteBuf b) {}
    public static S2CStopActionbarPacket decode(FriendlyByteBuf b) { return new S2CStopActionbarPacket(); }
    public static void handle(S2CStopActionbarPacket p,Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx=c.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandlers::stop)); ctx.setPacketHandled(true);
    }
}
