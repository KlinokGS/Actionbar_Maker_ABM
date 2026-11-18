package net.klinok.actionbar_maker.api;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.AbmFormatCodes;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarSequence;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.network.S2CPlayActionbarPacket;
import net.klinok.actionbar_maker.network.S2CPlaySequencePacket;
import net.klinok.actionbar_maker.network.S2CStopActionbarPacket;
import net.klinok.actionbar_maker.server.AbmStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Public Actionbar Maker API for mods.
 *
 * <p>Examples:</p>
 * <pre>{@code
 * // Play actionbar by name
 * AbmApi.play(player, "welcome");
 *
 * // Show custom text (color codes with '&' — like in /abm make)
 * AbmApi.make(targets, "&6[Server] &fHello!");
 *
 * // Create new actionbar
 * ActionbarDefinition definition = AbmApi.createActionbar(server, "my_actionbar", "&aHi &fthere");
 * }</pre>
 *
 * <p>All methods should be used on the server side (in any server thread, using Forge).</p>
 */
public final class AbmApi {
    private AbmApi() {
    }

    // ------------------------------------------------------------------
    // Play saved content
    // ------------------------------------------------------------------

    /** Play saved actionbar (search by name in any collection). */
    public static boolean play(Collection<ServerPlayer> targets, String name) {
        return playActionbar(targets, name, -1, -1, -1) || playSequence(targets, name);
    }

    /** Play saved actionbar or group for one player. */
    public static boolean play(ServerPlayer target, String name) {
        return play(List.of(target), name);
    }

    /**
     * Play saved actionbar by name with changed timings.
     * Values lower than 0 will get defaults from definition.
     */
    public static boolean playActionbar(Collection<ServerPlayer> targets, String name, int fadeIn, int stay, int fadeOut) {
        MinecraftServer server = serverOf(targets);
        if (server == null || targets.isEmpty()) {
            return false;
        }
        Optional<ActionbarDefinition> loaded = AbmStorage.loadActionbar(server, name);
        if (loaded.isEmpty()) {
            return false;
        }
        ActionbarDefinition definition = loaded.get();
        definition.normalize();
        int realFadeIn = fadeIn >= 0 ? fadeIn : definition.defaultFadeIn;
        int realStay = stay >= 0 ? stay : definition.defaultStay;
        int realFadeOut = fadeOut >= 0 ? fadeOut : definition.defaultFadeOut;
        for (ServerPlayer target : targets) {
            AbmNetwork.sendToPlayer(target, new S2CPlayActionbarPacket(definition, realFadeIn, realStay, realFadeOut));
        }
        ActionbarMaker.LOGGER.info("[ABM API] playActionbar '{}' -> {} player(s)", definition.name, targets.size());
        return true;
    }

    /** Play saved sequence by name. */
    public static boolean playSequence(Collection<ServerPlayer> targets, String name) {
        MinecraftServer server = serverOf(targets);
        if (server == null || targets.isEmpty()) {
            return false;
        }
        Optional<ActionbarSequence> loaded = AbmStorage.loadSequence(server, name);
        if (loaded.isEmpty()) {
            return false;
        }
        ActionbarSequence sequence = loaded.get();
        sequence.normalize();
        for (ServerPlayer target : targets) {
            AbmNetwork.sendToPlayer(target, new S2CPlaySequencePacket(sequence));
        }
        ActionbarMaker.LOGGER.info("[ABM API] playSequence '{}' -> {} player(s)", sequence.name, targets.size());
        return true;
    }

    // ------------------------------------------------------------------
    // Создание «на лету» (API-эквивалент /abm make)
    // ------------------------------------------------------------------

    /**
     * Make actionbar definition (without save).
     * Color codes with '&', like: "{&6[Server] &fHi}".
     */
    public static ActionbarDefinition buildActionbar(String name, String text) {
        ActionbarDefinition definition = ActionbarDefinition.createDefault(name == null || name.isBlank() ? "api" : name);
        definition.elements = AbmFormatCodes.parse(text == null ? "" : text);
        definition.normalize();
        return definition;
    }

    /** Custom text with default timings. */
    public static boolean make(Collection<ServerPlayer> targets, String text) {
        return make(targets, text, -1, -1, -1);
    }

    /** Custom text for one player with default timings */
    public static boolean make(ServerPlayer target, String text) {
        return make(List.of(target), text, -1, -1, -1);
    }

    /**
     * API-like /abm make: collect actionbar from text.
     * Values lower than 0 use default settings.
     * Returns false, if text empty/too long or targets are empty.
     */
    public static boolean make(Collection<ServerPlayer> targets, String text, int fadeIn, int stay, int fadeOut) {
        if (targets == null || targets.isEmpty()) {
            return false;
        }
        if (text == null || text.isBlank() || text.length() > 256) {
            ActionbarMaker.LOGGER.warn("[ABM API] make() rejected: text is empty or longer than 256 chars");
            return false;
        }
        ActionbarDefinition definition = buildActionbar("api", text);
        int realFadeIn = fadeIn >= 0 ? fadeIn : definition.defaultFadeIn;
        int realStay = stay >= 0 ? stay : definition.defaultStay;
        int realFadeOut = fadeOut >= 0 ? fadeOut : definition.defaultFadeOut;
        for (ServerPlayer target : targets) {
            AbmNetwork.sendToPlayer(target, new S2CPlayActionbarPacket(definition, realFadeIn, realStay, realFadeOut));
        }
        ActionbarMaker.LOGGER.info("[ABM API] make() -> {} player(s)", targets.size());
        return true;
    }

    // ------------------------------------------------------------------
    // Хранилище: создание, чтение, удаление
    // ------------------------------------------------------------------

    /** Save definition in world save (like actionbar, created in editor). */
    public static void saveActionbar(MinecraftServer server, ActionbarDefinition definition) throws IOException {
        AbmStorage.saveActionbar(server, definition);
        ActionbarMaker.LOGGER.info("[ABM API] saveActionbar '{}'", definition.name);
    }

    /** Collect actionbar and save it in a folder. */
    public static ActionbarDefinition createActionbar(MinecraftServer server, String name, String text) throws IOException {
        ActionbarDefinition definition = buildActionbar(name, text);
        saveActionbar(server, definition);
        return definition;
    }

    /** Find saved actionbar by name. */
    public static Optional<ActionbarDefinition> findActionbar(MinecraftServer server, String name) {
        return AbmStorage.loadActionbar(server, name);
    }

    /** Returns all saved actionbars. */
    public static List<ActionbarDefinition> listActionbars(MinecraftServer server) {
        return AbmStorage.loadActionbars(server);
    }

    /** Returns all saved groups. */
    public static List<ActionbarSequence> listSequences(MinecraftServer server) {
        return AbmStorage.loadSequences(server);
    }

    /** Delete actionbar by name. */
    public static boolean deleteActionbar(MinecraftServer server, String name) throws IOException {
        boolean deleted = AbmStorage.deleteActionbar(server, name);
        if (deleted) {
            ActionbarMaker.LOGGER.info("[ABM API] deleteActionbar '{}'", name);
        }
        return deleted;
    }

    // ------------------------------------------------------------------
    // Stopping
    // ------------------------------------------------------------------

    /** Stop current actionbar/group */
    public static void stop(Collection<ServerPlayer> targets) {
        if (targets == null) {
            return;
        }
        for (ServerPlayer target : targets) {
            AbmNetwork.sendToPlayer(target, new S2CStopActionbarPacket());
        }
    }

    /** Stop current actionbar/group for taget player */
    public static void stop(ServerPlayer target) {
        stop(List.of(target));
    }

    private static MinecraftServer serverOf(Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            return target.server;
        }
        return null;
    }
}
