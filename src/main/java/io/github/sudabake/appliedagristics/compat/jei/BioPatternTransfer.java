package io.github.sudabake.appliedagristics.compat.jei;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.util.*;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Uses AE's encoder and packets, including exact species components, for ordinary processing
 * patterns.
 */
final class BioPatternTransfer
    implements IRecipeTransferHandler<PatternEncodingTermMenu, AgristicsJei.Process> {
  @Override
  public Class<? extends PatternEncodingTermMenu> getContainerClass() {
    return PatternEncodingTermMenu.class;
  }

  @Override
  public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
    return Optional.empty();
  }

  @Override
  public RecipeType<AgristicsJei.Process> getRecipeType() {
    return AgristicsJei.CULTURE;
  }

  @Override
  public IRecipeTransferError transferRecipe(
      PatternEncodingTermMenu menu,
      AgristicsJei.Process recipe,
      IRecipeSlotsView slots,
      Player player,
      boolean maxTransfer,
      boolean doTransfer) {
    if (doTransfer) {
      var inputs =
          List.of(
              BioData.withIdentity(Agristics.SAMPLER.get(), recipe.entity()),
              new ItemStack(Agristics.NUTRIENT.get()),
              new ItemStack(Agristics.EMPTY_BIO_SEED.get()));
      EncodingHelper.encodeProcessingRecipe(
          menu,
          inputs.stream().map(stack -> List.of(GenericStack.fromItemStack(stack))).toList(),
          List.of(
              GenericStack.fromItemStack(
                  BioData.withIdentity(Agristics.BIO_SEED.get(), recipe.entity()))));
    }
    return null;
  }
}
