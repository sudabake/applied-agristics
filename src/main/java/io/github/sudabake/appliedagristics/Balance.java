package io.github.sudabake.appliedagristics;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Development defaults; all production costs are server-owned and configurable. */
public final class Balance {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue EARLY_TICKS = B.defineInRange("earlyCycleTicks", 200, 20, 1200000);
    public static final ModConfigSpec.IntValue WATER_TICKS = B.defineInRange("wateredCycleTicks", 100, 20, 1200000);
    public static final ModConfigSpec.IntValue WATER_COST = B.defineInRange("waterPerHarvestMb", 100, 1, 16000);
    public static final ModConfigSpec.IntValue LATE_TICKS = B.defineInRange("networkCycleTicks", 200, 20, 1200000);
    public static final ModConfigSpec.DoubleValue POWER = B.defineInRange("aePerCropPerTick", 4.0, 0.01, 1000000.0);
    public static final ModConfigSpec.IntValue PARALLEL_MAX = B.defineInRange("maximumParallel", 256, 256, 65536);
    public static final ModConfigSpec.IntValue SPEED_MAX = B.defineInRange("maximumSpeedMultiplier", 256, 256, 65536);
    public static final ModConfigSpec.DoubleValue FULL_POWER = B.defineInRange("fullUpgradeAePerCropPerTick", 5000.0, 0.01, 1000000.0);
    public static final ModConfigSpec.IntValue ALTAR_TICKS = B.defineInRange("altarCycleTicks", 100, 20, 1200000);
    public static final ModConfigSpec.IntValue CULTURE_TICKS = B.defineInRange("cultureCycleTicks", 400, 20, 1200000);
    public static final ModConfigSpec.IntValue CULTURE_WATER = B.defineInRange("cultureWaterMb", 100, 1, 16000);
    public static final ModConfigSpec.IntValue NOURISHMENT_TICKS = B.defineInRange("nourishmentTicks",400,100,1200000);
    public static final ModConfigSpec.IntValue EXTRACT_TICKS = B.defineInRange("cultureExtractionTicks", 100, 20, 1200000);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_LOOT_ADDITIONS = B.defineListAllowEmpty("bioLootAdditions", java.util.List.of(), value -> value instanceof String);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_COLORS = B.defineListAllowEmpty("bioColors", java.util.List.of(), value -> value instanceof String);
    public static final ModConfigSpec SPEC = B.build();
    private Balance() {}
}
