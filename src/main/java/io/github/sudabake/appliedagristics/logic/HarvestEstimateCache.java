package io.github.sudabake.appliedagristics.logic;

import appeng.api.stacks.AEItemKey;
import com.mojang.serialization.JsonOps;
import io.github.sudabake.appliedagristics.Balance;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootTable;

/** Derived per-dimension yield statistics. Inventory and in-flight harvests remain machine-owned. */
public final class HarvestEstimateCache extends SavedData {
    private static final String NAME = "appliedagristics_harvest_estimates";
    private static final int SCHEMA = 1;
    private static final int MAX_ENTRIES = 1024;
    private static final int RARE_LEARNING_LIMIT = 65536;
    private static final int RARE_OBSERVATIONS = 1024;
    private static final int MAX_HISTORY = 1_000_000;
    private static final Factory<HarvestEstimateCache> FACTORY =
            new Factory<>(HarvestEstimateCache::new, HarvestEstimateCache::load);
    private static long generation;
    private long currentGeneration = generation;
    private String savedEnvironment = "";
    private String environment;
    private final Map<ResourceKey<LootTable>, String> tableSignatures = new HashMap<>();
    private final LinkedHashMap<Key, Estimate> entries = new LinkedHashMap<>(16, .75f, true);

    private record Key(AEItemKey seed, AEItemKey soil, String state, ResourceLocation biome,
                       ResourceKey<LootTable> table) {}

    private static final class Estimate {
        String signature;
        int samples;
        long lastSample;
        final Map<AEItemKey, Double> totals = new LinkedHashMap<>();

        boolean learning() {
            if (samples < Balance.LOOT_LEARNING_SAMPLES.get()) return true;
            return samples < RARE_LEARNING_LIMIT && totals.values().stream()
                    .anyMatch(count -> count > 0 && count < RARE_OBSERVATIONS && count / samples < .25);
        }
    }

    public static HarvestEstimateCache get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static void invalidate() {
        generation++;
    }

    public Map<AEItemKey, Double> expected(ServerLevel level, BlockPos pos, ItemStack seed,
            ItemStack soil, BlockState state, ResourceKey<LootTable> table, long cycles,
            Supplier<List<ItemStack>> sampler) {
        if (cycles <= 0) return Map.of();
        if (currentGeneration != generation) {
            entries.clear();
            tableSignatures.clear();
            environment = null;
            currentGeneration = generation;
            setDirty();
        }
        if (environment == null) {
            environment = environmentFingerprint(level);
            if (!savedEnvironment.equals(environment)) {
                entries.clear();
                savedEnvironment = environment;
                setDirty();
            }
        }
        String signature = tableSignatures.computeIfAbsent(table, key -> {
            var loot = level.getServer().reloadableRegistries().getLootTable(key);
            return LootTable.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), loot)
                    .result().map(json -> fingerprint(json.toString())).orElse("");
        });
        var key = new Key(AEItemKey.of(seed), AEItemKey.of(soil), state.toString(),
                level.getBiome(pos).unwrapKey().orElseThrow().location(), table);
        var estimate = entries.computeIfAbsent(key, ignored -> new Estimate());
        if (!signature.equals(estimate.signature)) {
            estimate.signature = signature;
            estimate.samples = 0;
            estimate.totals.clear();
        }
        long now = level.getGameTime();
        if (estimate.learning()
                || now < estimate.lastSample || now - estimate.lastSample >= Balance.LOOT_REFRESH_TICKS.get()) {
            // Bound historical weight so new samples continue to influence long-lived worlds.
            if (estimate.samples >= MAX_HISTORY) {
                double factor = (double) (estimate.samples / 2) / estimate.samples;
                estimate.samples /= 2;
                estimate.totals.replaceAll((item, count) -> count * factor);
            }
            int samples = (int) Math.min(cycles, Balance.NETWORK_LOOT_SAMPLES.get());
            var additions = new LinkedHashMap<AEItemKey, Double>();
            for (int i = 0; i < samples; i++) {
                for (var drop : sampler.get())
                    if (!drop.isEmpty()) additions.merge(AEItemKey.of(drop), (double) drop.getCount(), Double::sum);
            }
            additions.forEach((item, count) -> estimate.totals.merge(item, count, Double::sum));
            estimate.samples += samples;
            estimate.lastSample = now;
            setDirty();
        }
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.keySet().iterator().next());
        var result = new LinkedHashMap<AEItemKey, Double>();
        estimate.totals.forEach((item, count) -> result.put(item, count / estimate.samples * cycles));
        return result;
    }

    private static String fingerprint(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String environmentFingerprint(ServerLevel level) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            net.neoforged.fml.ModList.get().getMods().stream().sorted(Comparator.comparing(info -> info.getModId()))
                    .forEach(info -> digest.update((info.getModId() + "=" + info.getVersion() + "\n").getBytes(StandardCharsets.UTF_8)));
            var resources = new TreeMap<>(level.getServer().getResourceManager().listResources("loot_modifiers", id -> id.getPath().endsWith(".json")));
            resources.putAll(level.getServer().getResourceManager().listResources("loot_table", id -> id.getPath().endsWith(".json")));
            resources.putAll(level.getServer().getResourceManager().listResources("worldgen/configured_feature", id -> id.getPath().endsWith(".json")));
            for (var entry : new TreeMap<>(resources).entrySet()) {
                digest.update(entry.getKey().toString().getBytes(StandardCharsets.UTF_8));
                try (var stream = entry.getValue().open()) { digest.update(stream.readAllBytes()); }
            }
            for (var directory : List.of(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(),
                    level.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig"))) {
                if (!Files.isDirectory(directory)) continue;
                try (var files = Files.list(directory)) {
                    for (var file : files.filter(path -> path.getFileName().toString().endsWith(".toml")).sorted().toList()) {
                        digest.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(Files.readAllBytes(file));
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        } catch (IOException exception) {
            // A fresh identity keeps stale disk statistics out when settings cannot be read.
            return UUID.randomUUID().toString();
        }
    }

    public static HarvestEstimateCache load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new HarvestEstimateCache();
        if (tag.getInt("schema") != SCHEMA) return data;
        data.savedEnvironment = tag.getString("environment");
        for (var raw : tag.getList("estimates", Tag.TAG_COMPOUND)) {
            var saved = (CompoundTag) raw;
            var seed = AEItemKey.fromTag(registries, saved.getCompound("seed"));
            var soil = AEItemKey.fromTag(registries, saved.getCompound("soil"));
            var biome = ResourceLocation.tryParse(saved.getString("biome"));
            var table = ResourceLocation.tryParse(saved.getString("table"));
            int samples = saved.getInt("samples");
            if (seed == null || biome == null || table == null || samples <= 0 || samples > MAX_HISTORY + 63) continue;
            var estimate = new Estimate();
            estimate.signature = saved.getString("signature");
            estimate.samples = samples;
            estimate.lastSample = saved.getLong("lastSample");
            for (var value : saved.getList("totals", Tag.TAG_COMPOUND)) {
                var total = (CompoundTag) value;
                var item = AEItemKey.fromTag(registries, total.getCompound("key"));
                double count = total.getDouble("count");
                if (item != null && Double.isFinite(count) && count >= 0) estimate.totals.put(item, count);
            }
            data.entries.put(new Key(seed, soil, saved.getString("state"), biome,
                    ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table)), estimate);
            if (data.entries.size() >= MAX_ENTRIES) break;
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA);
        tag.putString("environment", savedEnvironment);
        var records = new ListTag();
        entries.forEach((key, estimate) -> {
            var entry = new CompoundTag();
            entry.put("seed", key.seed.toTag(registries));
            if (key.soil != null) entry.put("soil", key.soil.toTag(registries));
            entry.putString("state", key.state);
            entry.putString("biome", key.biome.toString());
            entry.putString("table", key.table.location().toString());
            entry.putString("signature", estimate.signature);
            entry.putInt("samples", estimate.samples);
            entry.putLong("lastSample", estimate.lastSample);
            var totals = new ListTag();
            estimate.totals.forEach((item, count) -> {
                var total = new CompoundTag();
                total.put("key", item.toTag(registries));
                total.putDouble("count", count);
                totals.add(total);
            });
            entry.put("totals", totals);
            records.add(entry);
        });
        tag.put("estimates", records);
        return tag;
    }
}
