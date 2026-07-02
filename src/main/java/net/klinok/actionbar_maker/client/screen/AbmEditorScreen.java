package net.klinok.actionbar_maker.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.klinok.actionbar_maker.client.AbmColors;
import net.klinok.actionbar_maker.client.AbmOverlay;
import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.client.ClientFileHelper;
import net.klinok.actionbar_maker.client.HeadTextureCache;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarElement;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.network.C2SSaveActionbarPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AbmEditorScreen extends AbmCompactScreen {
    private static final int MAX_PANEL_WIDTH = 760;
    private static final int MIN_SIDE_MARGIN = 16;
    private static final int ROW_HEIGHT = 24;
    private static final int TIMING_FIELD_WIDTH = 42;
    private static final int Y_OFFSET_FIELD_WIDTH = 30;
    private static final int RESET_Y_BUTTON_WIDTH = 22;
    private static final int SMALL_GAP = 4;
    private static final int MEDIUM_GAP = 10;
    private static final int ROW_STEP = ROW_HEIGHT + SMALL_GAP;
    private static final int GROUP_GAP = 14;
    private static final ResourceLocation LOGO_ICON = new ResourceLocation("actionbar_maker", "textures/logo_small.png");

    private ActionbarDefinition definition;
    private final List<ActionbarDefinition> templates;
    private final Screen previous;
    private final List<EditBox> elementBoxes = new ArrayList<>();
    private final List<Integer> elementBoxIndexes = new ArrayList<>();
    private EditBox nameBox;
    private EditBox fadeInBox;
    private EditBox stayBox;
    private EditBox fadeOutBox;
    private EditBox textYOffsetBox;
    private int fragmentScroll = 0;

    public AbmEditorScreen(ActionbarDefinition definition, List<ActionbarDefinition> templates, Screen previous) {
        super(AbmText.component("screen.editor.title"));
        this.definition = definition == null ? ActionbarDefinition.createDefault("actionbar") : definition.copy();
        this.definition.normalize();
        this.templates = new ArrayList<>();
        if (templates != null) {
            templates.forEach(template -> this.templates.add(template.copy()));
        }
        this.previous = previous;
    }

    @Override
    protected void init() {
        prepareCompactLayout();
        elementBoxes.clear();
        elementBoxIndexes.clear();
        ClientFileHelper.ensureClientDirectories();
        clampFragmentScroll();

        int left = getPanelLeft();
        int panelWidth = getPanelWidth();
        int y = ROW_HEIGHT + SMALL_GAP;

        int labelWidth = this.font.width(AbmText.component("label.name"));
        int nameBoxX = left + labelWidth + 8;
        int iconSpace = 28;
        int nameBoxWidth = Math.max(150, panelWidth - (nameBoxX - left) - iconSpace);

        nameBox = new EditBox(this.font, nameBoxX, y, nameBoxWidth, 20, AbmText.component("field.name"));
        nameBox.setMaxLength(96);
        nameBox.setValue(definition.name);
        addRenderableWidget(nameBox);
        y += ROW_STEP;

        int gap = 8;
        int backgroundW = 142;
        int reloadW = 132;
        int headW = panelWidth - backgroundW - reloadW - gap * 2;
        if (headW >= 150) {
            addRenderableWidget(Button.builder((definition.background ? AbmText.component("button.background.on") : AbmText.component("button.background.off")), button -> {
                syncFromFields();
                definition.background = !definition.background;
                refresh();
            }).bounds(left, y, backgroundW, 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.head", getHeadLabel()), button -> {
                syncFromFields();
                Minecraft.getInstance().setScreen(new AbmHeadSelectScreen(this));
            }).bounds(left + backgroundW + gap, y, headW, 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.reload_png"), button -> {
                HeadTextureCache.clear();
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(AbmText.component("message.png_cache_reloaded"), false);
                }
            }).bounds(left + backgroundW + gap + headW + gap, y, reloadW, 20).build());
        } else {
            addRenderableWidget(Button.builder((definition.background ? AbmText.component("button.background.on") : AbmText.component("button.background.off")), button -> {
                syncFromFields();
                definition.background = !definition.background;
                refresh();
            }).bounds(left, y, Math.max(90, panelWidth / 2 - gap / 2), 20).build());

            addRenderableWidget(Button.builder(AbmText.component("button.reload_png"), button -> HeadTextureCache.clear())
                    .bounds(left + panelWidth / 2 + gap / 2, y, Math.max(90, panelWidth / 2 - gap / 2), 20).build());
            y += ROW_HEIGHT;

            addRenderableWidget(Button.builder(AbmText.component("button.head", getHeadLabel()), button -> {
                syncFromFields();
                Minecraft.getInstance().setScreen(new AbmHeadSelectScreen(this));
            }).bounds(left, y, panelWidth, 20).build());
        }

        addTimingControls(left, panelWidth, getDurationLabelY());
        addFragmentScrollButtons(left, panelWidth);

        int rowsTop = getRowsTopY();
        int visibleRows = getVisibleRowCount();
        int start = fragmentScroll;
        int end = Math.min(definition.elements.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            addElementRow(i, left, panelWidth, rowsTop + (i - start) * ROW_HEIGHT);
        }

        addBottomButtons(left, panelWidth, getBottomButtonsY());
    }

    private void addTimingControls(int left, int panelWidth, int y) {
        if (useSingleTimingRow()) {
            int[] layout = getTimingLayout(left, panelWidth);
            fadeInBox = smallNumberBox(layout[1], y, definition.defaultFadeIn, TIMING_FIELD_WIDTH);
            stayBox = smallNumberBox(layout[3], y, definition.defaultStay, TIMING_FIELD_WIDTH);
            fadeOutBox = smallNumberBox(layout[5], y, definition.defaultFadeOut, TIMING_FIELD_WIDTH);
            textYOffsetBox = smallSignedNumberBox(layout[7], y, definition.textYOffset);

            addRenderableWidget(Button.builder(Component.literal("0"), button -> {
                syncFromFields();
                definition.textYOffset = 0;
                refresh();
            }).bounds(layout[8], y, RESET_Y_BUTTON_WIDTH, 20).build());
        } else {
            int gap = MEDIUM_GAP;
            int fieldW = TIMING_FIELD_WIDTH;
            int firstRowTotal = this.font.width(AbmText.string("label.fade_in")) + 5 + fieldW + gap
                    + this.font.width(AbmText.string("label.stay")) + 5 + fieldW + gap
                    + this.font.width(AbmText.string("label.fade_out")) + 5 + fieldW;
            int x = left + Math.max(0, (panelWidth - firstRowTotal) / 2);

            fadeInBox = smallNumberBox(x + this.font.width(AbmText.string("label.fade_in")) + 5, y, definition.defaultFadeIn, fieldW);
            x += this.font.width(AbmText.string("label.fade_in")) + 5 + fieldW + gap;
            stayBox = smallNumberBox(x + this.font.width(AbmText.string("label.stay")) + 5, y, definition.defaultStay, fieldW);
            x += this.font.width(AbmText.string("label.stay")) + 5 + fieldW + gap;
            fadeOutBox = smallNumberBox(x + this.font.width(AbmText.string("label.fade_out")) + 5, y, definition.defaultFadeOut, fieldW);

            int secondRowY = y + ROW_HEIGHT;
            int secondRowTotal = this.font.width(AbmText.string("label.y")) + 5 + Y_OFFSET_FIELD_WIDTH + 5 + RESET_Y_BUTTON_WIDTH;
            int secondRowX = left + Math.max(0, (panelWidth - secondRowTotal) / 2);
            textYOffsetBox = smallSignedNumberBox(secondRowX + this.font.width(AbmText.string("label.y")) + 5, secondRowY, definition.textYOffset);
            addRenderableWidget(Button.builder(Component.literal("0"), button -> {
                syncFromFields();
                definition.textYOffset = 0;
                refresh();
            }).bounds(textYOffsetBox.getX() + textYOffsetBox.getWidth() + 5, secondRowY, RESET_Y_BUTTON_WIDTH, 20).build());
        }

        addRenderableWidget(fadeInBox);
        addRenderableWidget(stayBox);
        addRenderableWidget(fadeOutBox);
        addRenderableWidget(textYOffsetBox);
    }

    private void addFragmentScrollButtons(int left, int panelWidth) {
        if (definition.elements.size() <= getVisibleRowCount()) {
            return;
        }
        int y = getRowsTopY() - 20;
        int downX = left + panelWidth - 24;
        int upX = downX - 26;
        addRenderableWidget(Button.builder(Component.literal("▲"), button -> scrollFragments(-1))
                .bounds(upX, y, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), button -> scrollFragments(1))
                .bounds(downX, y, 22, 18).build());
    }

    private void addBottomButtons(int left, int panelWidth, int bottomY) {
        int gap = 8;
        if (panelWidth >= 620) {
            int topButtonW = (panelWidth - gap * 5) / 6;
            int x = left;
            addRenderableWidget(Button.builder(AbmText.component("button.add_fragment"), button -> addFragment())
                    .bounds(x, bottomY, topButtonW, 20).build());
            x += topButtonW + gap;
            addRenderableWidget(Button.builder(AbmText.component("button.rich_text"), button -> openRichTextEditor())
                    .bounds(x, bottomY, topButtonW, 20).build());
            x += topButtonW + gap;
            addRenderableWidget(Button.builder(AbmText.component("button.import_template"), button -> openTemplateSelect())
                    .bounds(x, bottomY, topButtonW, 20).build());
            x += topButtonW + gap;
            addRenderableWidget(Button.builder(AbmText.component("button.save"), button -> save(false))
                    .bounds(x, bottomY, topButtonW, 20).build());
            x += topButtonW + gap;
            addRenderableWidget(Button.builder(AbmText.component("button.save_template"), button -> save(true))
                    .bounds(x, bottomY, topButtonW, 20).build());
            x += topButtonW + gap;
            addRenderableWidget(Button.builder(AbmText.component("button.back"), button -> onClose())
                    .bounds(x, bottomY, panelWidth - (topButtonW + gap) * 5, 20).build());
        } else {
            int third = (panelWidth - gap * 2) / 3;
            addRenderableWidget(Button.builder(AbmText.component("button.add_fragment"), button -> addFragment())
                    .bounds(left, bottomY, third, 20).build());
            addRenderableWidget(Button.builder(AbmText.component("button.rich_text"), button -> openRichTextEditor())
                    .bounds(left + third + gap, bottomY, third, 20).build());
            addRenderableWidget(Button.builder(AbmText.component("button.import_template"), button -> openTemplateSelect())
                    .bounds(left + (third + gap) * 2, bottomY, panelWidth - (third + gap) * 2, 20).build());

            int saveW = (panelWidth - gap * 2) / 3;
            addRenderableWidget(Button.builder(AbmText.component("button.save"), button -> save(false))
                    .bounds(left, bottomY + 24, saveW, 20).build());
            addRenderableWidget(Button.builder(AbmText.component("button.save_template"), button -> save(true))
                    .bounds(left + saveW + gap, bottomY + 24, saveW, 20).build());
            addRenderableWidget(Button.builder(AbmText.component("button.back"), button -> onClose())
                    .bounds(left + (saveW + gap) * 2, bottomY + 24, panelWidth - (saveW + gap) * 2, 20).build());
        }
    }

    private void addFragment() {
        syncFromFields();
        if (definition.elements.size() < 32) {
            definition.elements.add(new ActionbarElement(AbmText.string("default.fragment_text"), 0xFFFFFF));
            fragmentScroll = getMaxFragmentScroll();
        }
        refresh();
    }

    private void openTemplateSelect() {
        syncFromFields();
        Minecraft.getInstance().setScreen(new AbmTemplateSelectScreen(this));
    }

    private void openRichTextEditor() {
        syncFromFields();
        Minecraft.getInstance().setScreen(new AbmRichTextScreen(this, definition.copy()));
    }

    public void replaceDefinition(ActionbarDefinition replacement) {
        if (replacement == null) {
            return;
        }
        this.definition = replacement.copy();
        this.definition.normalize();
        this.fragmentScroll = 0;
        refresh();
    }

    public List<ActionbarDefinition> getTemplates() {
        return templates;
    }

    public void applyTemplate(ActionbarDefinition template) {
        if (template == null) {
            return;
        }
        syncFromFields();
        String oldName = definition.name;
        ActionbarDefinition imported = template.copy();
        imported.name = oldName == null || oldName.isBlank() ? imported.name : oldName;
        imported.normalize();
        definition = imported;
        fragmentScroll = 0;
    }

    private void save(boolean template) {
        syncFromFields();
        ActionbarDefinition saved = definition.copy();
        if (template) {
            rememberTemplate(saved);
        }
        AbmNetwork.sendToServer(new C2SSaveActionbarPacket(saved, template));

        if (previous != null) {
            if (previous instanceof AbmManagerScreen manager) {
                manager.addActionbar(saved);
                manager.refresh();
            }
            this.minecraft.setScreen(previous);

        } else {
            this.onClose();
        }
    }

    private void rememberTemplate(ActionbarDefinition template) {
        if (template == null) {
            return;
        }
        String key = template.name == null ? "" : template.name.toLowerCase(Locale.ROOT);
        for (int i = 0; i < templates.size(); i++) {
            ActionbarDefinition existing = templates.get(i);
            String existingKey = existing.name == null ? "" : existing.name.toLowerCase(Locale.ROOT);
            if (existingKey.equals(key)) {
                templates.set(i, template.copy());
                return;
            }
        }
        templates.add(template.copy());
    }

    private void addElementRow(int index, int left, int panelWidth, int y) {
        ActionbarElement element = definition.elements.get(index);
        int gap = 6;
        int colorW = 118;
        int toggleW = 22;
        int delW = 26;
        int fixedW = gap + colorW + gap + toggleW * 5 + gap + delW;
        int textW = Math.max(110, panelWidth - fixedW);

        EditBox box = new EditBox(this.font, left, y, textW, 18, AbmText.component("field.fragment", index + 1));
        box.setMaxLength(256);
        box.setValue(element.text);
        elementBoxes.add(box);
        elementBoxIndexes.add(index);
        addRenderableWidget(box);

        int x = left + textW + gap;
        addRenderableWidget(Button.builder(AbmText.component(AbmColors.COLOR_KEYS[getColorIndex(element.color)]), button -> {
            syncFromFields();
            ActionbarElement row = definition.elements.get(index);
            row.color = AbmColors.COLORS[(getColorIndex(row.color) + 1) % AbmColors.COLORS.length];
            refresh();
        }).bounds(x, y, colorW, 18).build());

        x += colorW + gap;
        addRenderableWidget(toggleButton("B", element.bold, x, y, button -> {
            syncFromFields();
            definition.elements.get(index).bold = !definition.elements.get(index).bold;
            refresh();
        }));
        x += toggleW;
        addRenderableWidget(toggleButton("I", element.italic, x, y, button -> {
            syncFromFields();
            definition.elements.get(index).italic = !definition.elements.get(index).italic;
            refresh();
        }));
        x += toggleW;
        addRenderableWidget(toggleButton("U", element.underlined, x, y, button -> {
            syncFromFields();
            definition.elements.get(index).underlined = !definition.elements.get(index).underlined;
            refresh();
        }));
        x += toggleW;
        addRenderableWidget(toggleButton("S", element.strikethrough, x, y, button -> {
            syncFromFields();
            definition.elements.get(index).strikethrough = !definition.elements.get(index).strikethrough;
            refresh();
        }));
        x += toggleW;
        addRenderableWidget(toggleButton("?", element.obfuscated, x, y, button -> {
            syncFromFields();
            definition.elements.get(index).obfuscated = !definition.elements.get(index).obfuscated;
            refresh();
        }));
        x += toggleW + gap;

        addRenderableWidget(Button.builder(Component.literal("X"), button -> {
            syncFromFields();
            if (definition.elements.size() > 1) {
                definition.elements.remove(index);
            }
            clampFragmentScroll();
            refresh();
        }).bounds(x, y, delW, 18).build());
    }

    private Button toggleButton(String label, boolean value, int x, int y, Button.OnPress action) {
        return Button.builder(Component.literal(value ? label + "✓" : label), action).bounds(x, y, 22, 18).build();
    }

    private EditBox smallNumberBox(int x, int y, int value, int width) {
        EditBox box = new EditBox(this.font, x, y, width, 20, AbmText.component("field.ticks"));
        box.setMaxLength(4);
        box.setValue(Integer.toString(value));
        return box;
    }

    private EditBox smallSignedNumberBox(int x, int y, int value) {
        EditBox box = new EditBox(this.font, x, y, AbmEditorScreen.Y_OFFSET_FIELD_WIDTH, 20, AbmText.component("field.pixels"));
        box.setMaxLength(3);
        box.setValue(Integer.toString(value));
        return box;
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
        graphics.drawString(this.font, AbmText.component("label.name"), left, 34, 0xAAAAAA, false);

        if (nameBox != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int iconX = nameBox.getX() + nameBox.getWidth() + 8;
            graphics.blit(LOGO_ICON, iconX, nameBox.getY(), 0, 0, 20, 20, 20, 20);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        drawTimingLabels(graphics, left, panelWidth, getDurationLabelY());

        int fragmentHelpY = getRowsTopY() - GROUP_GAP;
        Component help = AbmText.component("editor.fragments_help");
        graphics.drawString(this.font, help, left, fragmentHelpY, 0xAAAAAA, false);

        if (definition.elements.size() > getVisibleRowCount()) {
            int start = fragmentScroll + 1;
            int end = Math.min(definition.elements.size(), fragmentScroll + getVisibleRowCount());
            String counter = start + "-" + end + " / " + definition.elements.size();
            int counterRight = left + panelWidth - 58;
            graphics.drawString(this.font, counter, counterRight - this.font.width(counter), fragmentHelpY, 0xAAAAAA, false);
        }

        int bottomHelpY = getBottomButtonsY() - GROUP_GAP;
        if (bottomHelpY > fragmentHelpY + GROUP_GAP) {
            graphics.drawString(this.font, AbmText.component("editor.style_help"), left, bottomHelpY, 0x777777, false);
            graphics.drawString(this.font, AbmText.component("editor.placeholders_help"), left, bottomHelpY - GROUP_GAP, 0x666666, false);
        }

        syncFromFieldsLight();
        AbmOverlay.renderPreview(graphics, definition, abmWidth() / 2, abmHeight() - 34, 1.0F);
        super.render(graphics, virtualMouseX, virtualMouseY, partialTick);
        endCompactRender(graphics);
    }

    private void drawTimingLabels(GuiGraphics graphics, int left, int panelWidth, int y) {
        int labelY = y + 6;
        if (useSingleTimingRow()) {
            int[] layout = getTimingLayout(left, panelWidth);
            graphics.drawString(this.font, AbmText.component("label.fade_in"), layout[0], labelY, 0xAAAAAA, false);
            graphics.drawString(this.font, AbmText.component("label.stay"), layout[2], labelY, 0xAAAAAA, false);
            graphics.drawString(this.font, AbmText.component("label.fade_out"), layout[4], labelY, 0xAAAAAA, false);
            graphics.drawString(this.font, AbmText.component("label.y"), layout[6], labelY, 0xAAAAAA, false);
        } else {
            int gap = 10;
            int fieldW = TIMING_FIELD_WIDTH;
            int firstRowTotal = this.font.width(AbmText.string("label.fade_in")) + 5 + fieldW + gap
                    + this.font.width(AbmText.string("label.stay")) + 5 + fieldW + gap
                    + this.font.width(AbmText.string("label.fade_out")) + 5 + fieldW;
            int x = left + Math.max(0, (panelWidth - firstRowTotal) / 2);
            graphics.drawString(this.font, AbmText.component("label.fade_in"), x, labelY, 0xAAAAAA, false);
            x += this.font.width(AbmText.string("label.fade_in")) + 5 + fieldW + gap;
            graphics.drawString(this.font, AbmText.component("label.stay"), x, labelY, 0xAAAAAA, false);
            x += this.font.width(AbmText.string("label.stay")) + 5 + fieldW + gap;
            graphics.drawString(this.font, AbmText.component("label.fade_out"), x, labelY, 0xAAAAAA, false);

            int secondRowY = y + 30;
            int secondRowTotal = this.font.width(AbmText.string("label.y")) + 5 + Y_OFFSET_FIELD_WIDTH + 5 + RESET_Y_BUTTON_WIDTH;
            int secondRowX = left + Math.max(0, (panelWidth - secondRowTotal) / 2);
            graphics.drawString(this.font, AbmText.component("label.y"), secondRowX, secondRowY, 0xAAAAAA, false);
        }
    }

    private int[] getTimingLayout(int left, int panelWidth) {
        int labelGap = 5;
        int fadeLabelW = this.font.width(AbmText.string("label.fade_in"));
        int stayLabelW = this.font.width(AbmText.string("label.stay"));
        int fadeOutLabelW = this.font.width(AbmText.string("label.fade_out"));
        int yLabelW = this.font.width(AbmText.string("label.y"));
        int total = fadeLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + stayLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + fadeOutLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + yLabelW + labelGap + Y_OFFSET_FIELD_WIDTH + 5 + RESET_Y_BUTTON_WIDTH;
        int x = left + Math.max(0, (panelWidth - total) / 2);
        int[] layout = new int[9];
        layout[0] = x;
        layout[1] = x + fadeLabelW + labelGap;
        x += fadeLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP;
        layout[2] = x;
        layout[3] = x + stayLabelW + labelGap;
        x += stayLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP;
        layout[4] = x;
        layout[5] = x + fadeOutLabelW + labelGap;
        x += fadeOutLabelW + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP;
        layout[6] = x;
        layout[7] = x + yLabelW + labelGap;
        layout[8] = layout[7] + Y_OFFSET_FIELD_WIDTH + 5;
        return layout;
    }


    private int getMinimumSingleTimingWidth() {
        int labelGap = 5;
        return this.font.width(AbmText.string("label.fade_in")) + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + this.font.width(AbmText.string("label.stay")) + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + this.font.width(AbmText.string("label.fade_out")) + labelGap + TIMING_FIELD_WIDTH + GROUP_GAP
                + this.font.width(AbmText.string("label.y")) + labelGap + Y_OFFSET_FIELD_WIDTH + 5 + RESET_Y_BUTTON_WIDTH;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (definition.elements.size() > getVisibleRowCount()) {
            int oldScroll = fragmentScroll;
            if (delta > 0) {
                fragmentScroll = Math.max(0, fragmentScroll - 1);
            } else if (delta < 0) {
                fragmentScroll = Math.min(getMaxFragmentScroll(), fragmentScroll + 1);
            }
            if (oldScroll != fragmentScroll) {
                syncFromFields();
                refresh();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void scrollFragments(int direction) {
        syncFromFields();
        fragmentScroll = Math.max(0, Math.min(getMaxFragmentScroll(), fragmentScroll + direction));
        refresh();
    }

    // fragments start height
    private int getRowsTopY() {
        return getDurationLabelY() + (useSingleTimingRow() ? 45 : 60);
    }

    private int getDurationLabelY() {
        int panelWidth = getPanelWidth();
        int backgroundW = 142;
        int reloadW = 132;
        int headW = panelWidth - backgroundW - reloadW - 16;
        return headW >= 150 ? 88 : 112;
    }

    private boolean useSingleTimingRow() {
        return getPanelWidth() >= getMinimumSingleTimingWidth();
    }

    private int getVisibleRowCount() {
        int available = Math.max(ROW_HEIGHT, getBottomButtonsY() - getRowsTopY() - 8);
        return Math.max(1, Math.min(9, available / ROW_HEIGHT));
    }

    private int getMaxFragmentScroll() {
        return Math.max(0, definition.elements.size() - getVisibleRowCount());
    }

    private void clampFragmentScroll() {
        fragmentScroll = Math.max(0, Math.min(fragmentScroll, getMaxFragmentScroll()));
    }

    private int getBottomButtonsY() {
        return abmHeight() < 230 ? abmHeight() - 45 : abmHeight() - 70;
    }

    private int getPanelWidth() {
        return Math.max(280, Math.min(MAX_PANEL_WIDTH, abmWidth() - MIN_SIDE_MARGIN * 2));
    }

    private int getPanelLeft() {
        return (abmWidth() - getPanelWidth()) / 2;
    }

    private void syncFromFieldsLight() {
        if (nameBox != null) {
            definition.name = nameBox.getValue();
        }
        for (int i = 0; i < elementBoxes.size() && i < elementBoxIndexes.size(); i++) {
            int elementIndex = elementBoxIndexes.get(i);
            if (elementIndex >= 0 && elementIndex < definition.elements.size()) {
                definition.elements.get(elementIndex).text = elementBoxes.get(i).getValue();
            }
        }
        if (textYOffsetBox != null) {
            definition.textYOffset = parseIntRange(textYOffsetBox, definition.textYOffset);
        }
    }

    private void syncFromFields() {
        syncFromFieldsLight();
        definition.defaultFadeIn = parseInt(fadeInBox, definition.defaultFadeIn, 0);
        definition.defaultStay = parseInt(stayBox, definition.defaultStay, 1);
        definition.defaultFadeOut = parseInt(fadeOutBox, definition.defaultFadeOut, 0);
        definition.textYOffset = parseIntRange(textYOffsetBox, definition.textYOffset);
        definition.normalize();
    }

    private int parseInt(EditBox box, int fallback, int min) {
        if (box == null) {
            return fallback;
        }
        try {
            return Math.max(min, Integer.parseInt(box.getValue().trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseIntRange(EditBox box, int fallback) {
        if (box == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(box.getValue().trim());
            return Math.max(-8, Math.min(8, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void setSelectedHead(String headFile) {
        definition.head = headFile == null ? "" : headFile;
        definition.normalize();
    }

    public String getSelectedHead() {
        return definition.head == null ? "" : definition.head;
    }

    private String getHeadLabel() {
        return definition.head == null || definition.head.isBlank() ? AbmText.string("head.none_label") : definition.head;
    }

    private int getColorIndex(int color) {
        return AbmColors.indexOf(color);
    }

    private void refresh() {
        clearWidgets();
        init();
    }

    @Override
    public void onClose() {
        if (previous != null) {
            this.minecraft.setScreen(previous);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
