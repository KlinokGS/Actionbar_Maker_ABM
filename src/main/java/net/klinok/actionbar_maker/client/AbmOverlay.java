package net.klinok.actionbar_maker.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class AbmOverlay {
    private static ActionbarDefinition current;
    private static long startedAtMs;
    private static int fadeInTicks;
    private static int stayTicks;
    private static int fadeOutTicks;
    private static final float MIN_VISIBLE_ALPHA = 0.04F;

    private AbmOverlay() {
    }

    public static void play(ActionbarDefinition definition, int fadeIn, int stay, int fadeOut) {
        current = definition.copy();
        current.normalize();
        fadeInTicks = Math.max(0, fadeIn);
        stayTicks = Math.max(1, stay);
        fadeOutTicks = Math.max(0, fadeOut);
        startedAtMs = System.currentTimeMillis();
        HeadTextureCache.get(current.head);
    }

    public static boolean isActive() {
        if (current == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - startedAtMs;
        return elapsed < ticksToMs(fadeInTicks + stayTicks + fadeOutTicks);
    }

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isActive()) {
            current = null;
            return;
        }
        float alpha = getAlpha();
        if (alpha <= MIN_VISIBLE_ALPHA) {
            return;
        }

        // ChatScreen itself is drawn after HUD overlays, so ABM is rendered from ScreenEvent.Render.Post there.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ChatScreen) {
            return;
        }
        renderActionbar(graphics, current, screenWidth / 2, getVanillaActionbarY(screenHeight), alpha);
    }

    public static void renderAfterVanillaGui(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!isActive()) {
            current = null;
            return;
        }
        float alpha = getAlpha();
        if (alpha <= MIN_VISIBLE_ALPHA) {
            return;
        }
        renderActionbar(graphics, current, screenWidth / 2, getVanillaActionbarY(screenHeight), alpha);
    }

    public static void renderAfterChatScreen(GuiGraphics graphics, int screenWidth, int screenHeight) {
        renderAfterVanillaGui(graphics, screenWidth, screenHeight);
    }

    public static void renderPreview(GuiGraphics graphics, ActionbarDefinition definition, int centerX, int y, float alpha) {
        if (definition == null) {
            return;
        }
        ActionbarDefinition copy = definition.copy();
        copy.normalize();
        renderActionbar(graphics, copy, centerX, y, alpha);
    }

    private static float getAlpha() {
        long elapsedMs = System.currentTimeMillis() - startedAtMs;
        long fadeInMs = ticksToMs(fadeInTicks);
        long stayMs = ticksToMs(stayTicks);
        long fadeOutMs = ticksToMs(fadeOutTicks);

        if (fadeInMs > 0 && elapsedMs < fadeInMs) {
            return clamp(elapsedMs / (float) fadeInMs);
        }
        if (elapsedMs < fadeInMs + stayMs) {
            return 1.0F;
        }
        if (fadeOutMs > 0 && elapsedMs < fadeInMs + stayMs + fadeOutMs) {
            return clamp(1.0F - ((elapsedMs - fadeInMs - stayMs) / (float) fadeOutMs));
        }
        return fadeOutMs <= 0 ? 1.0F : 0.0F;
    }

    private static void renderActionbar(GuiGraphics graphics, ActionbarDefinition definition, int centerX, int topY, float alpha) {
        if (alpha <= MIN_VISIBLE_ALPHA) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        // Replace placeholders in elements for the current player
        ActionbarDefinition resolved = resolvePlaceholders(definition);
        int textWidth = 0;
        for (ActionbarElement element : resolved.elements) {
            textWidth += font.width(element.toComponent());
        }

        boolean hasHead = resolved.head != null && !resolved.head.isBlank() && HeadTextureCache.get(resolved.head) != null;
        int headSize = hasHead ? 16 : 0;
        int gap = hasHead && textWidth > 0 ? 4 : 0;
        int contentWidth = headSize + gap + textWidth;
        int contentHeight = Math.max(headSize, font.lineHeight);

        int contentLeft = centerX - contentWidth / 2;
        int textTop = topY + (contentHeight - font.lineHeight) / 2 + resolved.textYOffset;
        int headTop = topY + (contentHeight - headSize) / 2;

        if (resolved.background) {
            int bgAlpha = Math.max(0, Math.min(180, (int) (120 * alpha)));
            int bgColor = (bgAlpha << 24);
            int padX = 4;
            int padY = 4;
            graphics.fill(contentLeft - padX, topY - padY, contentLeft + contentWidth + padX, topY + contentHeight + padY, bgColor);
        }

        int drawX = contentLeft;
        if (hasHead) {
            ResourceLocation texture = HeadTextureCache.get(resolved.head);
            if (texture != null) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                graphics.blit(texture, drawX, headTop, 0, 0, headSize, headSize, headSize, headSize);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                drawX += headSize + gap;
            }
        }

        int textAlpha = Math.max(0, Math.min(255, (int) (255 * alpha)));
        for (ActionbarElement element : resolved.elements) {
            int color = (textAlpha << 24) | (element.color & 0xFFFFFF);
            graphics.drawString(font, element.toComponent(), drawX, textTop, color, false);
            drawX += font.width(element.toComponent());
        }
        RenderSystem.enableDepthTest();
        graphics.pose().popPose();
    }

    /**
     * Resolves placeholders in the actionbar definition for the current player.
     * Creates a copy of the definition with placeholder values substituted.
     */
    private static ActionbarDefinition resolvePlaceholders(ActionbarDefinition definition) {
        ActionbarDefinition copy = definition.copy();
        for (ActionbarElement element : copy.elements) {
            element.text = AbmPlaceholders.replace(element.text);
        }
        return copy;
    }

    private static int getVanillaActionbarY(int screenHeight) {
        return screenHeight - 68;
    }

    private static long ticksToMs(int ticks) {
        return Math.max(0, ticks) * 50L;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
