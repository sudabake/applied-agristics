package io.github.sudabake.appliedagristics.mixin;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SaplingBlock.class)
public interface SaplingAccessor {
    @Accessor("treeGrower") TreeGrower agristicsTreeGrower();
}
