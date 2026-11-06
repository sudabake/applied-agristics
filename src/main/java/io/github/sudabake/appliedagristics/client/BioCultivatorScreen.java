package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Separate JEI click target, sharing the native AE processor screen and direction page. */
public final class BioCultivatorScreen extends AltarScreen {
  public BioCultivatorScreen(MachineMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }
}
