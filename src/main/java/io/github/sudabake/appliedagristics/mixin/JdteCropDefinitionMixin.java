package io.github.sudabake.appliedagristics.mixin;

import io.github.sudabake.appliedagristics.compat.jdte.JdteBiologicalCrops;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runtime-only identity for our fallback definitions; explicit JDTE recipes remain unmarked. */
@Pseudo
@Mixin(targets = "com.jdte.common.recipes.GreenhouseCropDefinition", remap = false)
public abstract class JdteCropDefinitionMixin implements JdteBiologicalCrops.Definition {
  @Unique private ItemStack agristics$seed = ItemStack.EMPTY;

  @Override
  public ItemStack agristics$getSeed() {
    return agristics$seed.copy();
  }

  @Override
  public void agristics$setSeed(ItemStack seed) {
    agristics$seed = seed.copyWithCount(1);
  }

  @Inject(method = "generateHarvest", at = @At("HEAD"), cancellable = true, require = 0)
  private void agristics$harvest(ServerLevel level, BlockPos pos, ItemStack tool,
      CallbackInfoReturnable<List<ItemStack>> callback) {
    if (!agristics$seed.isEmpty())
      callback.setReturnValue(JdteBiologicalCrops.harvest(level, agristics$seed));
  }
}
