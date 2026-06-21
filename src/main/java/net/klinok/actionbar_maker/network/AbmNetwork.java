package net.klinok.actionbar_maker.network;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AbmNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;
    private static int packetId = 0;

    private AbmNetwork() {
    }

    public static void register() {
        if (CHANNEL != null) {
            return;
        }
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ActionbarMaker.MODID, "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        CHANNEL.messageBuilder(S2COpenManagerPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenManagerPacket::encode)
                .decoder(S2COpenManagerPacket::decode)
                .consumerMainThread(S2COpenManagerPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2COpenEditorPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenEditorPacket::encode)
                .decoder(S2COpenEditorPacket::decode)
                .consumerMainThread(S2COpenEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CPlayActionbarPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPlayActionbarPacket::encode)
                .decoder(S2CPlayActionbarPacket::decode)
                .consumerMainThread(S2CPlayActionbarPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SSaveActionbarPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSaveActionbarPacket::encode)
                .decoder(C2SSaveActionbarPacket::decode)
                .consumerMainThread(C2SSaveActionbarPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SDeleteActionbarPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SDeleteActionbarPacket::encode)
                .decoder(C2SDeleteActionbarPacket::decode)
                .consumerMainThread(C2SDeleteActionbarPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (CHANNEL == null) {
            ActionbarMaker.LOGGER.error("[ABM NETWORK] Tried to send {} to {}, but channel is not registered", packet.getClass().getSimpleName(), player.getGameProfile().getName());
            return;
        }
        ActionbarMaker.LOGGER.info("[ABM NETWORK] Sending {} to {}", packet.getClass().getSimpleName(), player.getGameProfile().getName());
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static boolean isRegistered() {
        return CHANNEL != null;
    }

    public static void sendToServer(Object packet) {
        if (CHANNEL == null) {
            ActionbarMaker.LOGGER.error("[ABM NETWORK] Tried to send {} to server, but channel is not registered", packet.getClass().getSimpleName());
            return;
        }
        CHANNEL.sendToServer(packet);
    }

    private static int nextId() {
        return packetId++;
    }
}
