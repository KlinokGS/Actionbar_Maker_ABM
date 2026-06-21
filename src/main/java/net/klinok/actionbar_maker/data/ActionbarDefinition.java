package net.klinok.actionbar_maker.data;

import net.klinok.actionbar_maker.client.AbmText;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActionbarDefinition {
    public String name = "";
    public boolean background = true;
    public String head = "";
    public int defaultFadeIn = 5;
    public int defaultStay = 40;
    public int defaultFadeOut = 10;
    public int textYOffset = 0;
    public List<ActionbarElement> elements = new ArrayList<>();

    public ActionbarDefinition() {
    }

    public static ActionbarDefinition createDefault(String name) {
        ActionbarDefinition definition = new ActionbarDefinition();
        definition.name = name == null || name.isBlank() ? "Actionbar_" + UUID.randomUUID().toString().substring(0, 8) : name;
        definition.background = true;
        definition.elements.add(new ActionbarElement("[Name] ", 0xFFFF55));
        definition.elements.add(new ActionbarElement("Hello!", 0xFFFFFF));
        return definition;
    }

    public ActionbarDefinition copy() {
        ActionbarDefinition copy = new ActionbarDefinition();
        copy.name = this.name;
        copy.background = this.background;
        copy.head = this.head;
        copy.defaultFadeIn = this.defaultFadeIn;
        copy.defaultStay = this.defaultStay;
        copy.defaultFadeOut = this.defaultFadeOut;
        copy.textYOffset = this.textYOffset;
        copy.elements = new ArrayList<>();
        for (ActionbarElement element : this.elements) {
            copy.elements.add(element.copy());
        }
        return copy;
    }

    public void normalize() {
        if (name == null || name.isBlank()) {
            name = "actionbar";
        }
        if (head == null) {
            head = "";
        }
        if (elements == null) {
            elements = new ArrayList<>();
        }
        if (elements.isEmpty()) {
            elements.add(new ActionbarElement(String.valueOf(AbmText.component("screen.editor.title")), 0xFFFFFF));
        }
        defaultFadeIn = Math.max(0, defaultFadeIn);
        defaultStay = Math.max(1, defaultStay);
        defaultFadeOut = Math.max(0, defaultFadeOut);
        textYOffset = Math.max(-8, Math.min(8, textYOffset));
    }

    public static void write(FriendlyByteBuf buf, ActionbarDefinition definition) {
        definition.normalize();
        buf.writeUtf(definition.name, 128);
        buf.writeBoolean(definition.background);
        buf.writeUtf(definition.head == null ? "" : definition.head, 256);
        buf.writeInt(definition.defaultFadeIn);
        buf.writeInt(definition.defaultStay);
        buf.writeInt(definition.defaultFadeOut);
        buf.writeInt(definition.textYOffset);
        buf.writeInt(definition.elements.size());
        for (ActionbarElement element : definition.elements) {
            ActionbarElement.write(buf, element);
        }
    }

    public static ActionbarDefinition read(FriendlyByteBuf buf) {
        ActionbarDefinition definition = new ActionbarDefinition();
        definition.name = buf.readUtf(128);
        definition.background = buf.readBoolean();
        definition.head = buf.readUtf(256);
        definition.defaultFadeIn = buf.readInt();
        definition.defaultStay = buf.readInt();
        definition.defaultFadeOut = buf.readInt();
        definition.textYOffset = buf.readInt();
        int count = Math.min(buf.readInt(), 32);
        definition.elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            definition.elements.add(ActionbarElement.read(buf));
        }
        definition.normalize();
        return definition;
    }
}
