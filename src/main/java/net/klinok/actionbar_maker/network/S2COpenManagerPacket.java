package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2COpenManagerPacket {
    public final List<ActionbarDefinition> actionbars;
    public final List<ActionbarDefinition> templates;

    public S2COpenManagerPacket(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates) {
        this.actionbars = actionbars;
        this.templates = templates;
    }

    public static void encode(S2COpenManagerPacket packet, FriendlyByteBuf buf) {
        writeDefinitions(buf, packet.actionbars);
        writeDefinitions(buf, packet.templates);
    }

    public static S2COpenManagerPacket decode(FriendlyByteBuf buf) {
        return new S2COpenManagerPacket(readDefinitions(buf), readDefinitions(buf));
    }

    public static void handle(S2COpenManagerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ActionbarMaker.LOGGER.info("[ABM PACKET] S2COpenManagerPacket received on {}", context.getDirection().getReceptionSide());
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openManager(packet.actionbars, packet.templates));
            } else {
                ActionbarMaker.LOGGER.warn("[ABM PACKET] S2COpenManagerPacket ignored because reception side is not CLIENT");
            }
        });
        context.setPacketHandled(true);
    }

    static void writeDefinitions(FriendlyByteBuf buf, List<ActionbarDefinition> definitions) {
        buf.writeInt(definitions == null ? 0 : definitions.size());
        if (definitions != null) {
            for (ActionbarDefinition definition : definitions) {
                ActionbarDefinition.write(buf, definition);
            }
        }
    }

    static List<ActionbarDefinition> readDefinitions(FriendlyByteBuf buf) {
        int count = Math.min(buf.readInt(), 256);
        List<ActionbarDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            definitions.add(ActionbarDefinition.read(buf));
        }
        return definitions;
    }
}
