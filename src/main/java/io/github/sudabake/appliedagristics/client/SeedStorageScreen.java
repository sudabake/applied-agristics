package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.*;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AE storage UI. Clicking a displayed seed withdraws one free stack; management lives in its
 * sub-screen.
 */
public final class SeedStorageScreen extends AEBaseScreen<MachineMenu> {
  private boolean dragging;
  private final AE2Button[] withdraw = new AE2Button[12];
  private final IconButton recoveryWarning;

  public SeedStorageScreen(MachineMenu menu, Inventory inventory, Component title) {
    super(
        menu, inventory, title, StyleManager.loadStyleDoc("/screens/appliedagristics_seeds.json"));
    for (int i = 0; i < withdraw.length; i++) {
      final int index = i;
      withdraw[i] = new AE2Button(Component.literal("↑"), button -> {
        var row = row(index);
        if (row == null) return;
        var seed = ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("seed"));
        PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(),
            menu.containerId, menu.sessionToken, 3, "", 0, seed));
      });
      withdraw[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(
          Component.translatable("gui.appliedagristics.seed_withdraw_hint")));
      widgets.add("withdraw" + i, withdraw[i]);
    }
    recoveryWarning =
        addToLeftToolbar(
            new IconButton(button -> {}) {
              @Override
              protected Icon getIcon() {
                return Icon.INVALID;
              }

              @Override
              public List<Component> getTooltipMessage() {
                return List.of(
                    Component.translatable(
                        "gui.appliedagristics.inventory_unresolved",
                        menu.state.getInt("unresolved")));
              }
            });
    widgets.add(
        "collect",
        new AE2Button(
            Component.translatable("gui.appliedagristics.collect"),
            button -> AltarScreen.send(menu, 0, 0)));
    addToLeftToolbar(
        new IconButton(button -> switchToScreen(new SeedStorageSettingsScreen(this))) {
          @Override
          protected Icon getIcon() {
            return Icon.COG;
          }

          @Override
          public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.appliedagristics.seed_settings"));
          }
        });
  }

  private CompoundTag row(int i) {
    var rows = menu.state.getList("rows", 10);
    return i >= 0 && i < rows.size() ? rows.getCompound(i) : null;
  }

  @Override
  public void drawBG(
      GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partial) {
    super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partial);
    for (int i = 0; i < 12; i++) {
      if (row(i) == null) continue;
      int x = leftPos + 8 + i % 3 * 100, y = topPos + 60 + i / 3 * 22;
      graphics.fill(x, y - 1, x + 96, y + 21, 0xFFBFC0CE);
    }
    for (var slot : menu.slots)
      Icon.SLOT_BACKGROUND
          .getBlitter()
          .dest(leftPos + slot.x - 1, topPos + slot.y - 1)
          .blit(graphics);
  }

  @Override
  public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
    super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
    for (int i = 0; i < 12; i++) {
      var row = row(i);
      if (row == null) continue;
      int x = 8 + i % 3 * 100, y = 60 + i / 3 * 22;
      var stack =
          ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("seed"));
      graphics.renderItem(stack, x + 2, y);
      graphics.drawString(
          font,
          font.plainSubstrByWidth(io.github.sudabake.appliedagristics.biology.BioData.seed(stack)
              ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                  io.github.sudabake.appliedagristics.biology.BioData.identity(stack)).getDescription().getString()
              : stack.getHoverName().getString(), 52),
          x + 22,
          y + 1,
          0x404054,
          false);
      graphics.drawString(
          font,
          font.plainSubstrByWidth(row.getLong("used") + "/" + row.getLong("count"), 52),
          x + 22,
          y + 11,
          0x66667A,
          false);
    }
    int pages = menu.state.getInt("pages");
    if (pages > 1) {
      graphics.fill(313, 60, 316, 146, 0xFF88899E);
      int y = 60 + 70 * menu.page / (pages - 1);
      graphics.fill(313, y, 316, y + 16, 0xFF7476AC);
    }
  }

  @Override
  protected void updateBeforeRender() {
    super.updateBeforeRender();
    for (int i = 0; i < withdraw.length; i++) withdraw[i].visible = row(i) != null;
    recoveryWarning.setVisibility(menu.state.getInt("unresolved") > 0);
    setTextContent("dialog_title", menu.machine.getDisplayName());
    setTextContent(
        "state",
        Component.translatable("status.appliedagristics." + menu.state.getString("status")));
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    super.render(graphics, mouseX, mouseY, partial);
    for (int i = 0; i < 12; i++) {
      var row = row(i);
      int x = leftPos + 8 + i % 3 * 100, y = topPos + 60 + i / 3 * 22;
      if (row == null || mouseX < x || mouseX >= x + 76 || mouseY < y || mouseY >= y + 21) continue;
      var stack =
          ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("seed"));
      drawTooltipWithHeader(
          graphics,
          mouseX,
          mouseY,
          List.of(
              stack.getHoverName(),
              Component.literal(row.getLong("used") + " / " + row.getLong("count")),
              Component.translatable("gui.appliedagristics.seed_withdraw_hint")));
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    double x = mouseX - leftPos, y = mouseY - topPos;
    if (button == 0 && x >= 313 && x < 318 && y >= 60 && y < 146) {
      dragging = true;
      scrollTo(y);
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  private void scrollTo(double localY) {
    menu.page =
        Math.clamp(
            (int) Math.round((localY - 60) / 86 * (Math.max(1, menu.state.getInt("pages")) - 1)),
            0,
            Math.max(0, menu.state.getInt("pages") - 1));
    AltarScreen.send(menu, 4, menu.page);
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    if (dragging && button == 0) {
      scrollTo(y - topPos);
      return true;
    }
    return super.mouseDragged(x, y, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    if (dragging && button == 0) {
      dragging = false;
      return true;
    }
    return super.mouseReleased(x, y, button);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    if (x >= leftPos && x < leftPos + 320 && y >= topPos + 48 && y < topPos + 148) {
      menu.page =
          Math.clamp(
              menu.page - (int) Math.signum(vertical),
              0,
              Math.max(0, menu.state.getInt("pages") - 1));
      AltarScreen.send(menu, 4, menu.page);
      return true;
    }
    return super.mouseScrolled(x, y, horizontal, vertical);
  }
}
