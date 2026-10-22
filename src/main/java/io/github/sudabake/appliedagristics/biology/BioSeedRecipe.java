package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Interim seed progression: one extract and one existing prosperity seed. */
public final class BioSeedRecipe extends CustomRecipe {
    public BioSeedRecipe(CraftingBookCategory category) { super(category); }
    private static ItemStack extract(CraftingInput input) {
        ItemStack extract = ItemStack.EMPTY; boolean seed = false;
        for (var stack : input.items()) if (!stack.isEmpty()) {
            if (stack.is(Agristics.EXTRACT.get()) && BioData.identified(stack) && extract.isEmpty()) extract = stack;
            else if (stack.is(com.blakebr0.mysticalagriculture.init.ModItems.PROSPERITY_SEED_BASE.get()) && !seed) seed = true;
            else return ItemStack.EMPTY;
        }
        return seed ? extract : ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput input, Level level) { return !extract(input).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var sample = extract(input); return sample.isEmpty() ? ItemStack.EMPTY : BioData.withIdentity(Agristics.BIO_SEED.get(), BioData.identity(sample));
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.BIO_SEED_RECIPE.get(); }
}
