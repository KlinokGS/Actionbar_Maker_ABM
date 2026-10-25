package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.client.AbmHistory;
import net.klinok.actionbar_maker.client.AbmOverlay;
import net.klinok.actionbar_maker.client.AbmText;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Read-only journal of the most recently displayed ABM messages. */
public final class AbmHistoryScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int TOP = 32;
    private final List<ActionbarDefinition> entries;
    private double scroll;

    public AbmHistoryScreen() {
        super(AbmText.component("screen.history.title"));
        this.entries = AbmHistory.getEntries();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font, AbmText.component("screen.history.empty"), this.width / 2, TOP + 10, 0xA0A0A0);
        } else {
            int visibleRows = Math.max(1, (this.height - TOP - 24) / ROW_HEIGHT);
            int first = Math.min((int) scroll, Math.max(0, entries.size() - visibleRows));
            int last = Math.min(entries.size(), first + visibleRows);
            for (int i = first; i < last; i++) {
                int rowY = TOP + (i - first) * ROW_HEIGHT;
                graphics.fill(8, rowY, this.width - 8, rowY + ROW_HEIGHT - 2, 0x50000000);
                AbmOverlay.renderHistoryEntry(graphics, entries.get(i), this.width / 2, rowY + 6);
            }
        }
        graphics.drawCenteredString(this.font, AbmText.component("screen.history.hint"), this.width / 2, this.height - 12, 0x909090);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visibleRows = Math.max(1, (this.height - TOP - 24) / ROW_HEIGHT);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        scroll = Math.max(0, Math.min(maxScroll, scroll - delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
