package net.klinok.actionbar_maker.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.klinok.actionbar_maker.ActionbarMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class HeadTextureCache {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private HeadTextureCache() {
    }

    public static ResourceLocation get(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        if (CACHE.containsKey(fileName)) {
            return CACHE.get(fileName);
        }
        Path path = ClientFileHelper.getHeadPath(fileName);
        if (path == null || !Files.exists(path)) {
            CACHE.put(fileName, null);
            return null;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(inputStream);
            DynamicTexture texture = new DynamicTexture(image);
            String hash = Integer.toHexString(fileName.toLowerCase(Locale.ROOT).hashCode());
            ResourceLocation location = new ResourceLocation(ActionbarMaker.MODID, "dynamic_head_" + hash);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            CACHE.put(fileName, location);
            return location;
        } catch (Exception exception) {
            ActionbarMaker.LOGGER.error("Failed to load ABM head png {}", path, exception);
            CACHE.put(fileName, null);
            return null;
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}
