package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class AbmConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.IntValue ACTIONBAR_DEFAULT_FADE_IN;
    public static final ForgeConfigSpec.IntValue ACTIONBAR_DEFAULT_STAY;
    public static final ForgeConfigSpec.IntValue ACTIONBAR_DEFAULT_FADE_OUT;
    public static final ForgeConfigSpec.IntValue SEQUENCE_DEFAULT_PAUSE_BEFORE;
    public static final ForgeConfigSpec.IntValue SEQUENCE_DEFAULT_PAUSE_AFTER;
    public static final ForgeConfigSpec.IntValue SEQUENCE_DEFAULT_FADE_IN;
    public static final ForgeConfigSpec.IntValue SEQUENCE_DEFAULT_STAY;
    public static final ForgeConfigSpec.IntValue SEQUENCE_DEFAULT_FADE_OUT;
    public static final ForgeConfigSpec.BooleanValue UI_SHOW_SETTINGS_BUTTON;

    private AbmConfig() {
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        COMMAND_PERMISSION_LEVEL = builder
                .comment("Минимальный уровень прав для команд /abm (0-4). 2 = оператор.")
                .comment("Minimum permission level for /abm commands (0-4). 2 = operator.")
                .defineInRange("commandPermissionLevel", 2, 0, 4);
        builder.pop();

        builder.push("actionbar");
        ACTIONBAR_DEFAULT_FADE_IN = builder
                .comment("Дефолтный fadeIn для новых экшнбаров (тики, 0-200).")
                .comment("Default fadeIn for new actionbars (ticks, 0-200).")
                .defineInRange("defaultFadeIn", 5, 0, 200);
        ACTIONBAR_DEFAULT_STAY = builder
                .comment("Дефолтный stay для новых экшнбаров (тики, 1-1200).")
                .comment("Default stay for new actionbars (ticks, 1-1200).")
                .defineInRange("defaultStay", 40, 1, 1200);
        ACTIONBAR_DEFAULT_FADE_OUT = builder
                .comment("Дефолтный fadeOut для новых экшнбаров (тики, 0-200).")
                .comment("Default fadeOut for new actionbars (ticks, 0-200).")
                .defineInRange("defaultFadeOut", 10, 0, 200);
        builder.pop();

        builder.push("sequence");
        SEQUENCE_DEFAULT_PAUSE_BEFORE = builder
                .comment("Дефолтная пауза перед каждым элементом при создании новой группы (тики, 0-1200).")
                .comment("Default pause before each entry when creating a new sequence group (ticks, 0-1200).")
                .defineInRange("defaultPauseBefore", 0, 0, 1200);
        SEQUENCE_DEFAULT_PAUSE_AFTER = builder
                .comment("Дефолтная пауза после каждого элемента при создании новой группы (тики, 0-1200).")
                .comment("Default pause after each entry when creating a new sequence group (ticks, 0-1200).")
                .defineInRange("defaultPauseAfter", 0, 0, 1200);
        SEQUENCE_DEFAULT_FADE_IN = builder
                .comment("Дефолтный fadeIn для новых элементов группы (тики, 0-200).")
                .comment("Default fadeIn for new sequence entries (ticks, 0-200).")
                .defineInRange("defaultFadeIn", 5, 0, 200);
        SEQUENCE_DEFAULT_STAY = builder
                .comment("Дефолтный stay для новых элементов группы (тики, 1-1200).")
                .comment("Default stay for new sequence entries (ticks, 1-1200).")
                .defineInRange("defaultStay", 40, 1, 1200);
        SEQUENCE_DEFAULT_FADE_OUT = builder
                .comment("Дефолтный fadeOut для новых элементов группы (тики, 0-200).")
                .comment("Default fadeOut for new sequence entries (ticks, 0-200).")
                .defineInRange("defaultFadeOut", 10, 0, 200);
        builder.pop();

        builder.push("ui");
        UI_SHOW_SETTINGS_BUTTON = builder
                .comment("Показывать кнопку настроек в редакторе экшнбара.")
                .comment("Show the settings button in the actionbar editor.")
                .define("showSettingsButton", true);
        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "actionbar_maker.toml");
        ActionbarMaker.LOGGER.info("[ABM CONFIG] Registered client config.");
    }

    /** Безопасный геттер: возвращает значение конфига, или дефолт, если конфиг ещё не загружен (dev-среда / выделенный сервер). */
    public static int commandPermissionLevel() {
        return safeGet(COMMAND_PERMISSION_LEVEL, 2);
    }

    public static int actionbarDefaultFadeIn() {
        return safeGet(ACTIONBAR_DEFAULT_FADE_IN, 5);
    }

    public static int actionbarDefaultStay() {
        return safeGet(ACTIONBAR_DEFAULT_STAY, 40);
    }

    public static int actionbarDefaultFadeOut() {
        return safeGet(ACTIONBAR_DEFAULT_FADE_OUT, 10);
    }

    public static int sequenceDefaultPauseBefore() {
        return safeGet(SEQUENCE_DEFAULT_PAUSE_BEFORE, 0);
    }

    public static int sequenceDefaultPauseAfter() {
        return safeGet(SEQUENCE_DEFAULT_PAUSE_AFTER, 0);
    }

    public static int sequenceDefaultFadeIn() {
        return safeGet(SEQUENCE_DEFAULT_FADE_IN, 5);
    }

    public static int sequenceDefaultStay() {
        return safeGet(SEQUENCE_DEFAULT_STAY, 40);
    }

    public static int sequenceDefaultFadeOut() {
        return safeGet(SEQUENCE_DEFAULT_FADE_OUT, 10);
    }

    public static boolean uiShowSettingsButton() {
        return safeGet(UI_SHOW_SETTINGS_BUTTON, true);
    }

    private static <T> T safeGet(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
