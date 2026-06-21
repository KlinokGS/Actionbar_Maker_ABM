package net.klinok.actionbar_maker.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbmCompactScreen extends Screen {
    private float abmUiScale = 1.0F;
    private int abmVirtualWidth = 0;
    private int abmVirtualHeight = 0;

    protected AbmCompactScreen(Component title) {
        super(title);
    }

    protected final void prepareCompactLayout() {
        Minecraft minecraft = Minecraft.getInstance();
        double currentScale = minecraft.getWindow().getGuiScale();
        if (currentScale > 1.0D) {
            double targetScale = Math.max(1.0D, currentScale - 1.0D);
            abmUiScale = (float) (targetScale / currentScale);
        } else {
            abmUiScale = 1.0F;
        }

        abmVirtualWidth = Math.max(1, Math.round(this.width / abmUiScale));
        abmVirtualHeight = Math.max(1, Math.round(this.height / abmUiScale));
    }

    protected final int abmWidth() {
        if (abmVirtualWidth <= 0) {
            prepareCompactLayout();
        }
        return abmVirtualWidth;
    }

    protected final int abmHeight() {
        if (abmVirtualHeight <= 0) {
            prepareCompactLayout();
        }
        return abmVirtualHeight;
    }

    protected final float abmScale() {
        if (abmVirtualWidth <= 0 || abmVirtualHeight <= 0) {
            prepareCompactLayout();
        }
        return abmUiScale;
    }

    protected final int virtualMouseX(int mouseX) {
        return Math.round(mouseX / abmScale());
    }

    protected final int virtualMouseY(int mouseY) {
        return Math.round(mouseY / abmScale());
    }

    protected final void beginCompactRender(GuiGraphics graphics) {
        graphics.pose().pushPose();
        float scale = abmScale();
        graphics.pose().scale(scale, scale, 1.0F);
    }

    protected final void endCompactRender(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / abmScale(), mouseY / abmScale(), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / abmScale(), mouseY / abmScale(), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float scale = abmScale();
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX / abmScale(), mouseY / abmScale(), delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
