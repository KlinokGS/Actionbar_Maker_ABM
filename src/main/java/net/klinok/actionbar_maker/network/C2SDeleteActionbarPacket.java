package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.server.AbmStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SDeleteActionbarPacket {
    public final String name;

    public C2SDeleteActionbarPacket(String name) {
        this.name = name;
    }

    public static void encode(C2SDeleteActionbarPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name == null ? "" : packet.name, 128);
    }

    public static C2SDeleteActionbarPacket decode(FriendlyByteBuf buf) {
        return new C2SDeleteActionbarPacket(buf.readUtf(128));
    }

    public static void handle(C2SDeleteActionbarPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            try {
                boolean deleted = AbmStorage.deleteActionbar(sender.server, packet.name);
                sender.sendSystemMessage((deleted ? Component.translatable("actionbar_maker.command.deleted", packet.name) : Component.translatable("actionbar_maker.command.not_found", packet.name)));
            } catch (Exception exception) {
                sender.sendSystemMessage(Component.translatable("actionbar_maker.command.delete_error", exception.getMessage()));
            }
        });
        ctx.setPacketHandled(true);
    }
}
