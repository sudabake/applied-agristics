package io.github.sudabake.appliedagristics.mixin;

import io.github.sudabake.appliedagristics.compat.jdte.JdteBiologicalCrops;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.jdte.common.recipes.GreenhouseCropResolver",remap=false)
public abstract class JdteCropResolverMixin {
    // JDTE 0.5.8 returns explicit, MA and Botany Pots recipes before this generic fallback.
    @Inject(method = "find", slice = @Slice(from = @At(value = "INVOKE",
        target = "Lcom/jdte/common/integrations/BotanyPotsGreenhouseIntegration;find(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lcom/jdte/common/recipes/GreenhouseCropDefinition;")),
        at = @At(value = "RETURN", ordinal = 1), cancellable = true, require = 0)
    private static void agrBiologicalCrop(Level level, ItemStack seed, CallbackInfoReturnable<Object> callback) {
        var original=callback.getReturnValue();var adapted=JdteBiologicalCrops.adapt(seed,original);
        if(adapted!=original) callback.setReturnValue(adapted);
    }
}
