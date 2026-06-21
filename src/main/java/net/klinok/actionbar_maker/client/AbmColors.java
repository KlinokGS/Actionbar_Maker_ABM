package net.klinok.actionbar_maker.client;

/**
 * Vanilla Minecraft chat/title colors.
 */
public final class AbmColors {
    public static final int[] COLORS = {
            0x000000, // black
            0x0000AA, // dark_blue
            0x00AA00, // dark_green
            0x00AAAA, // dark_aqua
            0xAA0000, // dark_red
            0xAA00AA, // dark_purple
            0xFFAA00, // gold
            0xAAAAAA, // gray
            0x555555, // dark_gray
            0x5555FF, // blue
            0x55FF55, // green
            0x55FFFF, // aqua
            0xFF5555, // red
            0xFF55FF, // light_purple
            0xFFFF55, // yellow
            0xFFFFFF  // white
    };

    public static final char[] FORMAT_CODES = {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    public static final String[] COLOR_KEYS = {
            "color.black",
            "color.dark_blue",
            "color.dark_green",
            "color.dark_aqua",
            "color.dark_red",
            "color.dark_purple",
            "color.gold",
            "color.gray",
            "color.dark_gray",
            "color.blue",
            "color.green",
            "color.aqua",
            "color.red",
            "color.light_purple",
            "color.yellow",
            "color.white"
    };

    public static final String[] SHORT_COLOR_KEYS = {
            "color.short.black",
            "color.short.dark_blue",
            "color.short.dark_green",
            "color.short.dark_aqua",
            "color.short.dark_red",
            "color.short.dark_purple",
            "color.short.gold",
            "color.short.gray",
            "color.short.dark_gray",
            "color.short.blue",
            "color.short.green",
            "color.short.aqua",
            "color.short.red",
            "color.short.light_purple",
            "color.short.yellow",
            "color.short.white"
    };

    private AbmColors() {
    }

    public static int indexOf(int color) {
        int normalized = color & 0xFFFFFF;
        for (int i = 0; i < COLORS.length; i++) {
            if ((COLORS[i] & 0xFFFFFF) == normalized) {
                return i;
            }
        }
        return COLORS.length - 1; // white
    }

    public static int colorByFormatCode(char code) {
        char lower = Character.toLowerCase(code);
        for (int i = 0; i < FORMAT_CODES.length; i++) {
            if (FORMAT_CODES[i] == lower) {
                return COLORS[i];
            }
        }
        return 0xFFFFFF;
    }

    public static boolean isColorCode(char code) {
        char lower = Character.toLowerCase(code);
        for (char formatCode : FORMAT_CODES) {
            if (formatCode == lower) {
                return true;
            }
        }
        return false;
    }

    public static char formatCodeByColor(int color) {
        return FORMAT_CODES[indexOf(color)];
    }
}
