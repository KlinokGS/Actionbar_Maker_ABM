package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.network.C2SDeleteActionbarPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AbmManagerScreen extends AbmCompactScreen {
    private static final int MAX_PANEL_WIDTH = 520;
    private static final int MIN_SIDE_MARGIN = 16;
    private static final int LAYOUT_TOP = 34;

    private final List<ActionbarDefinition> actionbars;
    private final List<ActionbarDefinition> templates;
    private int page = 0;

    public AbmManagerScreen(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates) {
        super(AbmText.component("screen.manager.title"));
        this.actionbars = new ArrayList<>();
        if (actionbars != null) {
            actionbars.forEach(definition -> this.actionbars.add(definition.copy()));
        }
        this.templates = new ArrayList<>();
        if (templates != null) {
            templates.forEach(definition -> this.templates.add(definition.copy()));
        }
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        int rows = getRowsPerPage();
        int start = page * rows;
        int end = Math.min(actionbars.size(), start + rows);

        int nameW = Math.max(120, panelWidth - 156);
        int editW = 62;
        int deleteW = 78;
        int gap = 8;

        for (int i = start; i < end; i++) {
            ActionbarDefinition definition = actionbars.get(i);
            int y = LAYOUT_TOP + (i - start) * 28;
            addRenderableWidget(Button.builder(Component.literal(definition.name), button ->
                    this.minecraft.setScreen(new AbmEditorScreen(definition.copy(), templates, this)))
                    .bounds(left, y, nameW, 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.edit.short"), button ->
                    this.minecraft.setScreen(new AbmEditorScreen(definition.copy(), templates, this)))
                    .bounds(left + nameW + gap, y, editW, 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.delete"), button -> {
                AbmNetwork.sendToServer(new C2SDeleteActionbarPacket(definition.name));
                actionbars.remove(definition);
                int maxPage = Math.max(0, (actionbars.size() - 1) / rows);
                page = Math.min(page, maxPage);
                refresh();
            }).bounds(left + nameW + gap + editW + gap, y, deleteW, 20).build());
        }

        int bottomY = abmHeight() - 28;
        int createW = 90;
        int closeW = 82;
        addRenderableWidget(Button.builder(AbmText.component("button.create"), button -> {
            ActionbarDefinition created = ActionbarDefinition.createDefault("");
            this.minecraft.setScreen(new AbmEditorScreen(created, templates, this));
        }).bounds(left, bottomY, createW, 20).build());

        addRenderableWidget(Button.builder(AbmText.component("button.close"), button -> this.onClose())
                .bounds(left + panelWidth - closeW, bottomY, closeW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            refresh();
        }).bounds(abmWidth() / 2 - 44, bottomY, 32, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int maxPage = Math.max(0, (actionbars.size() - 1) / rows);
            page = Math.min(maxPage, page + 1);
            refresh();
        }).bounds(abmWidth() / 2 + 12, bottomY, 32, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        beginCompactRender(graphics);
        int virtualMouseX = virtualMouseX(mouseX);
        int virtualMouseY = virtualMouseY(mouseY);
        graphics.drawCenteredString(this.font, this.title, abmWidth() / 2, 12, 0xFFFFFF);

        if (actionbars.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("manager.empty"), abmWidth() / 2, 55, 0xAAAAAA);
            graphics.drawCenteredString(this.font, AbmText.component("manager.empty_hint"), abmWidth() / 2, 69, 0x888888);
        }

        int rows = getRowsPerPage();
        int maxPage = Math.max(0, (actionbars.size() - 1) / rows);
        int bottomY = abmHeight() - 28;
        graphics.drawCenteredString(this.font, AbmText.component("manager.world_paths"), abmWidth() / 2, bottomY - 38, 0x777777);
        graphics.drawCenteredString(this.font, AbmText.component("manager.page", page + 1, maxPage + 1), abmWidth() / 2, bottomY - 24, 0xAAAAAA);
        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);
        endCompactRender(graphics);
    }

    private int getRowsPerPage() {
        int footerTop = abmHeight() - 72;
        return Math.max(3, Math.max(1, (footerTop - 34) / 28));
    }

    private int getPanelWidth() {
        return Math.max(280, Math.min(MAX_PANEL_WIDTH, abmWidth() - MIN_SIDE_MARGIN * 2));
    }

    private int getPanelLeft() {
        return (abmWidth() - getPanelWidth()) / 2;
    }

    public void refresh() {
        clearWidgets();
        init();
    }

    public void addActionbar(ActionbarDefinition definition) {
        // Проверяем, нет ли уже такого
        for (int i = 0; i < actionbars.size(); i++) {
            if (actionbars.get(i).name.equals(definition.name)) {
                actionbars.set(i, definition.copy());
                return;
            }
        }
        actionbars.add(definition.copy());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
