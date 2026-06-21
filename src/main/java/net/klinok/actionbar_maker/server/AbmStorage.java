package net.klinok.actionbar_maker.server;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.data.AbmJson;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AbmStorage {
    private static final String ABM_DIR = "abm";

    private AbmStorage() {
    }

    public static void ensureGlobalDirectories() {
        createDirectories(getConfigAbmDir());
        createDirectories(getHeadsDir());
        createDirectories(getTemplatesDir());
    }

    public static void ensureWorldDirectories(MinecraftServer server) {
        createDirectories(getWorldActionbarsDir(server));
    }

    public static Path getConfigAbmDir() {
        return FMLPaths.CONFIGDIR.get().resolve(ABM_DIR);
    }

    public static Path getHeadsDir() {
        return getConfigAbmDir().resolve("heads");
    }

    public static Path getTemplatesDir() {
        return getConfigAbmDir().resolve("templates");
    }

    public static Path getWorldActionbarsDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(ABM_DIR).resolve("actionbars");
    }

    public static List<ActionbarDefinition> loadActionbars(MinecraftServer server) {
        ensureWorldDirectories(server);
        return loadDefinitions(getWorldActionbarsDir(server));
    }

    public static List<ActionbarDefinition> loadTemplates() {
        ensureGlobalDirectories();
        return loadDefinitions(getTemplatesDir());
    }

    public static Optional<ActionbarDefinition> loadActionbar(MinecraftServer server, String name) {
        String normalized = normalizeName(name);
        return loadActionbars(server).stream()
                .filter(definition -> normalizeName(definition.name).equalsIgnoreCase(normalized))
                .findFirst();
    }

    public static void saveActionbar(MinecraftServer server, ActionbarDefinition definition) throws IOException {
        ensureWorldDirectories(server);
        saveDefinition(getWorldActionbarsDir(server), definition);
    }

    public static void saveTemplate(ActionbarDefinition definition) throws IOException {
        ensureGlobalDirectories();
        saveDefinition(getTemplatesDir(), definition);
    }

    public static boolean deleteActionbar(MinecraftServer server, String name) throws IOException {
        ensureWorldDirectories(server);
        String fileName = safeFileName(name) + ".json";
        Path direct = getWorldActionbarsDir(server).resolve(fileName);
        if (Files.exists(direct)) {
            Files.delete(direct);
            return true;
        }

        Optional<ActionbarDefinition> loaded = loadActionbar(server, name);
        if (loaded.isPresent()) {
            Path byJsonName = getWorldActionbarsDir(server).resolve(safeFileName(loaded.get().name) + ".json");
            if (Files.exists(byJsonName)) {
                Files.delete(byJsonName);
                return true;
            }
        }
        return false;
    }

    public static List<String> actionbarNames(MinecraftServer server) {
        return loadActionbars(server).stream().map(definition -> definition.name).toList();
    }

    private static List<ActionbarDefinition> loadDefinitions(Path directory) {
        createDirectories(directory);
        List<ActionbarDefinition> result = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(path -> readDefinition(path).ifPresent(result::add));
        } catch (IOException exception) {
            ActionbarMaker.LOGGER.error("Failed to read ABM directory {}", directory, exception);
        }
        return result;
    }

    private static Optional<ActionbarDefinition> readDefinition(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ActionbarDefinition definition = AbmJson.GSON.fromJson(reader, ActionbarDefinition.class);
            if (definition == null) {
                return Optional.empty();
            }
            definition.normalize();
            return Optional.of(definition);
        } catch (Exception exception) {
            ActionbarMaker.LOGGER.error("Failed to read ABM json {}", path, exception);
            return Optional.empty();
        }
    }

    private static void saveDefinition(Path directory, ActionbarDefinition definition) throws IOException {
        createDirectories(directory);
        definition.normalize();
        Path path = directory.resolve(safeFileName(definition.name) + ".json");
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            AbmJson.GSON.toJson(definition, writer);
        }
    }

    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            ActionbarMaker.LOGGER.error("Failed to create directory {}", path, exception);
        }
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    public static String safeFileName(String name) {
        String value = normalizeName(name);
        if (value.isBlank()) {
            value = "new_actionbar";
        }
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        value = value.replaceAll("\\s+", " ").trim();
        return value.isBlank() ? "new_actionbar" : value;
    }
}
