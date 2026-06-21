package net.klinok.actionbar_maker.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class ActionbarElement {
    public String text = "";
    public int color = 0xFFFFFF;
    public boolean bold = false;
    public boolean italic = false;
    public boolean underlined = false;
    public boolean strikethrough = false;
    public boolean obfuscated = false;

    public ActionbarElement() {
    }

    public ActionbarElement(String text, int color) {
        this.text = text;
        this.color = color;
    }

    public ActionbarElement copy() {
        ActionbarElement copy = new ActionbarElement();
        copy.text = this.text;
        copy.color = this.color;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underlined = this.underlined;
        copy.strikethrough = this.strikethrough;
        copy.obfuscated = this.obfuscated;
        return copy;
    }

    public Component toComponent() {
        Style style = Style.EMPTY
                .withBold(bold)
                .withItalic(italic)
                .withUnderlined(underlined)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated);
        return Component.literal(text == null ? "" : text).setStyle(style);
    }

    public static void write(FriendlyByteBuf buf, ActionbarElement element) {
        buf.writeUtf(element.text == null ? "" : element.text, 512);
        buf.writeInt(element.color);
        buf.writeBoolean(element.bold);
        buf.writeBoolean(element.italic);
        buf.writeBoolean(element.underlined);
        buf.writeBoolean(element.strikethrough);
        buf.writeBoolean(element.obfuscated);
    }

    public static ActionbarElement read(FriendlyByteBuf buf) {
        ActionbarElement element = new ActionbarElement();
        element.text = buf.readUtf(512);
        element.color = buf.readInt();
        element.bold = buf.readBoolean();
        element.italic = buf.readBoolean();
        element.underlined = buf.readBoolean();
        element.strikethrough = buf.readBoolean();
        element.obfuscated = buf.readBoolean();
        return element;
    }
}
