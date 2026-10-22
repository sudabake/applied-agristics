package io.github.sudabake.appliedagristics.mixin;

import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AE transient-recipe fallback normally forgets components via Ingredient.of(stack). */
@Mixin(value = FillCraftingGridFromRecipePacket.class, remap = false)
public abstract class BiologicalTransferMixin {
    @Shadow @Final private NonNullList<ItemStack> ingredientTemplates;
    @Shadow @Final private ResourceLocation recipeId;
    @Inject(method = "getDesiredIngredients", at = @At("RETURN"), remap = false)
    private void preserveBiologicalIdentity(Player player, CallbackInfoReturnable<NonNullList<Ingredient>> callback) {
        // Only the fallback path; actual server recipe ingredients remain authoritative.
        if (recipeId != null && player.level().getRecipeManager().byKey(recipeId).isPresent()) return;
        var ingredients = callback.getReturnValue();
        for (int slot = 0; slot < Math.min(ingredients.size(), ingredientTemplates.size()); slot++) {
            var stack = ingredientTemplates.get(slot);
            if (BioData.identified(stack) && (stack.is(Agristics.BIO_ESSENCE.get()) || stack.is(Agristics.BIO_SEED.get()) || stack.is(Agristics.EXTRACT.get())
                    || stack.is(Agristics.CULTURE.get()) || stack.is(Agristics.SAMPLER.get())))
                ingredients.set(slot, DataComponentIngredient.of(true, stack));
        }
    }
}
