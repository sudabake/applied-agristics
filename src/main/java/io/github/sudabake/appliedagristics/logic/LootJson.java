package io.github.sudabake.appliedagristics.logic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;

/** Inspect the loot table already loaded by the server, including datapack overrides. */
public final class LootJson {
    private static final Gson GSON = Deserializers.createLootTableSerializer().create();

    private LootJson() {}

    public static JsonElement encode(LootTable table) {
        return GSON.toJsonTree(table, LootTable.class);
    }
}
