package net.klinok.actionbar_maker.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.client.ClientFileHelper;
import net.klinok.actionbar_maker.client.HeadTextureCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AbmHeadSelectScreen extends AbmCompactScreen {
    private static final int MAX_PANEL_WIDTH = 520;
    private static final int MIN_SIDE_MARGIN = 16;
    private static final int ROW_HEIGHT = 24;
    private static final int PREVIEW_SIZE = 20;

    private final AbmEditorScreen parent;
    private List<String> heads = new ArrayList<>();
    private int page = 0;

    public AbmHeadSelectScreen(AbmEditorScreen parent) {
        super(AbmText.component("screen.head_select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        ClientFileHelper.ensureClientDirectories();
        heads = new ArrayList<>(ClientFileHelper.listHeads());
        clampPage();

        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        int y = 48;

        addRenderableWidget(Button.builder(parent.getSelectedHead().isBlank()
                        ? AbmText.component("head.none.selected")
                        : AbmText.component("head.none"), button -> selectHead(""))
                .bounds(left, y, panelWidth, 20).build());
        y += 30;

        int visibleRows = getVisibleRows();
        int start = page * visibleRows;
        int end = Math.min(heads.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            String fileName = heads.get(i);
            int rowY = y + (i - start) * ROW_HEIGHT;
            String selectedPrefix = fileName.equals(parent.getSelectedHead()) ? "✓ " : "";
            addRenderableWidget(Button.builder(Component.literal(selectedPrefix + fileName), button -> selectHead(fileName))
                    .bounds(left + PREVIEW_SIZE + 8, rowY, panelWidth - PREVIEW_SIZE - 8, 20).build());
        }

        int bottomY = getBottomButtonsY();
        int gap = 8;
        int buttonW = (panelWidth - gap * 3) / 4;
        addRenderableWidget(Button.builder(AbmText.component("button.refresh"), button -> {
            HeadTextureCache.clear();
            refresh();
        }).bounds(left, bottomY, buttonW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            refresh();
        }).bounds(left + buttonW + gap, bottomY, buttonW, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(getMaxPage(), page + 1);
            refresh();
        }).bounds(left + (buttonW + gap) * 2, bottomY, buttonW, 20).build());

        addRenderableWidget(Button.builder(AbmText.component("button.back"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + (buttonW + gap) * 3, bottomY, panelWidth - (buttonW + gap) * 3, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        beginCompactRender(graphics);
        int virtualMouseX = virtualMouseX(mouseX);
        int virtualMouseY = virtualMouseY(mouseY);
        graphics.drawCenteredString(this.font, this.title, abmWidth() / 2, 8, 0xFFFFFF);

        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        graphics.drawString(this.font, AbmText.component("head.folder"), left, 28, 0xAAAAAA, false);

        if (heads.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("head.empty"), abmWidth() / 2, 94, 0xAAAAAA);
            graphics.drawCenteredString(this.font, AbmText.component("head.empty_hint"), abmWidth() / 2, 108, 0x777777);
        }

        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);

        int visibleRows = getVisibleRows();
        int start = page * visibleRows;
        int end = Math.min(heads.size(), start + visibleRows);
        int y = 78;
        for (int i = start; i < end; i++) {
            String fileName = heads.get(i);
            int rowY = y + (i - start) * ROW_HEIGHT;
            drawPreview(graphics, fileName, left + 1, rowY);
        }

        if (!heads.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("manager.page", page + 1, getMaxPage() + 1), left + panelWidth / 2, getBottomButtonsY() - 14, 0x777777);
        }
        endCompactRender(graphics);
    }

    private void drawPreview(GuiGraphics graphics, String fileName, int x, int y) {
        ResourceLocation texture = HeadTextureCache.get(fileName);
        if (texture == null) {
            graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0x66000000);
            graphics.drawCenteredString(this.font, "?", x + PREVIEW_SIZE / 2, y + 6, 0xFF5555);
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, x, y, 0, 0, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE);
    }

    private void selectHead(String fileName) {
        parent.setSelectedHead(fileName);
        Minecraft.getInstance().setScreen(parent);
    }

    private int getVisibleRows() {
        int available = Math.max(ROW_HEIGHT, getBottomButtonsY() - 78 - 24);
        return Math.max(1, Math.min(10, available / ROW_HEIGHT));
    }

    private int getMaxPage() {
        int rows = getVisibleRows();
        return Math.max(0, (heads.size() - 1) / rows);
    }

    private void clampPage() {
        page = Math.max(0, Math.min(page, getMaxPage()));
    }

    private int getBottomButtonsY() {
        return abmHeight() < 180 ? abmHeight() - 28 : abmHeight() - 44;
    }

    private int getPanelWidth() {
        return Math.max(280, Math.min(MAX_PANEL_WIDTH, abmWidth() - MIN_SIDE_MARGIN * 2));
    }

    private int getPanelLeft() {
        return (abmWidth() - getPanelWidth()) / 2;
    }

    private void refresh() {
        clearWidgets();
        init();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
