package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.multiblock.PlanterLayout;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Vanilla tooltip event also used by JEI; no replacement items or saved-data changes. */
@EventBusSubscriber(modid = Agristics.ID, value = Dist.CLIENT)
public final class ItemTooltips {
  @SubscribeEvent
  public static void describe(ItemTooltipEvent event) {
    var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
    if (!id.getNamespace().equals(Agristics.ID)) return;
    String name =
        switch (id.getPath()) {
          case "seed_storage_tier1", "seed_storage_tier2" -> "seed_storage";
          default -> id.getPath();
        };
    String prefix = "tooltip.appliedagristics." + name;
    if (!I18n.exists(prefix + ".summary")) return;
    var lines = new ArrayList<Component>();
    lines.add(Component.translatable(prefix + ".summary").withStyle(ChatFormatting.GRAY));
    var details = new ArrayList<Component>();
    for (int i = 0; i < 4 && I18n.exists(prefix + ".detail." + i); i++) {
      var text =
          i == 0 && (name.equals("planter_speed_module") || name.equals("planter_parallel_module"))
              ? Component.translatable(
                  prefix + ".detail." + i,
                  Component.literal(Integer.toString(PlanterLayout.MODULE_LIMIT))
                      .withStyle(ChatFormatting.AQUA))
              : Component.translatable(prefix + ".detail." + i);
      details.add(text.withStyle(ChatFormatting.GRAY));
    }
    if (!details.isEmpty()) {
      var mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null && Screen.hasShiftDown()) lines.addAll(details);
      else
        lines.add(
            Component.translatable(
                    "tooltip.appliedagristics.hold_shift",
                    Component.literal("Shift").withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
    // Keep descriptions directly below the name, before advanced/debug information.
    event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
  }

  private ItemTooltips() {}
}
