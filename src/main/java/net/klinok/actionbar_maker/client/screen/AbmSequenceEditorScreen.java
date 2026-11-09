package net.klinok.actionbar_maker.client.screen;

import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.data.ActionbarSequence;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.network.C2SSaveSequencePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class AbmSequenceEditorScreen extends AbmCompactScreen {
    private final ActionbarSequence sequence;
    private final Screen previous;
    private final List<EditBox[]> timingFields = new ArrayList<>();
    private EditBox nameBox;
    private int editingIndex = -1;
    private int scroll;

    public AbmSequenceEditorScreen(ActionbarDefinition first, Screen previous) {
        super(Component.translatable("actionbar_maker.sequence.title"));
        this.previous = previous;
        this.sequence = ActionbarSequence.create(first == null ? "New_Group" : first.name + "_group");
        if (first != null) {
            ActionbarSequence.Entry entry = new ActionbarSequence.Entry();
            entry.definition = first.copy();
            entry.fadeIn = first.defaultFadeIn; entry.stay = first.defaultStay; entry.fadeOut = first.defaultFadeOut;
            sequence.entries.add(entry);
        }
    }

    public AbmSequenceEditorScreen(ActionbarSequence existing, Screen previous) {
        super(Component.translatable("actionbar_maker.sequence.title"));
        this.previous = previous;
        this.sequence = existing == null ? ActionbarSequence.create("New_Group") : existing.copy();
    }

    @Override protected void init() {
        prepareCompactLayout();
        timingFields.clear();
        int screenWidth = abmWidth();
        int screenHeight = abmHeight();
        int panel = Math.min(660, screenWidth - 24);
        int left = (screenWidth - panel) / 2;
        nameBox = new EditBox(font, left, 28, panel, 20, Component.literal("Group name"));
        nameBox.setMaxLength(96); nameBox.setValue(sequence.name); addRenderableWidget(nameBox);
        int visible = Math.max(1, Math.min(8, (screenHeight - 120) / 40));
        int start = Math.min(scroll, Math.max(0, sequence.entries.size() - visible));
        int end = Math.min(sequence.entries.size(), start + visible);
        for (int i=start;i<end;i++) {
            final int index=i;
            ActionbarSequence.Entry entry=sequence.entries.get(i);
            int y=66+(i-start)*40;
            String[] labels={Component.translatable("actionbar_maker.sequence.before").getString(),Component.translatable("actionbar_maker.sequence.fade_in").getString(),Component.translatable("actionbar_maker.sequence.stay").getString(),Component.translatable("actionbar_maker.sequence.fade_out").getString(),Component.translatable("actionbar_maker.sequence.after").getString()};
            int[] values={entry.pauseBefore,entry.fadeIn,entry.stay,entry.fadeOut,entry.pauseAfter};
            EditBox[] boxes=new EditBox[5];
            int parameterX=left+panel-359;
            int nameWidth=Math.max(100,parameterX-left-70);
            addRenderableWidget(Button.builder(Component.literal((i+1)+". "+entry.definition.name), b -> {
                    sync();
                    editingIndex=index;
                    minecraft.setScreen(new AbmEditorScreen(entry.definition.copy(), List.of(), this));
                }).bounds(left,y,nameWidth,22).build());
            int arrowsX=left+nameWidth+6;
            addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(index,-1)).bounds(arrowsX,y,22,22).build());
            addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(index,1)).bounds(arrowsX+26,y,22,22).build());
            int x=parameterX;
            for(int j=0;j<5;j++) {
                boxes[j]=new EditBox(font,x+j*61,y,44,20,Component.literal(labels[j]));
                boxes[j].setMaxLength(4); boxes[j].setValue(Integer.toString(values[j]));
                addRenderableWidget(boxes[j]);
            }
            addRenderableWidget(Button.builder(Component.translatable("actionbar_maker.sequence.delete"), b -> { sync(); sequence.entries.remove(index); refresh(); })
                    .bounds(parameterX+288+17,y,54,22).build());
            timingFields.add(boxes);
        }
        int bottom=screenHeight-30;
        addRenderableWidget(Button.builder(Component.translatable("actionbar_maker.sequence.add"), b -> {
            sync(); if(sequence.entries.size()<64) {
                ActionbarSequence.Entry e=new ActionbarSequence.Entry();
                e.definition=ActionbarDefinition.createDefault("Actionbar_"+(sequence.entries.size()+1));
                sequence.entries.add(e); scroll=Math.max(0,sequence.entries.size()-visible); refresh();
            }
        }).bounds(left,bottom,120,20).build());
        addRenderableWidget(Button.builder(Component.translatable("actionbar_maker.sequence.save"), b -> {
            sync(); sequence.name=nameBox.getValue().trim(); sequence.normalize();
            if(!sequence.entries.isEmpty()) {
                AbmNetwork.sendToServer(new C2SSaveSequencePacket(sequence));
                if (previous instanceof AbmManagerScreen manager) manager.addSequence(sequence);
            }
            minecraft.setScreen(previous);
        }).bounds(left+panel-204,bottom,100,20).build());
        addRenderableWidget(Button.builder(Component.translatable("actionbar_maker.sequence.back"), b -> minecraft.setScreen(previous))
                .bounds(left+panel-98,bottom,98,20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        sync();
        int visible = Math.max(1, Math.min(8, (abmHeight() - 120) / 40));
        int max = Math.max(0, sequence.entries.size() - visible);
        int old = scroll;
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else if (delta < 0) scroll = Math.min(max, scroll + 1);
        if (old != scroll) { refresh(); return true; }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void sync() {
        if (nameBox != null) sequence.name = nameBox.getValue().trim();
        int start=Math.min(scroll,Math.max(0,sequence.entries.size()-timingFields.size()));
        for(int i=0;i<timingFields.size();i++) {
            int idx=start+i; if(idx>=sequence.entries.size()) break;
            ActionbarSequence.Entry e=sequence.entries.get(idx); EditBox[] b=timingFields.get(i);
            e.pauseBefore=read(b[0],e.pauseBefore,0,1200); e.fadeIn=read(b[1],e.fadeIn,0,200);
            e.stay=read(b[2],e.stay,1,1200); e.fadeOut=read(b[3],e.fadeOut,0,200);
            e.pauseAfter=read(b[4],e.pauseAfter,0,1200);
        }
    }
    private int read(EditBox b,int old,int min,int max) {
        try { return Math.max(min,Math.min(max,Integer.parseInt(b.getValue().trim()))); }
        catch(Exception ex) { return old; }
    }
    private void move(int index,int delta) {
        sync(); int target=index+delta;
        if(target<0||target>=sequence.entries.size()) return;
        ActionbarSequence.Entry e=sequence.entries.remove(index); sequence.entries.add(target,e);
        scroll=Math.max(0,Math.min(scroll,sequence.entries.size()-1)); refresh();
    }
    public void updateEditedEntry(ActionbarDefinition definition) {
        if(editingIndex>=0&&editingIndex<sequence.entries.size()) {
            sync(); sequence.entries.get(editingIndex).definition=definition.copy(); refresh();
        }
        editingIndex=-1;
    }
    private void refresh() { clearWidgets(); init(); }
    @Override public void render(GuiGraphics g,int mx,int my,float pt) {
        renderBackground(g);
        beginCompactRender(g);
        int screenWidth = abmWidth();
        int screenHeight = abmHeight();
        int virtualMouseX = virtualMouseX(mx);
        int virtualMouseY = virtualMouseY(my);
        g.drawCenteredString(font,title,screenWidth/2,10,0xFFFFFF);
        int panel = Math.min(660, screenWidth - 24);
        int left = (screenWidth - panel) / 2;
        int visible = Math.max(1, Math.min(8, (screenHeight - 120) / 40));
        int start = Math.min(scroll, Math.max(0, sequence.entries.size() - visible));
        String[] labels = {
                Component.translatable("actionbar_maker.sequence.before").getString(),
                Component.translatable("actionbar_maker.sequence.fade_in").getString(),
                Component.translatable("actionbar_maker.sequence.stay").getString(),
                Component.translatable("actionbar_maker.sequence.fade_out").getString(),
                Component.translatable("actionbar_maker.sequence.after").getString()
        };
        for (int i = start; i < Math.min(sequence.entries.size(), start + visible); i++) {
            int y = 52 + (i - start) * 40;
            for (int j = 0; j < labels.length; j++) {
                g.drawString(font, labels[j], left + panel - 359 + j * 61, y, 0xAAAAAA, false);
            }
        }
        g.drawString(font,Component.translatable("actionbar_maker.sequence.order_hint"),left,screenHeight-48,0xAAAAAA);
        super.render(g,virtualMouseX,virtualMouseY,pt);
        endCompactRender(g);
    }
    @Override public void onClose() { minecraft.setScreen(previous); }
    @Override public boolean isPauseScreen() { return false; }
}
