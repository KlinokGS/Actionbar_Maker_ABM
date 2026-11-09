package net.klinok.actionbar_maker.network;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.klinok.actionbar_maker.data.ActionbarSequence;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;
public class S2CPlaySequencePacket {
    public final ActionbarSequence sequence;
    public S2CPlaySequencePacket(ActionbarSequence sequence) { this.sequence = sequence; }
    public static void encode(S2CPlaySequencePacket p, FriendlyByteBuf b) { ActionbarSequence.write(b,p.sequence); }
    public static S2CPlaySequencePacket decode(FriendlyByteBuf b) { return new S2CPlaySequencePacket(ActionbarSequence.read(b)); }
    public static void handle(S2CPlaySequencePacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx=c.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.playSequence(p.sequence))); ctx.setPacketHandled(true);
    }
}
