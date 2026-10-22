package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.block.CellCultivator;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Independent machine: Mystical Agriculture's actual frame, slots and FE gauge. */
public final class CultureScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation GUI = ResourceLocation.parse("mysticalagriculture:textures/gui/soul_extractor.png");
    private Button extraction;
    public CultureScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 193; inventoryLabelY = 102;
    }
    @Override protected void init() {
        super.init();
        extraction = addRenderableWidget(Button.builder(Component.empty(), b -> PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(), 9, "", 0, ItemStack.EMPTY))).bounds(leftPos + 132, topPos + 22, 36, 20).build());
        extraction.setTooltip(Tooltip.create(Component.translatable("gui.appliedagristics.culture_toggle_help")));
        updateButton();
    }
    private void updateButton() { extraction.setMessage(Component.translatable("gui.appliedagristics.culture_toggle_" + (menu.state.getBoolean("autoExtract") ? "on" : "off"))); }
    @Override protected void containerTick() { super.containerTick(); updateButton(); }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Adapt the native working area; the MA border and inventory stay intact.
        g.fill(leftPos + 22, topPos + 15, leftPos + 171, topPos + 105, 0xFFC6C6C6);
        g.fill(leftPos + 145, topPos + 4, leftPos + 170, topPos + 29, 0xFFC6C6C6);
        for (var slot : menu.slots) if (slot.container != menu.getPlayerInventory())
            g.blit(GUI, leftPos + slot.x - 1, topPos + slot.y - 1, 7, 111, 18, 18);
        int energy = Math.clamp(menu.state.getInt("energy") * 76 / CellCultivator.CAPACITY, 0, 76);
        if (energy > 0) g.blit(GUI, leftPos + 8, topPos + 18 + 76 - energy, 176, 32 + 76 - energy, 11, energy);
        g.fill(leftPos + 26, topPos + 18, leftPos + 34, topPos + 94, 0xFF373737);
        int water = Math.clamp(menu.state.getInt("water") * 74 / 16000, 0, 74);
        g.fill(leftPos + 27, topPos + 93 - water, leftPos + 33, topPos + 93, 0xFF387CAD);
        int width = Math.clamp(menu.state.getInt("progress") * 122 / Math.max(1, menu.state.getInt("duration")), 0, 122);
        g.fill(leftPos + 43, topPos + 89, leftPos + 165, topPos + 95, 0xFF8B8B8B);
        g.fill(leftPos + 43, topPos + 89, leftPos + 43 + width, topPos + 95, 0xFF699548);
        var work = ItemStack.parseOptional(minecraft.level.registryAccess(), menu.state.getCompound("work"));
        if (!work.isEmpty()) { g.renderItem(work, leftPos + 80, topPos + 66); g.drawString(font, "→", leftPos + 100, topPos + 70, 0x404040, false); }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        label(g, "culture_sample", 43, 17); label(g, "culture_nutrient", 67, 17); label(g, "culture_capsule", 91, 17);
        label(g, "culture_cells", 43, 54); label(g, "output", 116, 54);
        var state = Component.translatable("status.appliedagristics." + menu.state.getString("status"));
        g.drawString(font, font.plainSubstrByWidth(state.getString(), 94), 72, 101, 0x404040, false);
        // Inventory label has its own row in the native GUI; avoid overlapping status.
    }
    private void label(GuiGraphics g, String key, int x, int y) { g.drawString(font, Component.translatable("gui.appliedagristics." + key), x, y, 0x404040, false); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY);
        if (mouseY >= topPos + 18 && mouseY < topPos + 94) {
            if (mouseX >= leftPos + 7 && mouseX < leftPos + 20) g.renderTooltip(font, Component.literal(menu.state.getInt("energy") + " / " + CellCultivator.CAPACITY + " FE"), mouseX, mouseY);
            if (mouseX >= leftPos + 26 && mouseX < leftPos + 34) g.renderTooltip(font, Component.translatable("gui.appliedagristics.water", menu.state.getInt("water")), mouseX, mouseY);
        }
        if (mouseY >= topPos + 89 && mouseY < topPos + 96 && mouseX >= leftPos + 43 && mouseX < leftPos + 165)
            g.renderTooltip(font, Component.translatable("gui.appliedagristics.culture_progress", menu.state.getInt("progress") / 20.0, menu.state.getInt("duration") / 20.0, menu.state.getInt("cost")), mouseX, mouseY);
    }
}
