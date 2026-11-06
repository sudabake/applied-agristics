package io.github.sudabake.appliedagristics.logic;

import appeng.api.stacks.AEItemKey;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Keeps unresolvable entries intact until their item/component becomes available again. */
public final class StoredItemCounts {
    private StoredItemCounts() {}

    public static ListTag load(ListTag entries, Map<AEItemKey, Long> contents,
            HolderLookup.Provider registries) {
        var unresolved = new ListTag();
        for (var raw : entries) {
            var entry = (CompoundTag) raw;
            var key = AEItemKey.fromTag(registries, entry.getCompound("key"));
            long count = entry.getLong("count");
            long previous = key == null ? 0 : contents.getOrDefault(key, 0L);
            if (key == null || key.toStack(1).isEmpty() || count <= 0 || count > Long.MAX_VALUE - previous) {
                unresolved.add(entry.copy());
            } else {
                contents.put(key, previous + count);
            }
        }
        return unresolved;
    }

    public static ListTag save(Map<AEItemKey, Long> contents, ListTag unresolved,
            HolderLookup.Provider registries) {
        var entries = new ListTag();
        contents.forEach((key, count) -> {
            var entry = new CompoundTag();
            entry.put("key", key.toTag(registries));
            entry.putLong("count", count);
            entries.add(entry);
        });
        unresolved.forEach(entry -> entries.add(entry.copy()));
        return entries;
    }
}
