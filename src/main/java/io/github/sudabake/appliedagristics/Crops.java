package io.github.sudabake.appliedagristics;

import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.blakebr0.mysticalagriculture.registry.CropRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.FarmBlock;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;

public final class Crops {
    private Crops() {}
    private record Index(int size, Map<Item, Crop> seeds, Set<Item> essences) {}
    private static volatile Index currentIndex = new Index(-1, Map.of(), Set.of());
    private static Index index() {
        var crops = CropRegistry.getInstance().getCrops();
        var existing = currentIndex; if (existing.size == crops.size()) return existing;
        var seeds = new IdentityHashMap<Item, Crop>();
        Set<Item> essences = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (var crop : crops) { seeds.put(crop.getSeedsItem(), crop); essences.add(crop.getEssenceItem()); }
        // Publish a complete immutable snapshot; integrated client and server can initialize concurrently.
        var next = new Index(crops.size(), java.util.Collections.unmodifiableMap(seeds), java.util.Collections.unmodifiableSet(essences));
        currentIndex = next; return next;
    }
    public static List<Crop> all() { return CropRegistry.getInstance().getCrops().stream().filter(Crop::isEnabled).toList(); }
    public static Crop seed(ItemStack stack) {
        if (stack.isEmpty()) return null;
        var crop = index().seeds.get(stack.getItem()); return crop != null && crop.isEnabled() ? crop : null;
    }
    public static boolean essence(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Agristics.BIO_ESSENCE.get()) && io.github.sudabake.appliedagristics.biology.BioData.identified(stack)) return true;
        if (index().essences.contains(stack.getItem())) return true;
        // Tier and awakening essences need storage too, not just harvested resource essences.
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("mysticalagriculture") && id.getPath().endsWith("_essence");
    }
    public static boolean farmland(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FarmBlock;
    }
}
