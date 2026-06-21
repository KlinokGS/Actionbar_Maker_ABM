package net.klinok.actionbar_maker.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class AbmJson {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private AbmJson() {
    }
}
