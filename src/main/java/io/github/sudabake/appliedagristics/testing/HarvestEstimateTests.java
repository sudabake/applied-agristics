package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Balance;
import io.github.sudabake.appliedagristics.logic.HarvestEstimateCache;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class HarvestEstimateTests {
    @GameTest(template="empty", templateNamespace=Agristics.ID)
    public static void failedSamplingAndReloadDoNotReuseStaleYields(GameTestHelper helper) {
        var cache = new HarvestEstimateCache();
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        var seed = new ItemStack(Items.STICK);
        var soil = new ItemStack(Blocks.FARMLAND);
        var state = Blocks.WHEAT.defaultBlockState();
        var calls = new AtomicInteger();
        try {
            cache.expected(level, pos, seed, soil, state, Blocks.WHEAT.getLootTable(), 64, () -> {
                if (calls.incrementAndGet() == 2) throw new IllegalStateException("deliberate sampler failure");
                return List.of(new ItemStack(Items.APPLE));
            });
            helper.fail("The deliberately failed sampler must propagate its failure");
        } catch (IllegalStateException expected) {
            helper.assertTrue(expected.getMessage().equals("deliberate sampler failure"), "Only the intended failure is expected");
        }
        var recovered = cache.expected(level, pos, seed, soil, state, Blocks.WHEAT.getLootTable(), 64,
                () -> List.of(new ItemStack(Items.WHEAT)));
        helper.assertTrue(recovered.size() == 1 && recovered.get(AEItemKey.of(new ItemStack(Items.WHEAT))) == 64,
                "A failed sample group cannot contaminate the committed average");
        HarvestEstimateCache.invalidate();
        var reloaded = cache.expected(level, pos, seed, soil, state, Blocks.WHEAT.getLootTable(), 64,
                () -> List.of(new ItemStack(Items.CARROT)));
        helper.assertTrue(reloaded.size() == 1 && reloaded.get(AEItemKey.of(new ItemStack(Items.CARROT))) == 64,
                "A data reload replaces the old learned yields");
        helper.succeed();
    }
    @GameTest(template="empty", templateNamespace=Agristics.ID)
    public static void learnedRareDropsPersistAndSharedReadsAvoidNewRolls(GameTestHelper helper) {
        var level = helper.getLevel();
        var cache = new HarvestEstimateCache();
        var seed = new ItemStack(Items.STICK);
        var soil = new ItemStack(Blocks.FARMLAND);
        var state = Blocks.WHEAT.defaultBlockState();
        var table = Blocks.WHEAT.getLootTable();
        var calls = new AtomicInteger();
        Supplier<List<ItemStack>> sample = () -> calls.incrementAndGet() % 20 == 0
                ? List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.APPLE))
                : List.of(new ItemStack(Items.WHEAT));
        Map<AEItemKey, Double> expected = Map.of();
        int updates = (int) Math.ceil(65536.0 / Balance.NETWORK_LOOT_SAMPLES.get());
        var pos = helper.absolutePos(BlockPos.ZERO);
        for (int i = 0; i < updates; i++)
            expected = cache.expected(level, pos, seed, soil, state, table, 100, sample);
        int learned = calls.get();
        helper.assertTrue(Math.abs(expected.get(AEItemKey.of(new ItemStack(Items.WHEAT))) - 100) < .00001,
                "Fixed yield remains exact while learning random drops");
        helper.assertTrue(Math.abs(expected.get(AEItemKey.of(new ItemStack(Items.APPLE))) - 5) < .05,
                "Rare drops missed by the initial sample are learned later");
        cache.expected(level, pos, seed.copyWithCount(30), soil, state, table, 100, sample);
        helper.assertTrue(calls.get() == learned, "Changing seed count reuses learned per-harvest yield");
        var saved = cache.save(new net.minecraft.nbt.CompoundTag());
        var restored = HarvestEstimateCache.load(saved);
        var reused = restored.expected(level, pos, seed, soil, state, table, 100, sample);
        helper.assertTrue(reused.equals(expected) && calls.get() == learned,
                "Save/reload retains statistics and does not reroll the next harvest");
        restored.expected(level, pos, new ItemStack(Items.BLAZE_ROD), soil, state, table, 100, sample);
        helper.assertTrue(calls.get() > learned, "Different seed identity starts separate statistics");
        helper.succeed();
    }

    @GameTest(template="empty", templateNamespace=Agristics.ID, timeoutTicks=400)
    public static void learnedYieldRefreshesAfterItsInterval(GameTestHelper helper) {
        var cache = new HarvestEstimateCache();
        var calls = new AtomicInteger();
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        var state = Blocks.WHEAT.defaultBlockState();
        Supplier<List<ItemStack>> sample = () -> {
            calls.incrementAndGet();
            return List.of(new ItemStack(Items.WHEAT));
        };
        int updates = (int) Math.ceil((double) Balance.LOOT_LEARNING_SAMPLES.get() / Balance.NETWORK_LOOT_SAMPLES.get());
        for (int i = 0; i < updates; i++) cache.expected(level, pos, new ItemStack(Items.STICK),
                new ItemStack(Blocks.FARMLAND), state, Blocks.WHEAT.getLootTable(), 64, sample);
        int learned = calls.get();
        helper.runAfterDelay(Balance.LOOT_REFRESH_TICKS.get() + 1, () -> {
            cache.expected(level, pos, new ItemStack(Items.STICK), new ItemStack(Blocks.FARMLAND),
                    state, Blocks.WHEAT.getLootTable(), 64, sample);
            helper.assertTrue(calls.get() == learned + Balance.NETWORK_LOOT_SAMPLES.get(),
                    "A periodic refresh adds only the configured bounded sample count");
            helper.succeed();
        });
    }
}
