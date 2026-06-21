package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.screen.AbmEditorScreen;
import net.klinok.actionbar_maker.client.screen.AbmManagerScreen;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openManager(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ActionbarMaker.LOGGER.debug("[ABM CLIENT] Open manager packet. actionbars={}, templates={}",
                    actionbars == null ? 0 : actionbars.size(),
                    templates == null ? 0 : templates.size());
            ClientFileHelper.ensureClientDirectories();
            minecraft.setScreen(new AbmManagerScreen(actionbars, templates));
        });
    }

    public static void openEditor(ActionbarDefinition definition, List<ActionbarDefinition> templates) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ActionbarMaker.LOGGER.debug("[ABM CLIENT] Open editor packet. name={}, templates={}",
                    definition == null ? "null" : definition.name,
                    templates == null ? 0 : templates.size());
            ClientFileHelper.ensureClientDirectories();
            minecraft.setScreen(new AbmEditorScreen(definition, templates, null));
        });
    }

    public static void play(ActionbarDefinition definition, int fadeIn, int stay, int fadeOut) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ActionbarMaker.LOGGER.debug("[ABM CLIENT] Play packet. name={}, fadeIn={}, stay={}, fadeOut={}",
                    definition == null ? "null" : definition.name, fadeIn, stay, fadeOut);
            AbmOverlay.play(definition, fadeIn, stay, fadeOut);
        });
    }
}
