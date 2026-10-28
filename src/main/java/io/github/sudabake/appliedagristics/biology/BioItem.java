package io.github.sudabake.appliedagristics.biology;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

public class BioItem extends Item {
    public BioItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        return identityName(stack,super.getName(stack));
    }
    public static Component identityName(ItemStack stack,Component baseName) {
        var id = BioData.identity(stack);
        if (id == null) return baseName;
        var entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        var name = entity.<Component>map(type -> type.getDescription()).orElse(Component.literal(id.toString()));
        return Component.translatable("gui.appliedagristics.bio_name", baseName, name);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var id = BioData.identity(stack);
        if (id != null) lines.add(Component.translatable("gui.appliedagristics.bio_identity", id.toString()));
    }
}
