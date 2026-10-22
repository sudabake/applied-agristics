package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Same biological identity in every occupied slot; shape selects the desired drop. */
public final class BioLootRecipe extends CustomRecipe {
    public BioLootRecipe(CraftingBookCategory category) { super(category); }
    private static int index(CraftingInput input) {
        if (input.width() > 3 || input.height() > 3) return -1;
        ItemStack identity = ItemStack.EMPTY; int mask = 0;
        for (int y = 0; y < input.height(); y++) for (int x = 0; x < input.width(); x++) {
            var stack = input.getItem(x, y); if (stack.isEmpty()) continue;
            if (!stack.is(Agristics.BIO_ESSENCE.get()) || !BioData.identified(stack)) return -1;
            if (identity.isEmpty()) identity = stack;
            else if (!ItemStack.isSameItemSameComponents(identity, stack)) return -1;
            mask |= 1 << (y * 3 + x);
        }
        return BioPatterns.index(mask);
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        int index = index(input); if (index < 0) return false;
        var identity = input.items().stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        var drops=BioLootCatalog.drops(level,BioData.identity(identity));
        return index<drops.size() && drops.get(index)!=net.minecraft.world.item.Items.AIR;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int index = index(input); if (index < 0) return ItemStack.EMPTY;
        var identity = input.items().stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        var drops = BioLootCatalog.drops(registries, BioData.identity(identity));
        return index < drops.size() ? new ItemStack(drops.get(index)) : ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.BIO_LOOT_RECIPE.get(); }
}
