package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarSequence;
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
    private final List<ActionbarSequence> sequences = new ArrayList<>();
    private boolean showingSequences = false;
    private int page = 0;

    public AbmManagerScreen(List<ActionbarDefinition> actionbars, List<ActionbarDefinition> templates, List<ActionbarSequence> sequences) {
        super(AbmText.component("screen.manager.title"));
        this.actionbars = new ArrayList<>();
        if (actionbars != null) {
            actionbars.forEach(definition -> this.actionbars.add(definition.copy()));
        }
        this.templates = new ArrayList<>();
        if (templates != null) {
            templates.forEach(definition -> this.templates.add(definition.copy()));
        }
        if (sequences != null) sequences.forEach(sequence -> this.sequences.add(sequence.copy()));
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        int rows = getRowsPerPage();
        int itemCount = showingSequences ? sequences.size() : actionbars.size();
        int start = page * rows;
        int end = Math.min(itemCount, start + rows);

        int deleteW = 78;
        int editW = 62;
        int gap = 8;
        int nameW = Math.max(120, panelWidth - deleteW - gap);
        int sequenceNameW = Math.max(120, panelWidth - 156);

        for (int i = start; i < end; i++) {
            if (showingSequences) {
                ActionbarSequence sequence = sequences.get(i);
                int rowY = LAYOUT_TOP + (i - start) * 28;
                addRenderableWidget(Button.builder(Component.literal(sequence.name + "  (" + sequence.entries.size() + ")"), b ->
                        minecraft.setScreen(new AbmSequenceEditorScreen(sequence, this)))
                        .bounds(left, rowY, sequenceNameW + gap + editW, 20).build());
                addRenderableWidget(Button.builder(AbmText.component("button.delete"), b -> {
                    AbmNetwork.sendToServer(new C2SDeleteActionbarPacket(sequence.name));
                    sequences.remove(sequence);
                    page = Math.min(page, Math.max(0, (sequences.size() - 1) / rows));
                    refresh();
                }).bounds(left + sequenceNameW + gap + editW + gap, rowY, deleteW, 20).build());
                continue;
            }
            ActionbarDefinition definition = actionbars.get(i);
            int y = LAYOUT_TOP + (i - start) * 28;
            addRenderableWidget(Button.builder(Component.literal(definition.name), button ->
                    this.minecraft.setScreen(new AbmEditorScreen(definition.copy(), templates, this)))
                    .bounds(left, y, nameW, 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.delete"), button -> {
                AbmNetwork.sendToServer(new C2SDeleteActionbarPacket(definition.name));
                actionbars.remove(definition);
                int maxPage = Math.max(0, (actionbars.size() - 1) / rows);
                page = Math.min(page, maxPage);
                refresh();
            }).bounds(left + nameW + gap, y, deleteW, 20).build());
        }

        int bottomY = abmHeight() - 28;
        int createW = 90;
        int closeW = 82;
        addRenderableWidget(Button.builder(AbmText.component("button.create"), button -> {
            if (showingSequences) {
                this.minecraft.setScreen(new AbmSequenceEditorScreen((ActionbarSequence) null, this));
            } else {
                ActionbarDefinition created = ActionbarDefinition.createDefault("");
                this.minecraft.setScreen(new AbmEditorScreen(created, templates, this));
            }
        }).bounds(left, bottomY, createW, 20).build());

        addRenderableWidget(Button.builder(AbmText.component("button.close"), button -> this.onClose())
                .bounds(left + panelWidth - closeW, bottomY, closeW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            refresh();
        }).bounds(abmWidth() / 2 - 92, bottomY, 32, 20).build());

        addRenderableWidget(Button.builder(Component.translatable(showingSequences ? "actionbar_maker.manager.show_actionbars" : "actionbar_maker.manager.show_sequences"), button -> {
            showingSequences = !showingSequences; page = 0; refresh();
        }).bounds(abmWidth() / 2 - 56, bottomY, 112, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int maxPage = Math.max(0, (itemCount - 1) / rows);
            page = Math.min(maxPage, page + 1);
            refresh();
        }).bounds(abmWidth() / 2 + 60, bottomY, 32, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        beginCompactRender(graphics);
        int virtualMouseX = virtualMouseX(mouseX);
        int virtualMouseY = virtualMouseY(mouseY);
        graphics.drawCenteredString(this.font, this.title, abmWidth() / 2, 12, 0xFFFFFF);

        int itemCount = showingSequences ? sequences.size() : actionbars.size();
        if (itemCount == 0) {
            graphics.drawCenteredString(this.font, AbmText.component(showingSequences ? "manager.sequences_empty" : "manager.empty"), abmWidth() / 2, 55, 0xAAAAAA);
            graphics.drawCenteredString(this.font, AbmText.component(showingSequences ? "manager.sequences_empty_hint" : "manager.empty_hint"), abmWidth() / 2, 69, 0x888888);
        }

        int rows = getRowsPerPage();
        int maxPage = Math.max(0, (itemCount - 1) / rows);
        int bottomY = abmHeight() - 28;
        graphics.drawCenteredString(this.font, AbmText.component(showingSequences ? "manager.sequence_path" : "manager.world_paths"), abmWidth() / 2, bottomY - 38, 0x777777);
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

    public void addSequence(ActionbarSequence sequence) {
        for (int i = 0; i < sequences.size(); i++) {
            if (sequences.get(i).name.equalsIgnoreCase(sequence.name)) { sequences.set(i, sequence.copy()); refresh(); return; }
        }
        sequences.add(sequence.copy());
        if (showingSequences) refresh();
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
