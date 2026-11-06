package io.github.sudabake.appliedagristics.mixin;

import io.github.sudabake.appliedagristics.compat.jdte.JdteBiologicalCrops;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.jdte.common.recipes.GreenhouseCropResolver",remap=false)
public abstract class JdteCropResolverMixin {
    @Inject(method="findGeneric",at=@At("RETURN"),cancellable=true,require=0)
    private static void agrBiologicalCrop(ItemStack seed,CallbackInfoReturnable<Object> callback) {
        var original=callback.getReturnValue();var adapted=JdteBiologicalCrops.adapt(seed,original);
        if(adapted!=original) callback.setReturnValue(adapted);
    }
}
