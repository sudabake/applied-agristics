package io.github.sudabake.appliedagristics.mixin;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TreeGrower.class)
public interface TreeGrowerAccessor {
    @Accessor("tree") Optional<ResourceKey<ConfiguredFeature<?,?>>> agristicsTree();
    @Accessor("megaTree") Optional<ResourceKey<ConfiguredFeature<?,?>>> agristicsMegaTree();
    @Accessor("secondaryTree") Optional<ResourceKey<ConfiguredFeature<?,?>>> agristicsSecondaryTree();
    @Accessor("secondaryMegaTree") Optional<ResourceKey<ConfiguredFeature<?,?>>> agristicsSecondaryMegaTree();
}
