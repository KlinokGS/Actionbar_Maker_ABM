package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class S2COpenEditorPacket {
    public final ActionbarDefinition definition;
    public final List<ActionbarDefinition> templates;

    public S2COpenEditorPacket(ActionbarDefinition definition, List<ActionbarDefinition> templates) {
        this.definition = definition;
        this.templates = templates;
    }

    public static void encode(S2COpenEditorPacket packet, FriendlyByteBuf buf) {
        ActionbarDefinition.write(buf, packet.definition);
        S2COpenManagerPacket.writeDefinitions(buf, packet.templates);
    }

    public static S2COpenEditorPacket decode(FriendlyByteBuf buf) {
        return new S2COpenEditorPacket(ActionbarDefinition.read(buf), S2COpenManagerPacket.readDefinitions(buf));
    }

    public static void handle(S2COpenEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ActionbarMaker.LOGGER.info("[ABM PACKET] S2COpenEditorPacket received on {}", context.getDirection().getReceptionSide());
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openEditor(packet.definition, packet.templates));
            } else {
                ActionbarMaker.LOGGER.warn("[ABM PACKET] S2COpenEditorPacket ignored because reception side is not CLIENT");
            }
        });
        context.setPacketHandled(true);
    }
}
