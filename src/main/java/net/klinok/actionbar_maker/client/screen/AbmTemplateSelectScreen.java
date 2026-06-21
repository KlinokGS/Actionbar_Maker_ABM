package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.client.ClientFileHelper;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AbmTemplateSelectScreen extends AbmCompactScreen {
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MIN_SIDE_MARGIN = 16;
    private static final int ROW_HEIGHT = 24;
    private static final int Y_BASE = 54;

    private final AbmEditorScreen parent;
    private final List<ActionbarDefinition> templates = new ArrayList<>();
    private int page = 0;

    public AbmTemplateSelectScreen(AbmEditorScreen parent) {
        super(AbmText.component("screen.template_select.title"));
        this.parent = parent;
        reloadTemplates();
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        reloadTemplates();
        clampPage();

        int left = getPanelLeft();
        int panelWidth = getPanelWidth();

        int visibleRows = getVisibleRows();
        int start = page * visibleRows;
        int end = Math.min(templates.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            ActionbarDefinition template = templates.get(i);
            int rowY = Y_BASE + (i - start) * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal(template.name), button -> selectTemplate(template))
                    .bounds(left, rowY, panelWidth, 20).build());
        }

        int bottomY = getBottomButtonsY();
        int gap = 8;
        int buttonW = (panelWidth - gap * 3) / 4;
        addRenderableWidget(Button.builder(AbmText.component("button.refresh"), button -> {
            reloadTemplates();
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

    private void reloadTemplates() {
        Map<String, ActionbarDefinition> merged = new LinkedHashMap<>();
        if (parent != null && parent.getTemplates() != null) {
            for (ActionbarDefinition template : parent.getTemplates()) {
                putTemplate(merged, template);
            }
        }
        for (ActionbarDefinition template : ClientFileHelper.listLocalTemplates()) {
            putTemplate(merged, template);
        }
        templates.clear();
        templates.addAll(merged.values());
    }

    private void putTemplate(Map<String, ActionbarDefinition> merged, ActionbarDefinition template) {
        if (template == null) {
            return;
        }
        ActionbarDefinition copy = template.copy();
        copy.normalize();
        String key = copy.name == null || copy.name.isBlank() ? "template_" + merged.size() : copy.name.toLowerCase(Locale.ROOT);
        merged.putIfAbsent(key, copy);
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
        graphics.drawString(this.font, AbmText.component("template.folder"), left, 28, 0xAAAAAA, false);
        graphics.drawString(this.font, AbmText.component("template.hint"), left, 40, 0x777777, false);

        if (templates.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("template.empty"), abmWidth() / 2, 94, 0xAAAAAA);
            graphics.drawCenteredString(this.font, AbmText.component("template.empty_hint"), abmWidth() / 2, 108, 0x777777);
        }

        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);

        int visibleRows = getVisibleRows();
        int start = page * visibleRows;
        int end = Math.min(templates.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            ActionbarDefinition template = templates.get(i);
            int rowY = Y_BASE + (i - start) * ROW_HEIGHT + 6;
            String preview = buildPreview(template);
            if (!preview.isBlank()) {
                int previewX = left + panelWidth - this.font.width(preview) - 8;
                graphics.drawString(this.font, preview, previewX, rowY, 0x999999, false);
            }
        }

        if (!templates.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("manager.page", page + 1, getMaxPage() + 1), left + panelWidth / 2, getBottomButtonsY() - 14, 0x777777);
        }
        endCompactRender(graphics);
    }

    private String buildPreview(ActionbarDefinition template) {
        if (template == null || template.elements == null || template.elements.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ActionbarElement element : template.elements) {
            if (element != null && element.text != null) {
                builder.append(element.text);
            }
        }
        String value = builder.toString().replace('\n', ' ').trim();
        if (value.length() > ROW_HEIGHT) {
            return value.substring(0, ROW_HEIGHT) + "...";
        }
        return value;
    }

    private void selectTemplate(ActionbarDefinition template) {
        parent.applyTemplate(template);
        Minecraft.getInstance().setScreen(parent);
    }

    private int getVisibleRows() {
        int available = Math.max(ROW_HEIGHT, getBottomButtonsY() - 54 - ROW_HEIGHT);
        return Math.max(1, Math.min(12, available / ROW_HEIGHT));
    }

    private int getMaxPage() {
        int rows = getVisibleRows();
        return Math.max(0, (templates.size() - 1) / rows);
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
