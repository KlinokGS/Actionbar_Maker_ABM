package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.data.ActionbarElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser and serializer for Minecraft-style formatting codes.
 * Supported codes:
 * colors: &0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f
 * styles: &k obfuscated, &l bold, &m strikethrough, &n underline, &o italic
 * reset: &r
 * literal ampersand: &&
 */
public final class AbmFormatCodes {
    public static final char PREFIX = '&';

    private AbmFormatCodes() {
    }

    public static List<ActionbarElement> parse(String raw) {
        List<ActionbarElement> result = new ArrayList<>();
        if (raw == null) {
            raw = "";
        }

        ActionbarElement current = new ActionbarElement("", 0xFFFFFF);
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == PREFIX && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                if (raw.charAt(i + 1) == PREFIX) {
                    buffer.append(PREFIX);
                    i++;
                    continue;
                }
                if (isSupportedCode(code)) {
                    flush(result, current, buffer);
                    applyCode(current, code);
                    i++;
                    continue;
                }
            }
            buffer.append(c);
        }
        flush(result, current, buffer);

        if (result.isEmpty()) {
            result.add(new ActionbarElement("", 0xFFFFFF));
        }
        return mergeAdjacent(result);
    }

    public static String serialize(List<ActionbarElement> elements) {
        StringBuilder builder = new StringBuilder();
        if (elements == null || elements.isEmpty()) {
            return "";
        }

        for (ActionbarElement element : elements) {
            if (element == null || element.text == null || element.text.isEmpty()) {
                continue;
            }
            builder.append(PREFIX).append(AbmColors.formatCodeByColor(element.color));
            if (element.obfuscated) {
                builder.append(PREFIX).append('k');
            }
            if (element.bold) {
                builder.append(PREFIX).append('l');
            }
            if (element.strikethrough) {
                builder.append(PREFIX).append('m');
            }
            if (element.underlined) {
                builder.append(PREFIX).append('n');
            }
            if (element.italic) {
                builder.append(PREFIX).append('o');
            }
            builder.append(escapeText(element.text));
        }
        return builder.toString();
    }

    public static String stripCodes(String raw) {
        StringBuilder builder = new StringBuilder();
        if (raw == null) {
            return "";
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == PREFIX && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                char code = Character.toLowerCase(next);
                if (next == PREFIX) {
                    builder.append(PREFIX);
                    i++;
                    continue;
                }
                if (isSupportedCode(code)) {
                    i++;
                    continue;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    public static boolean isSupportedCode(char code) {
        char lower = Character.toLowerCase(code);
        return AbmColors.isColorCode(lower)
                || lower == 'k'
                || lower == 'l'
                || lower == 'm'
                || lower == 'n'
                || lower == 'o'
                || lower == 'r';
    }

    private static void applyCode(ActionbarElement current, char code) {
        if (AbmColors.isColorCode(code)) {
            current.color = AbmColors.colorByFormatCode(code);
            resetStyles(current);
            return;
        }
        switch (code) {
            case 'k' -> current.obfuscated = true;
            case 'l' -> current.bold = true;
            case 'm' -> current.strikethrough = true;
            case 'n' -> current.underlined = true;
            case 'o' -> current.italic = true;
            case 'r' -> {
                current.color = 0xFFFFFF;
                resetStyles(current);
            }
            default -> {
            }
        }
    }

    private static void resetStyles(ActionbarElement current) {
        current.bold = false;
        current.italic = false;
        current.underlined = false;
        current.strikethrough = false;
        current.obfuscated = false;
    }

    private static void flush(List<ActionbarElement> result, ActionbarElement style, StringBuilder buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        ActionbarElement element = style.copy();
        element.text = buffer.toString();
        result.add(element);
        buffer.setLength(0);
    }

    private static String escapeText(String text) {
        return text == null ? "" : text.replace("&", "&&");
    }

    private static List<ActionbarElement> mergeAdjacent(List<ActionbarElement> source) {
        List<ActionbarElement> result = new ArrayList<>();
        for (ActionbarElement element : source) {
            if (element.text == null || element.text.isEmpty()) {
                continue;
            }
            if (!result.isEmpty() && sameStyle(result.get(result.size() - 1), element)) {
                result.get(result.size() - 1).text += element.text;
            } else {
                result.add(element.copy());
            }
        }
        return result;
    }

    private static boolean sameStyle(ActionbarElement a, ActionbarElement b) {
        return (a.color & 0xFFFFFF) == (b.color & 0xFFFFFF)
                && a.bold == b.bold
                && a.italic == b.italic
                && a.underlined == b.underlined
                && a.strikethrough == b.strikethrough
                && a.obfuscated == b.obfuscated;
    }
}
