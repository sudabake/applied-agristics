package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.*;
import appeng.client.gui.style.*;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import appeng.api.config.*;
import io.github.sudabake.appliedagristics.block.ItemPort;

/** Sixteen-slot storage; only the output port exposes an automatic export toggle. */
public final class PortScreen extends AEBaseScreen<MachineMenu> {
    private final SettingToggleButton<YesNo> autoOutput;
    public PortScreen(MachineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/appliedagristics_port.json"));
        if (!((ItemPort)menu.machine).isInput()) {
            autoOutput=new SettingToggleButton<>(Settings.AUTO_EXPORT,YesNo.NO,(b,backwards)->AltarScreen.send(menu,8,0));
            addToLeftToolbar(autoOutput);
        } else autoOutput=null;
    }
    @Override public void drawBG(GuiGraphics graphics,int offsetX,int offsetY,int mouseX,int mouseY,float partialTick) {
        super.drawBG(graphics,offsetX,offsetY,mouseX,mouseY,partialTick);
        for (var slot:menu.slots) Icon.SLOT_BACKGROUND.getBlitter().dest(leftPos+slot.x-1,topPos+slot.y-1).blit(graphics);
    }
    @Override protected void updateBeforeRender() { super.updateBeforeRender(); if (autoOutput!=null) autoOutput.set(menu.state.getBoolean("autoOutput")?YesNo.YES:YesNo.NO); setTextContent("dialog_title",menu.machine.getDisplayName()); setTextContent("state",Component.translatable("status.appliedagristics."+menu.state.getString("status"))); }
}
