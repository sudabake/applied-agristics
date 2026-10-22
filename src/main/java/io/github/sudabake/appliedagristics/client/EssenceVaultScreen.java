package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.StyleManager;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Read-only list; extraction is available through the connected AE network. */
public final class EssenceVaultScreen extends AEBaseScreen<MachineMenu> {
    public EssenceVaultScreen(MachineMenu menu,Inventory inventory,Component title) { super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/appliedagristics_vault.json")); }
    @Override public void drawBG(GuiGraphics graphics,int offsetX,int offsetY,int mouseX,int mouseY,float partialTick) {
        super.drawBG(graphics,offsetX,offsetY,mouseX,mouseY,partialTick);
        for (var slot:menu.slots) Blitter.texture("guis/inscriber.png").src(7,94,18,18).dest(leftPos+slot.x-1,topPos+slot.y-1).blit(graphics);
    }
    @Override public void drawFG(GuiGraphics graphics,int offsetX,int offsetY,int mouseX,int mouseY) {
        super.drawFG(graphics,offsetX,offsetY,mouseX,mouseY);
        var rows=menu.state.getList("rows",10);
        for (int i=0;i<rows.size();i++) {
            var row=rows.getCompound(i); var stack=ItemStack.parseOptional(minecraft.level.registryAccess(),row.getCompound("item")); int y=22+i*17;
            graphics.renderItem(stack,9,y);
            graphics.drawString(font,font.plainSubstrByWidth(stack.getHoverName().getString(),89),29,y+1,0x404054,false);
            String count=Long.toString(row.getLong("count")); graphics.drawString(font,count,29,y+9,0x404054,false);
        }
        int pages=menu.state.getInt("pages");
        if (pages>1) {
            graphics.fill(164,22,167,90,0xFF777789);
            int thumb=Math.max(8,68*4/Math.max(4,menu.state.getInt("types")));
            int y=22+(68-thumb)*menu.page/(pages-1);
            graphics.fill(164,y,167,y+thumb,0xFFBBBBD0);
        }
    }
    @Override protected void updateBeforeRender() { super.updateBeforeRender(); setTextContent("state",Component.translatable("gui.appliedagristics.vault_readonly",menu.state.getInt("types"))); }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if (x>=leftPos && x<leftPos+176 && y>=topPos+22 && y<topPos+98) {
            menu.page=Math.clamp(menu.page-(int)Math.signum(vertical),0,Math.max(0,menu.state.getInt("pages")-1));
            PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(),4,"",menu.page,ItemStack.EMPTY)); return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
}
