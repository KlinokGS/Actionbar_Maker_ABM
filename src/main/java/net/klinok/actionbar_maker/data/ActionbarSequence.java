package net.klinok.actionbar_maker.data;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActionbarSequence {
    public String name = "";
    public List<Entry> entries = new ArrayList<>();

    public static class Entry {
        public ActionbarDefinition definition = ActionbarDefinition.createDefault("");
        public int pauseBefore = 0;
        public int fadeIn = 5;
        public int stay = 40;
        public int fadeOut = 10;
        public int pauseAfter = 0;

        public Entry copy() {
            Entry e = new Entry();
            e.definition = definition.copy();
            e.pauseBefore = pauseBefore;
            e.fadeIn = fadeIn;
            e.stay = stay;
            e.fadeOut = fadeOut;
            e.pauseAfter = pauseAfter;
            return e;
        }
        public void normalize() {
            if (definition == null) definition = ActionbarDefinition.createDefault("");
            definition.normalize();
            pauseBefore = clamp(pauseBefore, 0, 1200);
            fadeIn = clamp(fadeIn, 0, 200);
            stay = clamp(stay, 1, 1200);
            fadeOut = clamp(fadeOut, 0, 200);
            pauseAfter = clamp(pauseAfter, 0, 1200);
        }
        private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
        public static void write(FriendlyByteBuf buf, Entry e) {
            e.normalize();
            ActionbarDefinition.write(buf, e.definition);
            buf.writeInt(e.pauseBefore); buf.writeInt(e.fadeIn); buf.writeInt(e.stay);
            buf.writeInt(e.fadeOut); buf.writeInt(e.pauseAfter);
        }
        public static Entry read(FriendlyByteBuf buf) {
            Entry e = new Entry();
            e.definition = ActionbarDefinition.read(buf);
            e.pauseBefore = buf.readInt(); e.fadeIn = buf.readInt(); e.stay = buf.readInt();
            e.fadeOut = buf.readInt(); e.pauseAfter = buf.readInt();
            e.normalize(); return e;
        }
    }

    public static ActionbarSequence create(String name) {
        ActionbarSequence s = new ActionbarSequence();
        s.name = name == null || name.isBlank() ? "Group_" + UUID.randomUUID().toString().substring(0,8) : name;
        return s;
    }
    public ActionbarSequence copy() {
        ActionbarSequence c = create(name);
        c.entries = new ArrayList<>();
        if (entries != null) for (Entry e : entries) c.entries.add(e.copy());
        return c;
    }
    public void normalize() {
        if (name == null || name.isBlank()) name = "sequence";
        if (entries == null) entries = new ArrayList<>();
        if (entries.size() > 64) entries = new ArrayList<>(entries.subList(0,64));
        for (Entry e : entries) if (e != null) e.normalize();
        entries.removeIf(e -> e == null);
    }
    public static void write(FriendlyByteBuf buf, ActionbarSequence s) {
        s.normalize(); buf.writeUtf(s.name,128); buf.writeInt(s.entries.size());
        for (Entry e : s.entries) Entry.write(buf,e);
    }
    public static ActionbarSequence read(FriendlyByteBuf buf) {
        ActionbarSequence s = create(buf.readUtf(128));
        int count = Math.max(0, Math.min(64,buf.readInt()));
        for (int i=0;i<count;i++) s.entries.add(Entry.read(buf));
        s.normalize(); return s;
    }
}
