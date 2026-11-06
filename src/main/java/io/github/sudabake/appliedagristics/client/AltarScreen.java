package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.*;
import appeng.client.gui.style.*;
import appeng.client.gui.widgets.*;
import appeng.menu.SlotSemantics;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

/** Uses the same screen, slot, toolbar and upgrade panel implementation as AE addons. */
public class AltarScreen extends AEBaseScreen<MachineMenu> {
    private final SettingToggleButton<appeng.api.config.YesNo> autoOutput;
    private final IconButton directionButton;
    public AltarScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StyleManager.loadStyleDoc(menu.machine instanceof io.github.sudabake.appliedagristics.block.MEBioCultivator ? "/screens/appliedagristics_me_bio.json" : "/screens/appliedagristics_me_infusion.json"));
        widgets.add("progressBar", new ProgressBar(menu, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL));
        widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), () -> appeng.api.upgrades.Upgrades.getTooltipLinesForMachine(menu.machine.getBlockState().getBlock())));
        autoOutput = addToLeftToolbar(new SettingToggleButton<>(appeng.api.config.Settings.AUTO_EXPORT,
            appeng.api.config.YesNo.NO, (button, backwards) -> send(menu, 8, 0)));
        directionButton = addToLeftToolbar(new IconButton(b -> openIO()) {
            @Override protected Icon getIcon() { return Icon.ACCESS_READ_WRITE; }
            @Override public List<Component> getTooltipMessage() { return List.of(Component.translatable("gui.appliedagristics.io_config")); }
        });
    }
    public void openIO() { switchToScreen(new AltarIOScreen<>(this)); }
    static void send(MachineMenu menu, int action, long value) {
        PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(), menu.containerId, menu.sessionToken, action, "", value, ItemStack.EMPTY));
    }
    @Override public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTick) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTick);
        for (var slot : menu.slots) {
            if (!menu.getSlots(SlotSemantics.UPGRADE).contains(slot) && slot.isActive())
                Blitter.texture("guis/molecular_assembler.png").src(125, 15, 18, 18).dest(leftPos + slot.x - 1, topPos + slot.y - 1).blit(graphics);
        }
        Blitter.texture("guis/molecular_assembler.png").src(85, 49, 34, 15).dest(leftPos + 76, topPos + 41).blit(graphics);
        Blitter.texture("guis/molecular_assembler.png").src(148, 48, 6, 18).dest(leftPos + 152, topPos + 39).blit(graphics);
    }
    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        boolean enabled=menu.state.getBoolean("autoOutput");
        autoOutput.set(enabled ? appeng.api.config.YesNo.YES : appeng.api.config.YesNo.NO);
        directionButton.setVisibility(enabled);
        setTextContent("dialog_title",menu.machine.getDisplayName());
        setTextContent("state", Component.translatable("status.appliedagristics." + menu.state.getString("status")));
        setTextContent("speed", Component.literal("×" + (1 << menu.state.getInt("acceleration"))));
    }
}
