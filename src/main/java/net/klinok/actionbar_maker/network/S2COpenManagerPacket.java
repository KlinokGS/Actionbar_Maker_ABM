package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.ClientPacketHandlers;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarSequence;
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
    public final List<ActionbarSequence> sequences;

    public S2COpenManagerPacket(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates, List<ActionbarSequence> sequences) {
        this.actionbars = actionbars;
        this.templates = templates;
        this.sequences = sequences;
    }

    public static void encode(S2COpenManagerPacket packet, FriendlyByteBuf buf) {
        writeDefinitions(buf, packet.actionbars);
        writeDefinitions(buf, packet.templates);
        buf.writeInt(packet.sequences == null ? 0 : packet.sequences.size());
        if (packet.sequences != null) for (ActionbarSequence sequence : packet.sequences) ActionbarSequence.write(buf, sequence);
    }

    public static S2COpenManagerPacket decode(FriendlyByteBuf buf) {
        List<ActionbarDefinition> actionbars = readDefinitions(buf);
        List<ActionbarDefinition> templates = readDefinitions(buf);
        int count = Math.min(buf.readInt(), 256);
        List<ActionbarSequence> sequences = new ArrayList<>();
        for (int i = 0; i < count; i++) sequences.add(ActionbarSequence.read(buf));
        return new S2COpenManagerPacket(actionbars, templates, sequences);
    }

    public static void handle(S2COpenManagerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ActionbarMaker.LOGGER.info("[ABM PACKET] S2COpenManagerPacket received on {}", context.getDirection().getReceptionSide());
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openManager(packet.actionbars, packet.templates, packet.sequences));
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
