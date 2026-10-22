package io.github.sudabake.appliedagristics.biology;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** World-owned, append-only pattern slots. Missing drops leave tombstones instead of shifting recipes. */
public final class BioLootOrder extends SavedData {
    private static final Factory<BioLootOrder> FACTORY=new Factory<>(BioLootOrder::new,BioLootOrder::load);
    private final Map<ResourceLocation,List<ResourceLocation>> slots=new LinkedHashMap<>();

    public static BioLootOrder get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY,"appliedagristics_bio_loot_order");
    }
    public Map<ResourceLocation,List<Item>> reconcile(Map<ResourceLocation,List<Item>> current) {
        var result=new LinkedHashMap<ResourceLocation,List<Item>>();
        current.forEach((entity,items)->{
            var active=new HashSet<ResourceLocation>();
            for(var item:items) if(item!=Items.AIR) active.add(BuiltInRegistries.ITEM.getKey(item));
            var order=slots.computeIfAbsent(entity,k->new ArrayList<>());
            var known=new HashSet<>(order);
            active.stream().sorted(Comparator.comparing(ResourceLocation::toString)).filter(id->!known.contains(id)).forEach(id->{order.add(id);setDirty();});
            result.put(entity,order.stream().map(id->active.contains(id)?BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR):Items.AIR).toList());
        });
        return Collections.unmodifiableMap(result);
    }
    public static BioLootOrder load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new BioLootOrder();
        for(var raw:tag.getList("entities",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)raw;var entity=ResourceLocation.tryParse(entry.getString("entity"));if(entity==null) continue;
            var order=new ArrayList<ResourceLocation>();
            for(var value:entry.getList("slots",Tag.TAG_STRING)) {
                // Keep IDs even when their mod is absent, so reinstalling it restores the same slot.
                var id=ResourceLocation.tryParse(value.getAsString());if(id!=null) order.add(id);
            }
            data.slots.put(entity,order);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        var entries=new ListTag();
        slots.forEach((entity,order)->{
            var entry=new CompoundTag();entry.putString("entity",entity.toString());var values=new ListTag();
            order.forEach(id->values.add(StringTag.valueOf(id.toString())));entry.put("slots",values);entries.add(entry);
        });
        tag.put("entities",entries);return tag;
    }
}
