package io.github.sudabake.appliedagristics.client;

import appeng.api.config.*;
import appeng.client.gui.*;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Native settings page with a single selected collection category and deliberate two-click
 * ejection.
 */
public final class SeedStorageSettingsScreen extends AESubScreen<MachineMenu, SeedStorageScreen> {
  private final AECheckbox autoCollect;
  private final AE2Button[] filters = new AE2Button[4];
  private final AE2Button eject;
  private boolean confirming;

  public SeedStorageSettingsScreen(SeedStorageScreen parent) {
    super(parent, "/screens/appliedagristics_seed_settings.json");
    widgets.add(
        "back",
        new TabButton(Icon.BACK, menu.machine.getDisplayName(), button -> returnToParent()));
    autoCollect = widgets.addCheckbox("autoCollect",
        Component.translatable("gui.appliedagristics.auto_collect_label"), () -> {
          confirming = false;
          AltarScreen.send(menu, 15, 0);
        });
    autoCollect.setTooltip(Tooltip.create(Component.translatable("gui.appliedagristics.auto_collect_help")));
    for (int i = 0; i < 4; i++) {
      final int filter = i;
      filters[i] =
          new AE2Button(
              Component.translatable("gui.appliedagristics.collect_filter_" + i),
              button -> {
                confirming = false;
                AltarScreen.send(menu, 16, filter);
              });
      widgets.add("filter" + i, filters[i]);
    }
    eject =
        new AE2Button(
            Component.empty(),
            button -> {
              if (confirming) {
                confirming = false;
                AltarScreen.send(menu, 17, 0);
              } else confirming = true;
            });
    eject.setTooltip(Tooltip.create(Component.translatable("gui.appliedagristics.eject_warning")));
    widgets.add("eject", eject);
  }

  @Override
  protected boolean shouldAddToolbar() {
    return false;
  }

  @Override
  protected void updateBeforeRender() {
    super.updateBeforeRender();
    autoCollect.setSelected(menu.state.getBoolean("autoCollect"));
    for (int i = 0; i < filters.length; i++)
      filters[i].active = i != menu.state.getInt("collectFilter");
    eject.setMessage(
        Component.translatable(
            "gui.appliedagristics." + (confirming ? "eject_confirm" : "eject_seeds")));
  }
}
