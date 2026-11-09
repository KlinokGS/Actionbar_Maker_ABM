package net.klinok.actionbar_maker.network;
import net.klinok.actionbar_maker.data.ActionbarSequence;
import net.klinok.actionbar_maker.server.AbmStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class C2SSaveSequencePacket {
    public final ActionbarSequence sequence;
    public C2SSaveSequencePacket(ActionbarSequence sequence) { this.sequence=sequence; }
    public static void encode(C2SSaveSequencePacket p,FriendlyByteBuf b) { ActionbarSequence.write(b,p.sequence); }
    public static C2SSaveSequencePacket decode(FriendlyByteBuf b) { return new C2SSaveSequencePacket(ActionbarSequence.read(b)); }
    public static void handle(C2SSaveSequencePacket p,Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx=c.get(); ctx.enqueueWork(() -> {
            ServerPlayer player=ctx.getSender(); if(player==null) return;
            try {
                p.sequence.normalize();
                if(p.sequence.entries.isEmpty()) throw new IllegalArgumentException("Group must contain at least one actionbar");
                AbmStorage.saveSequence(player.server,p.sequence);
                player.sendSystemMessage(Component.literal("Actionbar group saved: "+p.sequence.name));
            } catch(Exception ex) { player.sendSystemMessage(Component.literal("Could not save actionbar group: "+ex.getMessage())); }
        }); ctx.setPacketHandled(true);
    }
}
