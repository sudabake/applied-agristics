package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import com.google.gson.GsonBuilder;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Cultivations;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real server ticks in a disposable world, using the same harness before and after optimization. */
@PrefixGameTestTemplate(false)
public final class PlanterPerformanceTest {
    private static final int MACHINES = 100;
    private static final int MEASURE_TICKS = 1200;
    private static int startTick = Integer.MAX_VALUE;
    private static long tickStart;
    private static final List<Long> DURATIONS = new ArrayList<>();

    private static void beforeTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        tickStart = System.nanoTime();
    }

    private static void afterTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int tick = event.getServer().getTickCount();
        if (tick >= startTick && tick < startTick + MEASURE_TICKS)
            DURATIONS.add(System.nanoTime() - tickStart);
    }

    @GameTest(template="empty", templateNamespace=Agristics.ID, timeoutTicks=2200)
    public static void hundredFullyUpgradedArrays(GameTestHelper helper) {
        var level = helper.getLevel();
        level.random.setSeed(742019L);
        DURATIONS.clear();
        startTick = Integer.MAX_VALUE;
        MinecraftForge.EVENT_BUS.addListener(PlanterPerformanceTest::beforeTick);
        MinecraftForge.EVENT_BUS.addListener(PlanterPerformanceTest::afterTick);
        var fixtures = new ArrayList<PlanterFixture>();
        var seeds = seeds();
        helper.assertTrue(seeds.size() == 30, "The pressure test must contain exactly 30 crop identities");
        for (int i = 0; i < MACHINES; i++) {
            var center = helper.absolutePos(new BlockPos(3 + i % 10 * 8, 2, 3 + i / 10 * 8));
            for (int x = -1; x <= 3; x++) {
                var chunk = new net.minecraft.world.level.ChunkPos(center.offset(x, 0, 0));
                level.setChunkForced(chunk.x, chunk.z, true);
            }
            var fixture = PlanterFixture.install(level, center, true);
            fixture.modules(4, 4, false);
            for (var seed : seeds) fixture.seeds().add(AEItemKey.of(seed), 256);
            fixture.seeds().toggleAutoCollect();
            fixtures.add(fixture);
        }
        boolean shared = System.getProperty("appliedagristics.benchmark").contains("shared");
        if (shared) helper.runAtTickTime(80, () -> {
            for (int i = 1; i < fixtures.size(); i++)
                appeng.api.networking.GridHelper.createConnection(
                        fixtures.get(i - 1).access().getMainNode().getNode(),
                        fixtures.get(i).access().getMainNode().getNode());
        });
        helper.runAtTickTime(120, () -> {
            for (var fixture : fixtures) {
                helper.assertTrue(fixture.controller().structure.formed(), "All 100 arrays must be formed");
                helper.assertTrue(fixture.access().getMainNode().isActive(), "All 100 independent AE grids must be active");
                for (var seed : seeds)
                    fixture.controller().configure(Cultivations.id(seed, level), true, -1);
            }
        });
        var initial = new TreeMap<String, Long>();
        var measurement = new LinkedHashMap<String, Object>();
        helper.runAtTickTime(520, () -> {
            verifyRunning(helper, fixtures);
            initial.putAll(inventory(fixtures));
            measurement.put("gc_before", gc());
            measurement.put("power_before", power(fixtures));
            startTick = level.getServer().getTickCount() + 2;
            measurement.put("wall_start_ns", System.nanoTime());
        });
        helper.runAtTickTime(520 + MEASURE_TICKS + 4, () -> {
            verifyRunning(helper, fixtures);
            long wallEnd = System.nanoTime();
            helper.assertTrue(DURATIONS.size() == MEASURE_TICKS, "The benchmark must measure 1200 real server ticks");
            var outputs = inventory(fixtures);
            initial.forEach((key, count) -> outputs.merge(key, -count, Long::sum));
            long wallStart = (long) measurement.remove("wall_start_ns");
            measurement.put("wall_seconds_including_final_checks", (wallEnd - wallStart) / 1e9);
            measurement.put("label", System.getProperty("appliedagristics.benchmark"));
            measurement.put("machines", MACHINES);
            measurement.put("crops_per_machine", seeds.size());
            measurement.put("seeds_per_crop", 256);
            measurement.put("speed_modules", 4);
            measurement.put("parallel_modules", 4);
            measurement.put("conversion_modules", 0);
            measurement.put("auto_collect", true);
            measurement.put("network_topology", shared ? "shared AE grid via API connections" : "independent AE grids");
            measurement.put("warmup_ticks", 400);
            measurement.put("measure_ticks", DURATIONS.size());
            measurement.put("power_after", power(fixtures));
            measurement.put("gc_after", gc());
            var sorted = DURATIONS.stream().mapToLong(Long::longValue).sorted().toArray();
            measurement.put("mean_mspt", DURATIONS.stream().mapToLong(Long::longValue).average().orElseThrow() / 1e6);
            measurement.put("median_mspt", sorted[sorted.length / 2] / 1e6);
            measurement.put("p95_mspt", sorted[(int) (sorted.length * .95)] / 1e6);
            measurement.put("p99_mspt", sorted[(int) (sorted.length * .99)] / 1e6);
            measurement.put("max_mspt", sorted[sorted.length - 1] / 1e6);
            measurement.put("ticks_over_50ms", DURATIONS.stream().filter(value -> value > 50_000_000L).count());
            measurement.put("tick_nanoseconds", List.copyOf(DURATIONS));
            measurement.put("output_deltas", outputs);
            measurement.put("seed_identities", seeds.stream().map(seed -> Cultivations.id(seed, level).toString()).toList());
            measurement.put("loaded_mods", net.minecraftforge.fml.ModList.get().getMods().stream()
                    .map(mod -> mod.getModId() + "=" + mod.getVersion()).sorted().toList());
            measurement.put("java_version", System.getProperty("java.version"));
            measurement.put("maximum_heap_bytes", Runtime.getRuntime().maxMemory());
            try {
                var destination = Path.of("benchmark-result.json");
                Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(measurement));
                com.mojang.logging.LogUtils.getLogger().info("PLANTER_BENCHMARK_PASSED report={}", destination.toAbsolutePath());
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            helper.succeed();
        });
    }

    private static void verifyRunning(GameTestHelper helper, List<PlanterFixture> fixtures) {
        for (var fixture : fixtures) {
            var controller = fixture.controller();
            if (System.getProperty("appliedagristics.benchmark").contains("shared"))
                helper.assertTrue(controller.getMainNode().getGrid() == fixtures.get(0).controller().getMainNode().getGrid(),
                        "Every array must use the same actual AE grid");
            helper.assertTrue(controller.parallel() == 256 && Math.abs(controller.speed() - 256) < .000001,
                    "Modules must remain fully upgraded");
            helper.assertTrue(controller.tasks.size() == 30, "All 30 crops must be configured on every machine");
            for (var task : controller.tasks.values())
                helper.assertTrue(task.enabled && task.occupied == 256 && task.power > 0 && task.status.equals("growing"),
                        "Every measured crop must actually be producing at full occupancy");
        }
    }

    private static double power(List<PlanterFixture> fixtures) {
        return fixtures.stream().flatMap(fixture -> fixture.controller().tasks.values().stream())
                .mapToDouble(task -> task.power).sum();
    }

    private static Map<String, Long> inventory(List<PlanterFixture> fixtures) {
        var result = new TreeMap<String, Long>();
        for (var fixture : fixtures) {
            fixture.vault().mountInventories((storage, priority) -> {
                var amounts = new KeyCounter();
                storage.getAvailableStacks(amounts);
                for (var entry : amounts)
                    result.merge(((AEItemKey) entry.getKey()).toTag().toString(),
                            entry.getLongValue(), Long::sum);
            });
            for (var seed : fixture.seeds().keys())
                result.merge(seed.toTag().toString(),
                        fixture.seeds().count(seed), Long::sum);
        }
        return result;
    }

    private static Map<String, Long> gc() {
        var result = new TreeMap<String, Long>();
        for (var collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            result.put(collector.getName() + "_count", collector.getCollectionCount());
            result.put(collector.getName() + "_milliseconds", collector.getCollectionTime());
        }
        return result;
    }

    private static List<ItemStack> seeds() {
        var seeds = new ArrayList<ItemStack>();
        for (var item : List.of(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS,
                Items.NETHER_WART, Items.COCOA_BEANS, Items.SWEET_BERRIES, Items.MELON_SEEDS,
                Items.PUMPKIN_SEEDS, Items.OAK_SAPLING, Items.SUGAR_CANE, Items.CACTUS,
                Items.RED_MUSHROOM, Items.BROWN_MUSHROOM)) seeds.add(new ItemStack(item));
        for (var name : List.of("inferium", "water", "fire", "dirt"))
            seeds.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    new ResourceLocation("mysticalagriculture:" + name + "_seeds"))));
        for (var name : List.of("cow", "pig", "chicken", "sheep", "zombie", "skeleton",
                "spider", "witch", "enderman", "slime", "blaze", "ghast"))
            seeds.add(BioData.withIdentity(Agristics.BIO_SEED.get(), new ResourceLocation("minecraft:" + name)));
        return seeds;
    }
}
