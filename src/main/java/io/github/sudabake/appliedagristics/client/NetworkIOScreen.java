package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

/** Shares the open menu with its parent; server validation still owns every change. */
public final class NetworkIOScreen extends Screen {
    private final MachineScreen parent;
    private final MachineMenu menu;
    private ToggleButton output;
    public NetworkIOScreen(MachineScreen parent) {
        super(Component.translatable("gui.appliedagristics.io_config"));this.parent=parent;menu=parent.getMenu();
    }
    @Override protected void init() {
        int x=(width-176)/2,y=(height-100)/2;
        addRenderableWidget(new TabButton(Icon.BACK,Component.translatable("gui.back"),b -> onClose())).setPosition(x-22,y);
        output=addRenderableWidget(new ToggleButton(Icon.ACCESS_WRITE,Icon.INVALID,b -> PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(),11,"",0,net.minecraft.world.item.ItemStack.EMPTY))));
        output.setPosition(x+12,y+32);
        output.setTooltipOn(List.of(Component.translatable("gui.appliedagristics.direct_output_on")));
        output.setTooltipOff(List.of(Component.translatable("gui.appliedagristics.direct_output_off")));
    }
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        super.renderBackground(g,mouseX,mouseY,partialTick);
        int x=(width-176)/2,y=(height-100)/2;
        g.fill(x,y,x+176,y+100,0xFF3D3E52);g.fill(x+2,y+2,x+174,y+98,0xFFCDCED8);
        g.drawString(font,title,x+8,y+8,0x404054,false);
        g.drawString(font,Component.translatable("gui.appliedagristics.direct_output"),x+38,y+37,0x404054,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        output.setState(menu.state.getBoolean("directNetworkOutput"));super.render(g,mouseX,mouseY,partialTick);
    }
    @Override public void tick() { if(minecraft.player==null || minecraft.player.containerMenu!=menu) minecraft.setScreen(null); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
