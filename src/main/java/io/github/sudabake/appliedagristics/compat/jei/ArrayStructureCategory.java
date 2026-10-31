package io.github.sudabake.appliedagristics.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.client.ArrayGuide;
import io.github.sudabake.appliedagristics.client.ArrayScene;
import io.github.sudabake.appliedagristics.multiblock.PlanterLayout;
import java.util.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ArrayStructureCategory implements IRecipeCategory<String> {
  private final IDrawable icon;
  private final ArrayScene scene = new ArrayScene();

  public ArrayStructureCategory(IGuiHelper gui) {
    icon =
        gui.createDrawableIngredient(
            VanillaTypes.ITEM_STACK, new ItemStack(Agristics.NETWORK.get()));
  }

  @Override
  public RecipeType<String> getRecipeType() {
    return AgristicsJei.STRUCTURE;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("jei.appliedagristics.structure");
  }

  @Override
  public IDrawable getIcon() {
    return icon;
  }

  @Override
  public int getWidth() {
    return 260;
  }

  @Override
  public int getHeight() {
    return 146;
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, String recipe, IFocusGroup focus) {
    var counts = new LinkedHashMap<PlanterLayout.Role, Integer>();
    for (var role : PlanterLayout.example()) counts.merge(role, 1, Integer::sum);
    int i = 0;
    for (var entry : counts.entrySet())
      builder
          .addSlot(RecipeIngredientRole.INPUT, 7 + 23 * i++, 127)
          .setStandardSlotBackground()
          .addItemStack(new ItemStack(ArrayScene.block(entry.getKey()), entry.getValue()));
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, String recipe, IFocusGroup focus) {
    // JEI handles hit testing, focus and drag dispatch; GuideME owns the camera.
    builder.addInputHandler(
        new IJeiInputHandler() {
          @Override
          public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, 150, 90);
          }

          @Override
          public boolean handleInput(double x, double y, IJeiUserInput input) {
            return input.getKey().getType() == InputConstants.Type.MOUSE
                && input.getKey().getValue() == 0;
          }

          @Override
          public boolean handleMouseDragged(
              double x, double y, InputConstants.Key button, double dx, double dy) {
            if (button.getType() != InputConstants.Type.MOUSE || button.getValue() != 0)
              return false;
            scene.drag(dx, dy);
            return true;
          }
        });
  }

  @Override
  public List<Component> getTooltipStrings(
      String recipe, IRecipeSlotsView slots, double x, double y) {
    return x >= 0 && x < 150 && y >= 0 && y < 90
        ? List.of(Component.translatable("jei.appliedagristics.drag_rotate"))
        : List.of();
  }

  @Override
  public void draw(
      String recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
    var font = Minecraft.getInstance().font;
    scene.draw(g, 0, 0, 150, 90);
    for (int i = 0; i < 6; i++)
      g.drawString(
          font,
          Component.translatable("jei.appliedagristics.array_line" + i),
          151,
          5 + i * 14,
          0x404040,
          false);
    g.fill(1, 94, 68, 111, 0xff505966);
    g.fill(73, 94, 166, 111, 0xff505966);
    g.fill(171, 94, 259, 111, 0xff505966);
    g.drawString(
        font, Component.translatable("jei.appliedagristics.rotate"), 7, 99, 0xffffff, false);
    var label =
        scene.selectedLayer() < 0
            ? Component.translatable("jei.appliedagristics.all_layers")
            : Component.translatable("jei.appliedagristics.layer", scene.selectedLayer() + 1);
    g.drawString(font, label, 79, 99, 0xffffff, false);
    g.drawString(
        font, Component.translatable("jei.appliedagristics.guide"), 177, 99, 0xffffff, false);
    g.drawString(
        font,
        Component.translatable("jei.appliedagristics.example_materials"),
        5,
        115,
        0x404040,
        false);
  }

  @Override
  public boolean handleInput(String recipe, double x, double y, InputConstants.Key input) {
    if (input.getType() != InputConstants.Type.MOUSE
        || input.getValue() != 0
        || x < 0
        || x > 260
        || y < 94
        || y > 111) return false;
    if (x < 69) scene.rotate();
    else if (x < 167) scene.layer();
    else ArrayGuide.open();
    return true;
  }
}
