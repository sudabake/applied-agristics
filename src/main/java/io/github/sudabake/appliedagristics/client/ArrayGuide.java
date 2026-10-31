package io.github.sudabake.appliedagristics.client;

import guideme.Guide;
import guideme.PageAnchor;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.resources.ResourceLocation;

/** GuideME owns indexing, GUI hover hotkeys and interactive documentation scenes. */
public final class ArrayGuide {
  public static final ResourceLocation PAGE =
      ResourceLocation.fromNamespaceAndPath(Agristics.ID, "index.md");

  public static Guide guide() {
    return appeng.core.AppEngClient.instance().getGuide();
  }

  public static void init() {}

  public static void open() {
    appeng.core.AppEngClient.instance().openGuideAtAnchor(PageAnchor.page(PAGE));
  }

  private ArrayGuide() {}
}
