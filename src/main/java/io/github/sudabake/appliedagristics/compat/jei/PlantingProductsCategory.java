package io.github.sudabake.appliedagristics.compat.jei;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.logic.PlantingCatalog;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Possible item types, including the retained seed. Quantities remain owned by the production
 * rules.
 */
final class PlantingProductsCategory implements IRecipeCategory<PlantingCatalog.Entry> {
  private final IDrawable icon, arrow;

  PlantingProductsCategory(IGuiHelper gui) {
    icon =
        gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Agristics.EARLY.get()));
    arrow = gui.createAnimatedRecipeArrow(80);
  }

  @Override
  public RecipeType<PlantingCatalog.Entry> getRecipeType() {
    return AgristicsJei.PLANTING;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("jei.appliedagristics.planting_products");
  }

  @Override
  public IDrawable getIcon() {
    return icon;
  }

  @Override
  public int getWidth() {
    return 176;
  }

  @Override
  public int getHeight() {
    return 70;
  }

  @Override
  public void setRecipe(
      IRecipeLayoutBuilder builder, PlantingCatalog.Entry recipe, IFocusGroup focuses) {
    builder
        .addSlot(RecipeIngredientRole.INPUT, 12, 27)
        .setStandardSlotBackground()
        .addItemStack(recipe.seed().copyWithCount(1));
    for (int i = 0; i < recipe.outputs().size(); i++)
      builder
          .addSlot(RecipeIngredientRole.OUTPUT, 88 + i % 4 * 20, 7 + i / 4 * 20)
          .setStandardSlotBackground()
          .addItemStack(recipe.outputs().get(i).copyWithCount(1));
  }

  @Override
  public void draw(
      PlantingCatalog.Entry recipe,
      IRecipeSlotsView slots,
      GuiGraphics graphics,
      double x,
      double y) {
    arrow.draw(graphics, 44, 27);
  }
}
