package net.klinok.actionbar_maker.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarElement;
import net.klinok.actionbar_maker.data.ActionbarSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class AbmOverlay {
    private static ActionbarDefinition current;
    private static int startedAtTick;
    private static int fadeInTicks;
    private static int stayTicks;
    private static int fadeOutTicks;
    private static ActionbarSequence activeSequence;
    private static int sequenceStartedAtTick;
    private static int activeSequenceEntry = -1;
    private static int clientTick = 0;
    private static final float MIN_VISIBLE_ALPHA = 0.04F;

    private AbmOverlay() {
    }

    /** Called from the client tick handler to keep a local tick counter. */
    public static void tick() {
        clientTick++;
    }

    public static void stop() {
        current = null;
        activeSequence = null;
        activeSequenceEntry = -1;
    }

    public static void playSequence(ActionbarSequence sequence) {
        stop();
        if (sequence == null) return;
        ActionbarSequence copy = sequence.copy();
        copy.normalize();
        if (copy.entries.isEmpty()) return;
        activeSequence = copy;
        sequenceStartedAtTick = clientTick;
        updateSequence();
    }

    private static void updateSequence() {
        if (activeSequence == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        int elapsedTicks = Math.max(0, clientTick - sequenceStartedAtTick);
        long cursor = 0;
        for (int i = 0; i < activeSequence.entries.size(); i++) {
            ActionbarSequence.Entry entry = activeSequence.entries.get(i);
            long start = cursor + entry.pauseBefore;
            long visibleEnd = start + entry.fadeIn + entry.stay + entry.fadeOut;
            long end = visibleEnd + entry.pauseAfter;
            if (elapsedTicks < start) {
                current = null;
                activeSequenceEntry = -1;
                return;
            }
            if (elapsedTicks < visibleEnd) {
                if (activeSequenceEntry != i) {
                    activeSequenceEntry = i;
                    current = resolvePlaceholders(entry.definition);
                    HeadTextureCache.get(current.head);
                    AbmHistory.add(current);
                    fadeInTicks = entry.fadeIn;
                    stayTicks = entry.stay;
                    fadeOutTicks = entry.fadeOut;
                    startedAtTick = sequenceStartedAtTick + (int) start;
                }
                return;
            }
            if (elapsedTicks < end) {
                current = null;
                activeSequenceEntry = -1;
                return;
            }
            cursor = end;
        }
        stop();
    }

    public static void play(ActionbarDefinition definition, int fadeIn, int stay, int fadeOut) {
        stop();
        current = definition.copy();
        current.normalize();
        current = resolvePlaceholders(current);
        fadeInTicks = Math.max(0, fadeIn);
        stayTicks = Math.max(1, stay);
        fadeOutTicks = Math.max(0, fadeOut);
        startedAtTick = clientTick;
        HeadTextureCache.get(current.head);
        // Keep an immutable snapshot of what was actually shown, including resolved placeholders.
        AbmHistory.add(current);
    }

    public static boolean isActive() {
        updateSequence();
        if (current == null) {
            return false;
        }
        int elapsed = clientTick - startedAtTick;
        return elapsed < (fadeInTicks + stayTicks + fadeOutTicks);
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
        renderActionbar(graphics, copy, centerX, y, alpha, false, true);
    }

    private static float getAlpha() {
        int elapsed = clientTick - startedAtTick;
        if (fadeInTicks > 0 && elapsed < fadeInTicks) {
            return clamp(elapsed / (float) fadeInTicks);
        }
        if (elapsed < fadeInTicks + stayTicks) {
            return 1.0F;
        }
        if (fadeOutTicks > 0 && elapsed < fadeInTicks + stayTicks + fadeOutTicks) {
            return clamp(1.0F - ((elapsed - fadeInTicks - stayTicks) / (float) fadeOutTicks));
        }
        return fadeOutTicks <= 0 ? 1.0F : 0.0F;
    }

    public static void renderHistoryEntry(GuiGraphics graphics, ActionbarDefinition definition, int centerX, int topY) {
        if (definition == null) {
            return;
        }
        ActionbarDefinition copy = definition.copy();
        copy.normalize();
        renderActionbar(graphics, copy, centerX, topY, 1.0F, false, false);
    }

    private static void renderActionbar(GuiGraphics graphics, ActionbarDefinition definition, int centerX, int topY, float alpha) {
        renderActionbar(graphics, definition, centerX, topY, alpha, false, false);
    }

    private static void renderActionbar(GuiGraphics graphics, ActionbarDefinition definition, int centerX, int topY, float alpha, boolean reserveHeadSlot, boolean resolveText) {
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
        ActionbarDefinition resolved = resolveText ? resolvePlaceholders(definition) : definition;
        int textWidth = 0;
        for (ActionbarElement element : resolved.elements) {
            textWidth += font.width(element.toComponent());
        }

        boolean hasHead = resolved.head != null && !resolved.head.isBlank() && HeadTextureCache.get(resolved.head) != null;
        int headSize = (hasHead || reserveHeadSlot) ? 16 : 0;
        int gap = (reserveHeadSlot || (hasHead && textWidth > 0)) ? 4 : 0;
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
        if (headSize > 0) {
            if (hasHead) {
                ResourceLocation texture = HeadTextureCache.get(resolved.head);
                if (texture != null) {
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                    graphics.blit(texture, drawX, headTop, 0, 0, headSize, headSize, headSize, headSize);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            drawX += headSize + gap;
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

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
