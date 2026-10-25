package net.klinok.actionbar_maker.client;

import net.klinok.actionbar_maker.ActionbarMaker;
import net.klinok.actionbar_maker.client.screen.AbmEditorScreen;
import net.klinok.actionbar_maker.client.screen.AbmManagerScreen;
import net.klinok.actionbar_maker.client.screen.AbmHistoryScreen;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

public final class AbmClient {
    private static final KeyMapping HISTORY_KEY = new KeyMapping(
            "key.actionbar_maker.history",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.actionbar_maker"
    );

    private AbmClient() {
    }

    public static void init() {
        ClientFileHelper.ensureClientDirectories();
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }

    public static void openManager(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ClientFileHelper.ensureClientDirectories();
            minecraft.setScreen(new AbmManagerScreen(actionbars, templates));
        });
    }

    public static void openEditor(ActionbarDefinition definition, List<ActionbarDefinition> templates) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ClientFileHelper.ensureClientDirectories();
            minecraft.setScreen(new AbmEditorScreen(definition, templates, null));
        });
    }

    public static void play(ActionbarDefinition definition, int fadeIn, int stay, int fadeOut) {
        AbmOverlay.play(definition, fadeIn, stay, fadeOut);
    }

    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();

            // The history key must work even after the temporary actionbar has disappeared.
            if (HISTORY_KEY.consumeClick() && minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new AbmHistoryScreen());
            }

            if (!AbmOverlay.isActive()) {
                return;
            }
            if (minecraft.player != null && minecraft.gui != null) {
                // While custom ABM overlay is alive, wipe vanilla /title actionbar text.
                minecraft.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
            }
        }

        @SubscribeEvent
        public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof ChatScreen && AbmOverlay.isActive()) {
                AbmOverlay.renderAfterChatScreen(
                        event.getGuiGraphics(),
                        minecraft.getWindow().getGuiScaledWidth(),
                        minecraft.getWindow().getGuiScaledHeight()
                );
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ActionbarMaker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(AbmClient::init);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(HISTORY_KEY);
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("abm_actionbar", AbmOverlay::renderOverlay);
        }

    }
}
