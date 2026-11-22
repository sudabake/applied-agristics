package io.github.sudabake.appliedagristics.logic;

import com.google.gson.JsonObject;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Upgrading a machine must never consume its stored items, water or completed harvest. */
public final class EmptyMachineRecipe extends ShapedRecipe {
    private EmptyMachineRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
            recipe.getIngredients(), recipe.getResultItem(null), recipe.showNotification());
    }
    @Override public boolean matches(CraftingContainer input, Level level) {
        if (!super.matches(input, level)) return false;
        for (var stack : input.getItems()) if (stack.is(Agristics.EARLY.get().asItem()) || stack.is(Agristics.INPUT_PORT.get().asItem())) {
            var tag = BlockItem.getBlockEntityData(stack); if (tag == null) continue;
            if (!tag.getCompound("inventory").getList("Items", 10).isEmpty() || !tag.getList("pending", 10).isEmpty()
                || !net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(tag.getCompound("water")).isEmpty()) return false;
        }
        return true;
    }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.EMPTY_MACHINE_RECIPE.get(); }
    public static final class Serializer implements RecipeSerializer<EmptyMachineRecipe> {
        @Override public EmptyMachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new EmptyMachineRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }
        @Override public EmptyMachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new EmptyMachineRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }
        @Override public void toNetwork(FriendlyByteBuf buf, EmptyMachineRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
