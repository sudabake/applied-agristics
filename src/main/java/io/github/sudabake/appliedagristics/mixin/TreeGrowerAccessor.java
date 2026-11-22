package io.github.sudabake.appliedagristics.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractTreeGrower.class)
public interface TreeGrowerAccessor {
    @Invoker("getConfiguredFeature")
    ResourceKey<ConfiguredFeature<?, ?>> agristicsTree(RandomSource random, boolean flowers);
}
