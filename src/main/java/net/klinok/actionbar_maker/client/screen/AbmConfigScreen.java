package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmConfig;
import net.klinok.actionbar_maker.client.AbmText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AbmConfigScreen extends AbmCompactScreen {
    private static final int FIELD_WIDTH = 42;
    private static final int FIELD_GAP = 12;
    private static final int BTN_H = 20;
    private static final int LABEL_GAP = 5;

    private final Screen parent;
    private EditBox[] actionBarBoxes;
    private EditBox[] sequenceBoxes;
    private EditBox pauseBeforeBox;
    private EditBox pauseAfterBox;

    public AbmConfigScreen(Screen parent) {
        super(AbmText.component("screen.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        prepareCompactLayout();

        Component[] timingLabels = timingLabels();
        Component labelPauseBefore = AbmText.component("config.pause_before");
        Component labelPauseAfter = AbmText.component("config.pause_after");

        int timingRowW = rowWidth(timingLabels);
        int pauseRowW = rowWidth(new Component[]{labelPauseBefore, labelPauseAfter});
        int panelWidth = Math.max(240, Math.max(timingRowW, pauseRowW));

        int top = 36;


        int row1Y = top + 22;
        actionBarBoxes = addCenteredRow(timingLabels, row1Y, timingRowW, new int[]{
                AbmConfig.actionbarDefaultFadeIn(),
                AbmConfig.actionbarDefaultStay(),
                AbmConfig.actionbarDefaultFadeOut()
        });

        int row2Y = row1Y + BTN_H + 30;
        sequenceBoxes = addCenteredRow(timingLabels, row2Y, timingRowW, new int[]{
                AbmConfig.sequenceDefaultFadeIn(),
                AbmConfig.sequenceDefaultStay(),
                AbmConfig.sequenceDefaultFadeOut()
        });

        int pauseY = row2Y + BTN_H + 24;
        EditBox[] pauseBoxes = addCenteredRow(new Component[]{labelPauseBefore, labelPauseAfter}, pauseY, pauseRowW,
                new int[]{AbmConfig.sequenceDefaultPauseBefore(), AbmConfig.sequenceDefaultPauseAfter()});
        pauseBeforeBox = pauseBoxes[0];
        pauseAfterBox = pauseBoxes[1];

        boolean show = AbmConfig.uiShowSettingsButton();
        int left = panelLeft(panelWidth);
        addRenderableWidget(Button.builder(AbmText.component(show ? "config.show_settings_on" : "config.show_settings_off"), b -> {
            syncFromFields();
            try {
                AbmConfig.UI_SHOW_SETTINGS_BUTTON.set(!show);
            } catch (Exception ignored) {
            }
            this.minecraft.setScreen(new AbmConfigScreen(parent));
        }).bounds(left, pauseY + BTN_H + 12, panelWidth, BTN_H).build());

        addRenderableWidget(Button.builder(AbmText.component("button.back"), b -> this.onClose())
                .bounds(left, pauseY + BTN_H + 12 + BTN_H + 6, panelWidth, BTN_H).build());
    }

    private Component[] timingLabels() {
        return new Component[]{
                AbmText.component("label.fade_in"),
                AbmText.component("label.stay"),
                AbmText.component("label.fade_out")
        };
    }

    /** Raw width */
    private int rowWidth(Component[] labels) {
        int width = 0;
        for (int i = 0; i < labels.length; i++) {
            width += this.font.width(labels[i]) + LABEL_GAP + FIELD_WIDTH;
            if (i < labels.length - 1) {
                width += FIELD_GAP;
            }
        }
        return width;
    }

    /** Left side of panel */
    private int panelLeft(int panelWidth) {
        return (abmWidth() - panelWidth) / 2;
    }

    /** Centered row inside of a panel */
    private EditBox[] addCenteredRow(Component[] labels, int y, int rowWidth, int[] values) {
        int panelWidth = Math.max(240, Math.max(rowWidth(timingLabels()),
                rowWidth(new Component[]{AbmText.component("config.pause_before"), AbmText.component("config.pause_after")})));
        int x = panelLeft(panelWidth) + (panelWidth - rowWidth) / 2;
        EditBox[] boxes = new EditBox[labels.length];
        for (int i = 0; i < labels.length; i++) {
            x += this.font.width(labels[i]) + LABEL_GAP;
            boxes[i] = numberBox(x, y, FIELD_WIDTH);
            boxes[i].setValue(Integer.toString(values[i]));
            addRenderableWidget(boxes[i]);
            x += FIELD_WIDTH + FIELD_GAP;
        }
        return boxes;
    }

    private EditBox numberBox(int x, int y, int width) {
        EditBox box = new EditBox(this.font, x, y, width, BTN_H, Component.empty());
        box.setMaxLength(4);
        box.setFilter(s -> s.matches("[0-9]*"));
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        beginCompactRender(graphics);
        int virtualMouseX = virtualMouseX(mouseX);
        int virtualMouseY = virtualMouseY(mouseY);
        int centerX = abmWidth() / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);

        // Section titles
        drawScaledHeader(graphics, AbmText.component("config.section_actionbar"), centerX,
                actionBarBoxes == null ? row1Y() - 18 : actionBarBoxes[0].getY() - 18);
        drawScaledHeader(graphics, AbmText.component("config.section_sequence"), centerX,
                sequenceBoxes == null ? row2Y() - 18 : sequenceBoxes[0].getY() - 18);

        drawFieldLabels(graphics, actionBarBoxes, timingLabels());
        drawFieldLabels(graphics, sequenceBoxes, timingLabels());
        drawLabel(graphics, pauseBeforeBox, AbmText.component("config.pause_before"));
        drawLabel(graphics, pauseAfterBox, AbmText.component("config.pause_after"));

        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);
        endCompactRender(graphics);
    }

    /** Title sections setting */
    private void drawScaledHeader(GuiGraphics graphics, Component text, int centerX, int y) {
        float scale = 1.2f;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(this.font, text, (int) (centerX / scale), (int) (y / scale), 0xFFFF55);
        graphics.pose().popPose();
    }

    private int row1Y() {
        return 36 + 22;
    }

    private int row2Y() {
        return row1Y() + BTN_H + 30;
    }

    private void drawFieldLabels(GuiGraphics graphics, EditBox[] boxes, Component[] labels) {
        if (boxes == null) {
            return;
        }
        for (int i = 0; i < boxes.length; i++) {
            drawLabel(graphics, boxes[i], labels[i]);
        }
    }

    private void drawLabel(GuiGraphics graphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        graphics.drawString(this.font, label, box.getX() - LABEL_GAP - this.font.width(label), box.getY() + 6, 0xAAAAAA, false);
    }

    private void syncFromFields() {
        try {
            AbmConfig.ACTIONBAR_DEFAULT_FADE_IN.set(clamp(parse(boxAt(actionBarBoxes, 0)), 0, 200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.ACTIONBAR_DEFAULT_STAY.set(clamp(parse(boxAt(actionBarBoxes, 1)), 1, 1200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.ACTIONBAR_DEFAULT_FADE_OUT.set(clamp(parse(boxAt(actionBarBoxes, 2)), 0, 200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.SEQUENCE_DEFAULT_FADE_IN.set(clamp(parse(boxAt(sequenceBoxes, 0)), 0, 200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.SEQUENCE_DEFAULT_STAY.set(clamp(parse(boxAt(sequenceBoxes, 1)), 1, 1200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.SEQUENCE_DEFAULT_FADE_OUT.set(clamp(parse(boxAt(sequenceBoxes, 2)), 0, 200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.SEQUENCE_DEFAULT_PAUSE_BEFORE.set(clamp(parse(pauseBeforeBox), 0, 1200));
        } catch (Exception ignored) {
        }
        try {
            AbmConfig.SEQUENCE_DEFAULT_PAUSE_AFTER.set(clamp(parse(pauseAfterBox), 0, 1200));
        } catch (Exception ignored) {
        }
    }

    private static EditBox boxAt(EditBox[] boxes, int index) {
        return boxes == null ? null : boxes[index];
    }

    /** Check if null or not number → 0. */
    private static int parse(EditBox box) {
        if (box == null) {
            return 0;
        }
        String raw = box.getValue().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        syncFromFields();
        if (parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
