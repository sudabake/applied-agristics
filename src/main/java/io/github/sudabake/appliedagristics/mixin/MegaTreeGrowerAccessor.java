package io.github.sudabake.appliedagristics.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMegaTreeGrower.class)
public interface MegaTreeGrowerAccessor {
    @Invoker("getConfiguredMegaFeature")
    ResourceKey<ConfiguredFeature<?, ?>> agristicsMegaTree(RandomSource random);
}
