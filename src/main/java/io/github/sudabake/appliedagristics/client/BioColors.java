package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Agristics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BioColors {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(Agristics.BIO_ESSENCE.get(),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Agristics.ID, "boss"), (stack, level, entity, seed) -> {
                var id = BioData.identity(stack);
                return id == null ? 0 : id.toString().equals("minecraft:wither") ? 1 : id.toString().equals("minecraft:ender_dragon") ? 2 : 0;
            }));
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register(BioColors::color, Agristics.BIO_SEED.get(), Agristics.BIO_ESSENCE.get());
    }
    public static int color(ItemStack stack, int layer) {
        if (layer >= 2) return 0xFFFFFFFF;
        var id = BioData.identity(stack); if (id == null) return 0xFFB0B0B0;
        if (stack.is(Agristics.BIO_ESSENCE.get()) && (id.toString().equals("minecraft:wither") || id.toString().equals("minecraft:ender_dragon"))) return 0xFFFFFFFF;
        for (String setting : Balance.BIO_COLORS.get()) {
            var parts = setting.split("=", 2); if (parts.length != 2 || !parts[0].equals(id.toString())) continue;
            var values = parts[1].split(",");
            try { return 0xFF000000 | Integer.parseUnsignedInt(values[Math.min(layer, values.length - 1)].trim().replace("#", ""), 16) & 0xFFFFFF; }
            catch (NumberFormatException ignored) {}
        }
        var egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));
        if (egg != null) return 0xFF000000 | egg.getColor(layer);
        if (id.toString().equals("minecraft:wither")) return layer == 0 ? 0xFF56545D : 0xFF92C9B6;
        if (id.toString().equals("minecraft:ender_dragon")) return layer == 0 ? 0xFF514466 : 0xFFD59BED;
        return 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(Math.floorMod(id.hashCode(), 360) / 360f, layer == 0 ? .45f : .7f, layer == 0 ? .8f : 1f);
    }
}
