package io.github.sudabake.appliedagristics.mixin;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(StemBlock.class)
public interface StemAccessor { @Accessor("fruit") ResourceKey<Block> agrFruit(); }
