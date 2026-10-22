package io.github.sudabake.appliedagristics.logic;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.*;

/** Reads possible loot, not a random harvest. Uncertain custom functions retain native rules. */
public final class NativeSeedYield {
    private static final Map<ServerLevel,Map<String,Boolean>> CACHE=new WeakHashMap<>();
    public static void invalidate() { synchronized(CACHE) { CACHE.clear(); } }
    public static boolean reproduces(ServerLevel level,ItemStack seed,ItemStack soil) {
        var source=CropSources.find(level,seed); if (source!=null) return source.reproduces();
        if (BioData.seed(seed)) return false;
        var crop=Crops.seed(seed);
        if (crop!=null && soil.getItem() instanceof BlockItem ground)
            return com.blakebr0.mysticalagriculture.config.ModConfigs.SECONDARY_SEED_DROPS.get() && crop.getSecondaryChance(ground.getBlock())>0;
        var plant=Harvests.mature(seed);
        if (plant==null || plant.getBlock() instanceof StemBlock) return false;
        String key=BuiltInRegistries.ITEM.getKey(seed.getItem())+"/"+plant;
        synchronized(CACHE) {
            return CACHE.computeIfAbsent(level,k->new HashMap<>()).computeIfAbsent(key,k->{
                var table=level.getServer().reloadableRegistries().getLootTable(plant.getBlock().getLootTable());
                var json=LootTable.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),table).result();
                if (json.isEmpty()) return true;
                var matches=new ArrayList<Boolean>();
                collect(level,json.get(),BuiltInRegistries.ITEM.getKey(seed.getItem()).toString(),matches,new HashSet<>(),false);
                if (matches.isEmpty()) return false;
                if (matches.size()>1) return true;
                // A single unmodified one-seed drop is entirely reserved for replanting.
                // Any count-changing function is treated conservatively as capable of surplus.
                return matches.getFirst();
            });
        }
    }
    private static void collect(ServerLevel level,JsonElement element,String seed,List<Boolean> matches,Set<String> seen,boolean inherited) {
        if (element.isJsonArray()) { element.getAsJsonArray().forEach(e->collect(level,e,seed,matches,seen,inherited)); return; }
        if (!element.isJsonObject()) return;
        var object=element.getAsJsonObject(); String type=object.has("type") && object.get("type").isJsonPrimitive() ? object.get("type").getAsString() : "";
        boolean variable=inherited || increases(object.get("functions")) || object.has("rolls") && maximum(object.get("rolls"))>1;
        if (type.equals("minecraft:item") && object.has("name") && object.get("name").getAsString().equals(seed)) matches.add(variable);
        if (type.equals("minecraft:loot_table") && object.has("value") && object.get("value").isJsonPrimitive()) {
            String id=object.get("value").getAsString();
            if (seen.add(id)) {
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse(id));
                LootTable.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),level.getServer().reloadableRegistries().getLootTable(key)).result()
                    .ifPresent(e->collect(level,e,seed,matches,seen,true));
            }
        }
        object.entrySet().forEach(e->collect(level,e.getValue(),seed,matches,seen,variable));
    }
    private static boolean increases(JsonElement functions) {
        if (functions==null || !functions.isJsonArray()) return false;
        for (var value:functions.getAsJsonArray()) if (value.isJsonObject()) {
            var function=value.getAsJsonObject(); if (!function.has("function")) return true;
            String id=function.get("function").getAsString();
            if (id.equals("minecraft:set_count")) {
                if (maximum(function.get("count"))>1 || function.has("add") && function.get("add").getAsBoolean()) return true;
            } else if (!id.startsWith("minecraft:") || id.contains("bonus") || id.contains("looting") || id.contains("enchanted_count")) return true;
        }
        return false;
    }
    private static double maximum(JsonElement number) {
        if (number==null) return Double.POSITIVE_INFINITY;
        if (number.isJsonPrimitive() && number.getAsJsonPrimitive().isNumber()) return number.getAsDouble();
        if (number.isJsonObject()) {
            var object=number.getAsJsonObject();
            if (object.has("max")) return maximum(object.get("max"));
            if (object.has("value")) return maximum(object.get("value"));
            if (object.has("n")) return maximum(object.get("n"));
        }
        return Double.POSITIVE_INFINITY;
    }
    private NativeSeedYield() {}
}
