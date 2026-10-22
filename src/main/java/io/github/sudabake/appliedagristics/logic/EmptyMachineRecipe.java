package io.github.sudabake.appliedagristics.logic;

import com.mojang.serialization.MapCodec;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Upgrading a machine must never consume its stored items, water or completed harvest. */
public final class EmptyMachineRecipe extends ShapedRecipe {
    private EmptyMachineRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        for (var stack : input.items()) if (stack.is(Agristics.EARLY.get().asItem())) {
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA); if (data == null) continue;
            var tag = data.copyTag();
            if (!tag.getCompound("inventory").getList("Items", 10).isEmpty() || !tag.getList("pending", 10).isEmpty()
                || !net.neoforged.neoforge.fluids.FluidStack.parseOptional(level.registryAccess(), tag.getCompound("water").getCompound("Fluid")).isEmpty()) return false;
        }
        return true;
    }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.EMPTY_MACHINE_RECIPE.get(); }
    public static final class Serializer implements RecipeSerializer<EmptyMachineRecipe> {
        @Override public MapCodec<EmptyMachineRecipe> codec() { return ShapedRecipe.Serializer.CODEC.xmap(EmptyMachineRecipe::new, recipe -> recipe); }
        @Override public StreamCodec<RegistryFriendlyByteBuf, EmptyMachineRecipe> streamCodec() { return ShapedRecipe.Serializer.STREAM_CODEC.map(EmptyMachineRecipe::new, recipe -> recipe); }
    }
}
