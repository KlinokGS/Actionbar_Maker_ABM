package net.klinok.actionbar_maker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for replacing dynamic placeholders in actionbar text.
 * Placeholders are replaced on the client side for the specific player viewing the actionbar.
 *
 * Supported placeholders:
 * - %player% - player's username
 * - %health% - player's current health (formatted)
 */
public final class AbmPlaceholders {
    private static final Map<String, Function<Player, String>> PLACEHOLDERS = new HashMap<>();

    static {
        PLACEHOLDERS.put("%player%", player -> player.getGameProfile().getName());
        PLACEHOLDERS.put("%health%", player -> {
            float health = player.getHealth();
            int hearts = Math.round(health);
            return String.valueOf(hearts);
        });
    }

    private AbmPlaceholders() {
    }

    /**
     * Replaces all placeholders in the given text with values for the current player.
     *
     * @param text the text containing placeholders
     * @return text with placeholders replaced
     */
    public static String replace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, Function<Player, String>> entry : PLACEHOLDERS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue().apply(player));
        }
        return result;
    }

    /**
     * Gets a list of all supported placeholder keys.
     *
     * @return array of placeholder strings
     */
    public static String[] getSupportedPlaceholders() {
        return PLACEHOLDERS.keySet().toArray(new String[0]);
    }
}