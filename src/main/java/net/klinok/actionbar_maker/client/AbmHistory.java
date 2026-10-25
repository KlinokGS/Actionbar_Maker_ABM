package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.data.ActionbarDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-side session history of actionbars that were actually played. */
public final class AbmHistory {
    public static final int MAX_ENTRIES = 20;
    private static final List<ActionbarDefinition> ENTRIES = new ArrayList<>();

    private AbmHistory() {}

    public static synchronized void add(ActionbarDefinition definition) {
        if (definition == null) return;
        ENTRIES.add(0, definition.copy());
        while (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.remove(ENTRIES.size() - 1);
        }
    }

    public static synchronized List<ActionbarDefinition> getEntries() {
        List<ActionbarDefinition> copy = new ArrayList<>(ENTRIES.size());
        for (ActionbarDefinition entry : ENTRIES) copy.add(entry.copy());
        return Collections.unmodifiableList(copy);
    }

    public static synchronized void clear() {
        ENTRIES.clear();
    }
}
