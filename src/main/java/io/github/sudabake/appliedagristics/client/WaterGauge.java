package io.github.sudabake.appliedagristics.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared water display for the two independent farming machines. */
final class WaterGauge {
  static void draw(GuiGraphics graphics, int x, int y, int width, int height, int amount) {
    // Vanilla inset border, matching the independent machines' MA inventory slots.
    graphics.fill(x, y, x + width, y + height, 0xFF373737);
    graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    int filled = (int) Math.clamp((long) amount * (height - 4) / 16000, 0, height - 4);
    if (filled > 0) {
      graphics.enableScissor(x + 2, y + height - 2 - filled, x + width - 2, y + height - 2);
      var fluid = appeng.client.gui.style.FluidBlitter.create(new FluidStack(Fluids.WATER, 1));
      for (int py = y + 2; py < y + height - 2; py += 16)
        fluid.dest(x + 2, py, width - 4, 16).blit(graphics);
      graphics.disableScissor();
    }
    for (int py = y + 6; py < y + height - 3; py += 8)
      graphics.fill(x + width - 5, py, x + width - 2, py + 1, 0xFFC6C6C6);
  }

  private WaterGauge() {}
}
