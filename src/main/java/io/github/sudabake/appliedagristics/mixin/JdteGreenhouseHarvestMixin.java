package io.github.sudabake.appliedagristics.mixin;

import io.github.sudabake.appliedagristics.compat.jdte.JdteBiologicalCrops;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supply component-aware samples to JDTE's own batch planner and settlement. */
@Pseudo
@Mixin(targets = {"com.jdte.common.blockentities.GreenhouseBE",
    "com.jdte.common.blockentities.LargeGreenhouseBE"}, remap = false)
public abstract class JdteGreenhouseHarvestMixin implements JdteBiologicalCrops.BatchHost {
  @Inject(method = "generateSingleHarvest", at = @At("HEAD"), cancellable = true, require = 0)
  private void agristics$sample(ServerLevel level, @Coerce Object definition, ItemStack tool,
      CallbackInfoReturnable<List<ItemStack>> callback) {
    if (definition instanceof JdteBiologicalCrops.Definition biological) {
      var seed = biological.agristics$getSeed();
      if (!seed.isEmpty()) callback.setReturnValue(JdteBiologicalCrops.harvest(level, seed));
    }
  }
}
