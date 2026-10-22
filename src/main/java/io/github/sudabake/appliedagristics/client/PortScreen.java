package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.*;
import appeng.client.gui.style.*;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

/** Same AE screen and IO sub-screen as the altar, with a 4x4 port inventory. */
public final class PortScreen extends AEBaseScreen<MachineMenu> {
    public PortScreen(MachineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/appliedagristics_port.json"));
        addToLeftToolbar(new IconButton(b -> switchToScreen(new AltarIOScreen<>(this))) {
            @Override protected Icon getIcon() { return Icon.ACCESS_READ_WRITE; }
            @Override public List<Component> getTooltipMessage() { return List.of(Component.translatable("gui.appliedagristics.io_config")); }
        });
    }
    @Override public void drawBG(GuiGraphics graphics,int offsetX,int offsetY,int mouseX,int mouseY,float partialTick) {
        super.drawBG(graphics,offsetX,offsetY,mouseX,mouseY,partialTick);
        for (var slot:menu.slots) Blitter.texture("guis/inscriber.png").src(7,94,18,18).dest(leftPos+slot.x-1,topPos+slot.y-1).blit(graphics);
    }
    @Override protected void updateBeforeRender() { super.updateBeforeRender(); setTextContent("dialog_title",menu.machine.getDisplayName()); setTextContent("state",Component.translatable("status.appliedagristics."+menu.state.getString("status"))); }
}
