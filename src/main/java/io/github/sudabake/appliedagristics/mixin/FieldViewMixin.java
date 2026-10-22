package io.github.sudabake.appliedagristics.mixin;
import io.github.sudabake.appliedagristics.logic.Harvests;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class FieldViewMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void agrReadField(BlockPos pos, CallbackInfoReturnable<BlockState> callback) {
        var state = Harvests.virtualState((Level)(Object)this, pos); if (state != null) callback.setReturnValue(state);
    }
}
