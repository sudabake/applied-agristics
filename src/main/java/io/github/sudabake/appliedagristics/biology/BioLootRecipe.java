package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Same biological identity in every occupied slot; shape selects the desired drop. */
public final class BioLootRecipe extends CustomRecipe {
    public BioLootRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    private static int index(CraftingContainer input) {
        if (input.getWidth() > 3 || input.getHeight() > 3) return -1;
        ItemStack identity = ItemStack.EMPTY;
        int left = input.getWidth(), top = input.getHeight();
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            var stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!stack.is(Agristics.BIO_ESSENCE.get()) || !BioData.identified(stack)) return -1;
            if (identity.isEmpty()) identity = stack;
            else if (!ItemStack.isSameItemSameTags(identity, stack)) return -1;
            left = Math.min(left, slot % input.getWidth());
            top = Math.min(top, slot / input.getWidth());
        }
        if (identity.isEmpty()) return -1;
        int mask = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            if (!input.getItem(slot).isEmpty()) {
                mask |= 1 << ((slot / input.getWidth() - top) * 3 + slot % input.getWidth() - left);
            }
        }
        return BioPatterns.index(mask);
    }
    @Override public boolean matches(CraftingContainer input, Level level) {
        int index = index(input); if (index < 0) return false;
        var identity = input.getItems().stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        var drops=BioLootCatalog.drops(level,BioData.identity(identity));
        return index<drops.size() && drops.get(index)!=net.minecraft.world.item.Items.AIR;
    }
    @Override public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        int index = index(input); if (index < 0) return ItemStack.EMPTY;
        var identity = input.getItems().stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        var drops = BioLootCatalog.drops(registries, BioData.identity(identity));
        return index < drops.size() ? new ItemStack(drops.get(index)) : ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.BIO_LOOT_RECIPE.get(); }
}
