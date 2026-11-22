package io.github.sudabake.appliedagristics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Text control for the existing machine layouts on Forge 1.20.1. */
class MachineTextButton extends Button {
    MachineTextButton(Component message, OnPress onPress) {
        this(0, 0, 0, 0, message, onPress);
    }

    MachineTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean highlight = active && (isHovered() || isFocused());
        graphics.fill(x, y, x + w, y + h, highlight ? 0xff517497 : 0xff413f54);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, highlight ? 0xffd2e2f1 : active ? 0xff707084 : 0xffb1afc2);
        graphics.enableScissor(x + 2, y, x + w - 2, y + h);
        int color = highlight ? 0xff284866 : active ? 0xfff2f2f2 : 0xff413f54;
        int textX = Math.max(x + 2, x + (w - font.width(getMessage())) / 2);
        graphics.drawString(font, getMessage(), textX, y + (h - font.lineHeight + 1) / 2, color, false);
        graphics.disableScissor();
    }
}
