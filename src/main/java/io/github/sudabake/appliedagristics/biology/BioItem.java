package io.github.sudabake.appliedagristics.biology;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

public final class BioItem extends Item {
    public BioItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        var id = BioData.identity(stack);
        if (id == null) return super.getName(stack);
        var entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        var name = entity.<Component>map(type -> type.getDescription()).orElse(Component.literal(id.toString()));
        return Component.translatable("gui.appliedagristics.bio_name", super.getName(stack), name);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var id = BioData.identity(stack);
        if (id != null) lines.add(Component.translatable("gui.appliedagristics.bio_identity", id.toString()));
    }
}
