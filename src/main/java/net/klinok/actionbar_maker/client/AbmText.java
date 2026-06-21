package net.klinok.actionbar_maker.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class AbmText {
    private static final String PREFIX = "actionbar_maker.";

    private AbmText() {
    }

    public static Component component(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    public static String string(String key, Object... args) {
        return I18n.get(PREFIX + key, args);
    }
}
