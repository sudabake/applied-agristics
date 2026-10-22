package io.github.sudabake.appliedagristics.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Only explicit recipes and optional providers implement this; native MA/loot remain in Harvests. */
public interface CropSource {
    ItemStack product();
    ItemStack networkSoil();
    BlockState display();
    int ticks();
    default int ticks(net.minecraft.world.level.Level level,BlockPos pos,ItemStack soil) { return ticks(); }
    boolean acceptsSoil(ItemStack soil);
    boolean canGrow(ServerLevel level,BlockPos pos,ItemStack soil);
    boolean reproduces();
    List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil);
    default int maximumBatch() { return 64; }
    default boolean fixedOutputs() { return false; }
}
