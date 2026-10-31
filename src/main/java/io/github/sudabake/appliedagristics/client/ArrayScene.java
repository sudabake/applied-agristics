package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import guideme.color.LightDarkMode;
import guideme.document.LytRect;
import guideme.scene.*;
import guideme.scene.level.GuidebookLevel;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.multiblock.PlanterLayout;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** Uses GuideME's existing fake-world renderer, as in ExtendedAE's world display. */
public final class ArrayScene {
  private GuidebookScene scene;
  private int layer = -1;
  private float yaw = 135, pitch = 25;
  private boolean cameraDirty = true;
  private boolean fitCamera = true;
  private int viewportWidth, viewportHeight;

  public void rotate() {
    yaw += 45;
    cameraDirty = true;
  }

  public void drag(double dx, double dy) {
    if (!Double.isFinite(dx) || !Double.isFinite(dy)) return;
    yaw = (float) ((yaw + dx) % 360);
    pitch = (float) Math.clamp(pitch + dy, -85, 85);
    cameraDirty = true;
  }

  public void layer() {
    layer++;
    if (layer == 4) layer = -1;
    scene = null;
  }

  public int selectedLayer() {
    return layer;
  }

  public static Block block(PlanterLayout.Role role) {
    return switch (role) {
      case CONTROLLER -> Agristics.NETWORK.get();
      case ACCESS -> Agristics.NETWORK_ACCESS.get();
      case SEEDS -> Agristics.SEEDS.get();
      case VAULT -> Agristics.ESSENCE_VAULT.get();
      case INPUT -> Agristics.INPUT_PORT.get();
      case OUTPUT -> Agristics.OUTPUT_PORT.get();
      case CHAMBER -> Agristics.CULTIVATION_CHAMBER.get();
      case SPEED -> Agristics.SPEED_MODULE.get();
      case PARALLEL -> Agristics.PARALLEL_MODULE.get();
      case CONVERSION -> Agristics.CONVERSION_MODULE.get();
      default -> Agristics.PLANTER_FRAME.get();
    };
  }

  public void draw(GuiGraphics g, int x, int y, int width, int height) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (scene == null) {
      var world = new GuidebookLevel(mc.level.registryAccess());
      var roles = PlanterLayout.example();
      for (int yy = 0; yy < 4; yy++)
        if (layer < 0 || layer == yy)
          for (int z = 0; z < 3; z++)
            for (int xx = 0; xx < 3; xx++)
              world.setBlock(
                  new BlockPos(xx, yy, z),
                  block(roles[PlanterLayout.index(xx, yy, z)]).defaultBlockState(),
                  2);
      scene = new GuidebookScene(world, new CameraSettings());
      scene.getCameraSettings().setRotationCenter(scene.getWorldCenter());
      cameraDirty = true;
      fitCamera = true;
    }
    var bounds = new LytRect(x, y, width, height);
    var camera = scene.getCameraSettings();
    if (cameraDirty || viewportWidth != width || viewportHeight != height) {
      camera.setViewportSize(bounds.size());
      camera.setIsometricYawPitchRoll(yaw, pitch, 0);
      if (fitCamera || viewportWidth != width || viewportHeight != height) {
        camera.setZoom(1);
        var projected = scene.getScreenBounds();
        camera.setZoom(
            Math.min(
                (width - 12) / (projected.z() - projected.x()),
                (height - 8) / (projected.w() - projected.y())));
        fitCamera = false;
      }
      scene.centerScene();
      viewportWidth = width;
      viewportHeight = height;
      cameraDirty = false;
    }
    g.flush();
    var viewport = bounds.transform(g.pose().last().pose());
    var window = mc.getWindow();
    RenderSystem.viewport(
        (int) (viewport.x() * window.getGuiScale()),
        (int) (window.getHeight() - viewport.bottom() * window.getGuiScale()),
        (int) (viewport.width() * window.getGuiScale()),
        (int) (viewport.height() * window.getGuiScale()));
    // GuiGraphics scissor coordinates are absolute; JEI translates the pose before drawing a
    // recipe.
    g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
    try {
      GuidebookLevelRenderer.getInstance()
          .render(scene.getLevel(), camera, Collections.emptyList(), LightDarkMode.DARK_MODE);
    } finally {
      g.disableScissor();
      RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
    }
  }
}
