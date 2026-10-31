package io.github.sudabake.appliedagristics.biology;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Ordinary BlockItem placement preserves Forge placement checks and the identity component. */
public final class BioSeedItem extends ItemNameBlockItem {
    public BioSeedItem(Block plant,Properties properties) { super(plant,properties); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return BioData.identified(context.getItemInHand()) ? super.getPlacementState(context) : null;
    }
    @Override public Component getName(ItemStack stack) { return BioItem.identityName(stack,super.getName(stack)); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        var id=BioData.identity(stack);
        if (id!=null && flag.isAdvanced()) lines.add(Component.translatable("gui.appliedagristics.bio_identity",id.toString()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
