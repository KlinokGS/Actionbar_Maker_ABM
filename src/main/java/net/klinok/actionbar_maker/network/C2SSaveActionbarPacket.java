package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.server.AbmStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSaveActionbarPacket {
    public final ActionbarDefinition definition;
    public final boolean template;

    public C2SSaveActionbarPacket(ActionbarDefinition definition, boolean template) {
        this.definition = definition;
        this.template = template;
    }

    public static void encode(C2SSaveActionbarPacket packet, FriendlyByteBuf buf) {
        ActionbarDefinition.write(buf, packet.definition);
        buf.writeBoolean(packet.template);
    }

    public static C2SSaveActionbarPacket decode(FriendlyByteBuf buf) {
        return new C2SSaveActionbarPacket(ActionbarDefinition.read(buf), buf.readBoolean());
    }

    public static void handle(C2SSaveActionbarPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            try {
                packet.definition.normalize();
                if (packet.template) {
                    AbmStorage.saveTemplate(packet.definition);
                    sender.sendSystemMessage(Component.translatable("actionbar_maker.command.template_saved", packet.definition.name));
                } else {
                    AbmStorage.saveActionbar(sender.server, packet.definition);
                    sender.sendSystemMessage(Component.translatable("actionbar_maker.command.actionbar_saved", packet.definition.name));
                }
            } catch (Exception exception) {
                sender.sendSystemMessage(Component.translatable("actionbar_maker.command.save_error", exception.getMessage()));
            }
        });
        ctx.setPacketHandled(true);
    }
}
