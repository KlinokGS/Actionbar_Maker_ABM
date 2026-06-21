package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmColors;
import net.klinok.actionbar_maker.client.AbmFormatCodes;
import net.klinok.actionbar_maker.client.AbmOverlay;
import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AbmRichTextScreen extends AbmCompactScreen {
    private static final int MAX_PANEL_WIDTH = 760;
    private static final int MIN_SIDE_MARGIN = 16;
    private static final int ROW_HEIGHT = 34;
    private static final int STEP = 4;
    private static final int NEW_ROW = ROW_HEIGHT + STEP;

    private final AbmEditorScreen parent;
    private final ActionbarDefinition working;
    private EditBox textBox;

    public AbmRichTextScreen(AbmEditorScreen parent, ActionbarDefinition definition) {
        super(AbmText.component("screen.rich_text.title"));
        this.parent = parent;
        this.working = definition == null ? ActionbarDefinition.createDefault("new_actionbar") : definition.copy();
        this.working.normalize();
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        int y = 32;

        textBox = new EditBox(this.font, left, y, panelWidth, 20, AbmText.component("field.rich_text"));
        textBox.setMaxLength(1024);
        textBox.setValue(AbmFormatCodes.serialize(working.elements));
        addRenderableWidget(textBox);
        y += NEW_ROW;

        int styleW = 64;
        int styleGap = 6;
        int styleCount = 6;
        int styleTotal = styleW * styleCount + styleGap * (styleCount - 1);
        int styleX = left + Math.max(0, (panelWidth - styleTotal) / 2);
        addRenderableWidget(Button.builder(Component.literal("&l B"), button -> insertCode("&l", true))
                .bounds(styleX, y, styleW, 18).build());
        styleX += styleW + styleGap;
        addRenderableWidget(Button.builder(Component.literal("&o I"), button -> insertCode("&o", true))
                .bounds(styleX, y, styleW, 18).build());
        styleX += styleW + styleGap;
        addRenderableWidget(Button.builder(Component.literal("&n U"), button -> insertCode("&n", true))
                .bounds(styleX, y, styleW, 18).build());
        styleX += styleW + styleGap;
        addRenderableWidget(Button.builder(Component.literal("&m S"), button -> insertCode("&m", true))
                .bounds(styleX, y, styleW, 18).build());
        styleX += styleW + styleGap;
        addRenderableWidget(Button.builder(Component.literal("&k ?"), button -> insertCode("&k", true))
                .bounds(styleX, y, styleW, 18).build());
        styleX += styleW + styleGap;
        addRenderableWidget(Button.builder(Component.literal("&r"), button -> insertCode("&r", true))
                .bounds(styleX, y, styleW, 18).build());
        y += ROW_HEIGHT;

        int colorCols = panelWidth >= 650 ? 8 : 4;
        int gap = 5;
        int colorW = (panelWidth - gap * (colorCols - 1)) / colorCols;
        int colorH = 18;
        for (int i = 0; i < AbmColors.COLORS.length; i++) {
            int row = i / colorCols;
            int col = i % colorCols;
            int x = left + col * (colorW + gap);
            int cy = y + row * (colorH + 4);
            String label = "&" + AbmColors.FORMAT_CODES[i] + " " + AbmText.string(AbmColors.SHORT_COLOR_KEYS[i]);
            String code = "&" + AbmColors.FORMAT_CODES[i];
            addRenderableWidget(Button.builder(Component.literal(label), button -> insertCode(code, true))
                    .bounds(x, cy, colorW, colorH).build());
        }

        int bottomY = getBottomButtonsY();
        int buttonW = (panelWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(AbmText.component("button.apply_back"), button -> applyAndBack())
                .bounds(left, bottomY, buttonW, 20).build());
        addRenderableWidget(Button.builder(AbmText.component("button.reset_rich_text"), button -> resetCodes())
                .bounds(left + buttonW + gap, bottomY, buttonW, 20).build());
        addRenderableWidget(Button.builder(AbmText.component("button.back"), button -> onClose())
                .bounds(left + (buttonW + gap) * 2, bottomY, panelWidth - (buttonW + gap) * 2, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        beginCompactRender(graphics);
        int virtualMouseX = virtualMouseX(mouseX);
        int virtualMouseY = virtualMouseY(mouseY);

        int left = getPanelLeft();
        int panelWidth = getPanelWidth();

        graphics.drawCenteredString(this.font, this.title, abmWidth() / 2, 8, 0xFFFFFF);
        graphics.drawString(this.font, AbmText.component("rich_text.code_hint"), left, 20, 0xAAAAAA, false);
        graphics.drawString(this.font, AbmText.component("rich_text.toolbar_hint"), left, 58, 0x777777, false);
        graphics.drawString(this.font, AbmText.component("rich_text.colors_hint"), left, 94, 0xAAAAAA, false);

        int previewY = getPreviewY();
        graphics.drawString(this.font, AbmText.component("rich_text.preview"), left, previewY - 18, 0xAAAAAA, false);
        ActionbarDefinition preview = working.copy();
        preview.elements = AbmFormatCodes.parse(textBox == null ? "" : textBox.getValue());
        preview.normalize();
        AbmOverlay.renderPreview(graphics, preview, abmWidth() / 2, previewY, 1.0F);

        int helpY = getBottomButtonsY() - 42;
        if (helpY > previewY + 18) {
            graphics.drawString(this.font, AbmText.component("rich_text.codes_help_1"), left, helpY, 0x777777, false);
            graphics.drawString(this.font, AbmText.component("rich_text.codes_help_2"), left, helpY + 12, 0x777777, false);
        }

        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);
        endCompactRender(graphics);
    }

    private void insertCode(String code, boolean wrapSelection) {
        if (textBox == null || code == null || code.isEmpty()) {
            return;
        }
        int[] range = getDirectSelectedRange();
        String value = textBox.getValue();
        if (wrapSelection && range[0] >= 0 && range[0] < range[1]) {
            String selected = value.substring(range[0], range[1]);
            String replacement = code + selected + "&r";
            String updated = value.substring(0, range[0]) + replacement + value.substring(range[1]);
            textBox.setValue(updated);
            int cursor = range[0] + replacement.length();
            textBox.setCursorPosition(cursor);
            textBox.setHighlightPos(cursor);
        } else {
            textBox.insertText(code);
        }
    }

    private int[] getDirectSelectedRange() {
        String text = textBox == null ? "" : textBox.getValue();
        String selected = textBox == null ? "" : textBox.getHighlighted();
        if (selected == null || selected.isEmpty()) {
            return new int[]{-1, -1};
        }

        int cursor = textBox.getCursorPosition();
        int length = selected.length();
        if (cursor >= length && text.substring(cursor - length, cursor).equals(selected)) {
            return new int[]{cursor - length, cursor};
        }
        if (cursor + length <= text.length() && text.substring(cursor, cursor + length).equals(selected)) {
            return new int[]{cursor, cursor + length};
        }

        int first = text.indexOf(selected);
        int last = text.lastIndexOf(selected);
        if (first >= 0 && first == last) {
            return new int[]{first, first + length};
        }
        return new int[]{-1, -1};
    }

    private void resetCodes() {
        if (textBox == null) {
            return;
        }
        String plain = AbmFormatCodes.stripCodes(textBox.getValue());
        textBox.setValue(plain);
        textBox.setCursorPosition(plain.length());
        textBox.setHighlightPos(plain.length());
    }

    private void applyAndBack() {
        working.elements = copyElements(AbmFormatCodes.parse(textBox == null ? "" : textBox.getValue()));
        working.normalize();
        parent.replaceDefinition(working);
        this.minecraft.setScreen(parent);
    }

    private List<ActionbarElement> copyElements(List<ActionbarElement> source) {
        List<ActionbarElement> copy = new ArrayList<>();
        for (ActionbarElement element : source) {
            copy.add(element.copy());
        }
        return copy;
    }

    private int getPreviewY() {
        return Math.min(abmHeight() - 84, 190);
    }

    private int getBottomButtonsY() {
        return abmHeight() - 48;
    }

    private int getPanelWidth() {
        return Math.max(300, Math.min(MAX_PANEL_WIDTH, abmWidth() - MIN_SIDE_MARGIN * 2));
    }

    private int getPanelLeft() {
        return (abmWidth() - getPanelWidth()) / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
