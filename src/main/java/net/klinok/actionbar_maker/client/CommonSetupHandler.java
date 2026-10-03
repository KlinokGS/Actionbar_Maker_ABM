package net.klinok.actionbar_maker.client;
import net.klinok.actionbar_maker.ActionbarMaker;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = ActionbarMaker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetupHandler {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Path basicTemplatesDir = FMLPaths.CONFIGDIR.get().resolve("abm/templates");
                Path basicHeadsDir = FMLPaths.CONFIGDIR.get().resolve("abm/heads");
                Files.createDirectories(basicTemplatesDir);
                Files.createDirectories(basicHeadsDir);

                // get mod file
                IModFileInfo modFileInfo = ModList.get().getModFileById("actionbar_maker");
                if (modFileInfo == null) {
                    return;
                }

                // system templates path
                Path sourceDir = modFileInfo.getFile().findResource("assets/actionbar_maker/basics");

                if (!Files.exists(sourceDir)) {
                    ActionbarMaker.LOGGER.warn("Templates folder not found: {}", sourceDir);
                    return;
                }

                // copy files from mod folder into config/abm/basics/...
                try (Stream<Path> paths = Files.list(sourceDir)) {
                    paths.forEach(source -> {
                                String fileName = source.getFileName().toString();
                                Path targetDir;

                                if (fileName.endsWith(".json")) {
                                    targetDir = basicTemplatesDir;
                                } else if (fileName.endsWith(".png")) {
                                    targetDir = basicHeadsDir;
                                } else {
                                    return;
                                }

                                try {
                                    Files.createDirectories(targetDir);
                                    Path destination = targetDir.resolve(fileName);
                                    if (!Files.exists(destination)) {
                                        Files.copy(source, destination);
                                    }
                                } catch (IOException e) {
                                    ActionbarMaker.LOGGER.error("Failed to copy mod templates", e);
                                }
                            });
                }
            } catch (IOException e) {
                ActionbarMaker.LOGGER.error("Something went wrong with copying mod templates", e);
            }

        });
    }
}
