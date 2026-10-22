package io.github.sudabake.appliedagristics.biology;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Enumerate item possibilities, not a random kill. Code-only drops use explicit additions. */
@EventBusSubscriber(modid = Agristics.ID)
public final class BioLootCatalog {
    private static final Map<HolderLookup.Provider, Map<ResourceLocation, List<Item>>> SERVER = new WeakHashMap<>();
    private static volatile Map<ResourceLocation, List<Item>> client = Map.of();
    private BioLootCatalog() {}
    public static List<Item> clientDrops(ResourceLocation entity) { return client.getOrDefault(entity, List.of()); }
    public static Map<ResourceLocation, List<Item>> clientCatalog() { return client; }
    /** Optional client integrations can refresh after server loot data arrives or reloads. */
    public static final class ClientCatalogUpdated extends net.neoforged.bus.api.Event {}
    public static List<Item> drops(Level level, ResourceLocation entity) {
        if (entity == null) return List.of();
        return level instanceof ServerLevel server ? catalog(server).getOrDefault(entity, List.of()) : clientDrops(entity);
    }
    public static List<Item> drops(HolderLookup.Provider registries, ResourceLocation entity) {
        synchronized (SERVER) { var data = SERVER.get(registries); return data == null ? clientDrops(entity) : data.getOrDefault(entity, List.of()); }
    }
    private static Map<ResourceLocation, List<Item>> catalog(ServerLevel level) {
        synchronized (SERVER) { return SERVER.computeIfAbsent(level.registryAccess(), p -> build(level)); }
    }
    private static Map<ResourceLocation, List<Item>> build(ServerLevel level) {
        var result = new LinkedHashMap<ResourceLocation, List<Item>>();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type); var items = new TreeMap<String, Item>();
            try {
                var entity = type.create(level); if (!(entity instanceof Mob mob)) continue;
                collectTable(level, mob.getLootTable().location(), items, new HashSet<>());
                // Variant sub-tables, e.g. all sheep wool colors.
                var prefix = id.getPath().equals("sheep") && id.getNamespace().equals("minecraft") ? "entities/sheep/" : "entities/" + id.getPath() + "/";
                for (var key : level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE))
                    if (key.getNamespace().equals(id.getNamespace()) && key.getPath().startsWith(prefix)) collectTable(level, key, items, new HashSet<>());
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger("AppliedAgristics").warn("Unable to enumerate biological loot for {}", id, exception);
            }
            // These are created by entity/world code instead of ordinary loot table entries.
            if (id.toString().equals("minecraft:wither")) add(items, ResourceLocation.parse("minecraft:nether_star"));
            if (id.toString().equals("minecraft:ender_dragon")) add(items, ResourceLocation.parse("minecraft:dragon_egg"));
            List<Item> special = switch (id.toString()) {
                case "minecraft:zombie" -> List.of(Items.ZOMBIE_HEAD, Items.IRON_SWORD, Items.IRON_SHOVEL);
                case "minecraft:husk", "minecraft:zombie_villager" -> List.of(Items.IRON_SWORD, Items.IRON_SHOVEL);
                case "minecraft:drowned" -> List.of(Items.TRIDENT, Items.NAUTILUS_SHELL, Items.FISHING_ROD);
                case "minecraft:skeleton" -> List.of(Items.SKELETON_SKULL, Items.BOW);
                case "minecraft:stray", "minecraft:bogged" -> List.of(Items.BOW);
                case "minecraft:wither_skeleton" -> List.of(Items.STONE_SWORD);
                case "minecraft:creeper" -> List.of(Items.CREEPER_HEAD);
                case "minecraft:piglin" -> List.of(Items.PIGLIN_HEAD, Items.CROSSBOW, Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
                case "minecraft:piglin_brute" -> List.of(Items.GOLDEN_AXE);
                case "minecraft:zombified_piglin" -> List.of(Items.GOLDEN_SWORD);
                case "minecraft:pillager" -> List.of(Items.CROSSBOW);
                case "minecraft:vindicator" -> List.of(Items.IRON_AXE);
                default -> List.of();
            };
            for (var item : special) add(items, BuiltInRegistries.ITEM.getKey(item));
            for (String override : Balance.BIO_LOOT_ADDITIONS.get()) {
                var parts = override.split("=", 2); if (parts.length == 2 && parts[0].equals(id.toString()))
                    for (String item : parts[1].split(",")) { var key = ResourceLocation.tryParse(item.trim()); if (key != null) add(items, key); }
            }
            result.put(id, List.copyOf(items.values()));
        }
        return BioLootOrder.get(level).reconcile(result);
    }
    private static void add(Map<String, Item> output, ResourceLocation id) {
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR); if (item != Items.AIR) output.put(id.toString(), item);
    }
    private static void collectTable(ServerLevel level, ResourceLocation id, Map<String, Item> output, Set<ResourceLocation> visited) {
        if (!visited.add(id)) return;
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        var ops = level.getServer().reloadableRegistries().get().createSerializationContext(JsonOps.INSTANCE);
        LootTable.DIRECT_CODEC.encodeStart(ops, table).result().ifPresent(json -> {
            collect(level, json, output, visited);
            if (json.toString().contains("minecraft:furnace_smelt")) for (var item : new ArrayList<>(output.values()))
                level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(item)), level)
                    .ifPresent(recipe -> add(output, BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(level.registryAccess()).getItem())));
        });
    }
    private static void collect(ServerLevel level, JsonElement json, Map<String, Item> output, Set<ResourceLocation> visited) {
        if (json.isJsonArray()) { for (var value : json.getAsJsonArray()) collect(level, value, output, visited); return; }
        if (!json.isJsonObject()) return;
        var object = json.getAsJsonObject(); String type = object.has("type") ? object.get("type").getAsString() : "";
        if (object.has("name") && object.get("name").isJsonPrimitive()) {
            var name = ResourceLocation.tryParse(object.get("name").getAsString());
            if (name != null) switch (type) {
                case "minecraft:item" -> add(output, name);
                case "minecraft:tag" -> BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(Registries.ITEM, name)).ifPresent(tag -> tag.forEach(item -> add(output, BuiltInRegistries.ITEM.getKey(item.value()))));
                case "minecraft:loot_table" -> collectTable(level, name, output, visited);
                default -> {}
            }
        }
        if (type.equals("minecraft:loot_table") && object.has("value") && object.get("value").isJsonPrimitive()) {
            var name = ResourceLocation.tryParse(object.get("value").getAsString()); if (name != null) collectTable(level, name, output, visited);
        }
        for (var entry : object.entrySet()) if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) collect(level, entry.getValue(), output, visited);
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) synchronized (SERVER) { SERVER.clear(); }
        var data = catalog(event.getPlayerList().getServer().overworld()); var tag = new CompoundTag(); var entities = new ListTag();
        data.forEach((id, items) -> {
            var entry = new CompoundTag(); entry.putString("entity", id.toString()); var list = new ListTag();
            for (var item : items) list.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString())); entry.put("items", list); entities.add(entry);
        }); tag.put("entities", entities);
        var payload = new Messages.BiologicalCatalog(tag);
        if (event.getPlayer() != null) PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        else for (var player : event.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, payload);
    }
    public static void loadClient(CompoundTag tag) {
        var result = new HashMap<ResourceLocation, List<Item>>();
        for (var value : tag.getList("entities", 10)) {
            var entry = (CompoundTag)value; var id = ResourceLocation.tryParse(entry.getString("entity")); if (id == null) continue;
            var items = new ArrayList<Item>(); for (var raw : entry.getList("items", 8)) {
                var key = ResourceLocation.tryParse(raw.getAsString());
                items.add(key==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR));
            }
            result.put(id, List.copyOf(items));
        }
        client = Map.copyOf(result);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ClientCatalogUpdated());
    }
}
