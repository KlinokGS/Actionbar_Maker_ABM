package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.data.AbmJson;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientFileHelper {
    private ClientFileHelper() {
    }

    public static Path getConfigAbmDir() {
        return FMLPaths.CONFIGDIR.get().resolve("abm");
    }

    public static Path getHeadsDir() {
        return getConfigAbmDir().resolve("heads");
    }

    public static Path getTemplatesDir() {
        return getConfigAbmDir().resolve("templates");
    }

    public static void ensureClientDirectories() {
        try {
            Files.createDirectories(getHeadsDir());
            Files.createDirectories(getTemplatesDir());
        } catch (IOException exception) {
            ActionbarMaker.LOGGER.error("Failed to create ABM client directories", exception);
        }
    }

    public static List<String> listHeads() {
        ensureClientDirectories();
        try (var stream = Files.list(getHeadsDir())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(file -> file.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(String::toLowerCase))
                    .toList();
        } catch (IOException exception) {
            ActionbarMaker.LOGGER.error("Failed to list ABM heads", exception);
            return List.of();
        }
    }

    public static List<ActionbarDefinition> listLocalTemplates() {
        ensureClientDirectories();
        Map<String, ActionbarDefinition> result = new LinkedHashMap<>();
        try (var stream = Files.list(getTemplatesDir())) {
            stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(path -> readTemplate(path, result));
        } catch (IOException exception) {
            ActionbarMaker.LOGGER.error("Failed to list ABM local templates", exception);
        }
        return new ArrayList<>(result.values());
    }

    private static void readTemplate(Path path, Map<String, ActionbarDefinition> result) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ActionbarDefinition definition = AbmJson.GSON.fromJson(reader, ActionbarDefinition.class);
            if (definition == null) {
                return;
            }
            definition.normalize();
            String key = (definition.name == null || definition.name.isBlank() ? path.getFileName().toString() : definition.name).toLowerCase(Locale.ROOT);
            result.putIfAbsent(key, definition);
        } catch (Exception exception) {
            ActionbarMaker.LOGGER.error("Failed to read ABM local template {}", path, exception);
        }
    }

    public static Path getHeadPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Path root = getHeadsDir().toAbsolutePath().normalize();
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root)) {
            return null;
        }
        return path;
    }
}
