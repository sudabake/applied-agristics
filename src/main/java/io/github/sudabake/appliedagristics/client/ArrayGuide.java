package io.github.sudabake.appliedagristics.client;

import guideme.Guide;
import guideme.PageAnchor;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.resources.ResourceLocation;

/** GuideME owns indexing, GUI hover hotkeys and interactive documentation scenes. */
public final class ArrayGuide {
  public static final ResourceLocation PAGE =
      ResourceLocation.fromNamespaceAndPath(Agristics.ID, "array.md");

  public static Guide guide() {
    return appeng.core.AppEngClient.instance().getGuide();
  }

  public static void init() {}

  public static void open() {
    open(PAGE);
  }

  public static ResourceLocation pageForPart(String part) {
    return switch (part) {
      case "network_planter", "me_network_access", "planter_frame", "cultivation_chamber" -> PAGE;
      case "seed_storage", "planter_input_port" -> page("storage");
      case "essence_vault", "planter_output_port" -> page("crop-output");
      case "planter_speed_module", "planter_parallel_module", "planter_conversion_module" -> page("array-upgrades");
      default -> null;
    };
  }

  private static ResourceLocation page(String name) {
    return ResourceLocation.fromNamespaceAndPath(Agristics.ID,name+".md");
  }

  public static void open(ResourceLocation page) {
    appeng.core.AppEngClient.instance().openGuideAtAnchor(PageAnchor.page(page));
  }

  private ArrayGuide() {}
}
