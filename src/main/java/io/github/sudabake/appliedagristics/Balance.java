package io.github.sudabake.appliedagristics;

import net.minecraftforge.common.ForgeConfigSpec;

/** Development defaults; all production costs are server-owned and configurable. */
public final class Balance {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue EARLY_TICKS = B.comment("Dry basic-planter cycle, in game ticks (20 ticks = 1 second). Restart the world after changing production settings.").defineInRange("earlyCycleTicks", 200, 20, 1200000);
    public static final ForgeConfigSpec.IntValue WATER_TICKS = B.comment("Watered basic-planter cycle in ticks. Provider-defined planting recipes retain their own cycle rules.").defineInRange("wateredCycleTicks", 100, 20, 1200000);
    public static final ForgeConfigSpec.IntValue WATER_COST = B.comment("Water consumed for one accelerated basic-planter harvest, in mB.").defineInRange("waterPerHarvestMb", 100, 1, 16000);
    public static final ForgeConfigSpec.IntValue LATE_TICKS = B.comment("Work per network harvest in ticks, before speed and occupied parallelism. Data-pack and explicit provider recipes can override it.").defineInRange("networkCycleTicks", 200, 20, 1200000);
    public static final ForgeConfigSpec.DoubleValue POWER = B.comment("AE/t for one occupied seed at base speed, per running crop; not FE/t.").defineInRange("aePerCropPerTick", 4.0, 0.01, 1000000.0);
    public static final ForgeConfigSpec.IntValue PARALLEL_MAX = B.comment("Per-crop seed capacity with four parallel modules. Empty capacity produces nothing.").defineInRange("maximumParallel", 256, 256, 65536);
    public static final ForgeConfigSpec.IntValue SPEED_MAX = B.comment("Speed multiplier with four speed modules. Intermediate module counts follow the existing curved progression.").defineInRange("maximumSpeedMultiplier", 256, 256, 65536);
    public static final ForgeConfigSpec.DoubleValue FULL_POWER = B.comment("AE/t per fully occupied, fully upgraded crop before actual batch/output limits. Running crops add their costs.").defineInRange("fullUpgradeAePerCropPerTick", 5000.0, 0.01, 1000000.0);
    public static final ForgeConfigSpec.DoubleValue FALLBACK_SEED_CHANCE = B.comment("Extra-seed probability per harvest only when the native/provider route cannot reproduce. 0 disables, 1 guarantees one extra seed. Default 0.10 preserves existing behavior.").defineInRange("fallbackSeedChance", .10, 0, 1);
    public static final ForgeConfigSpec.BooleanValue CACHE_NETWORK_LOOT = B.comment("Learn and reuse average native/declared loot yields in ME arrays. Small harvests and external harvest callbacks retain their original path. False restores the previous sampling behavior.").define("cacheNetworkLoot", true);
    public static final ForgeConfigSpec.IntValue NETWORK_LOOT_SAMPLES = B.comment("Fresh loot samples per learning/refresh update, 1 to 64. Higher values reduce short-term statistical variation and increase calculation cost.").defineInRange("networkLootSamples", 4, 1, 64);
    public static final ForgeConfigSpec.IntValue LOOT_LEARNING_SAMPLES = B.comment("Initial samples shared by matching crops in one dimension. Learning is spread across production batches and machines. Statistics persist through dismantling and world saves.").defineInRange("lootLearningSamples", 4096, 64, 65536);
    public static final ForgeConfigSpec.IntValue LOOT_REFRESH_TICKS = B.comment("Game ticks between additional samples after initial learning, shared per crop/context. 200 ticks = 10 seconds. Reload data or restart the world after changing other mods' loot settings.").defineInRange("lootRefreshTicks", 200, 20, 12000);
    public static final ForgeConfigSpec.IntValue ALTAR_TICKS = B.comment("Base ME infusion work in ticks, before acceleration cards. Restart the world to apply production changes.").defineInRange("altarCycleTicks", 100, 20, 1200000);
    public static final ForgeConfigSpec.IntValue ME_BIO_TICKS = B.comment("Base ME biological seed work, in ticks. Four acceleration cards each double work and active power.").defineInRange("meBioCycleTicks",200,20,1200000);
    public static final ForgeConfigSpec.DoubleValue ME_BIO_POWER = B.comment("AE per work tick for the ME biological cultivator. Base 4 AE/t, speed cards multiply active AE/t.").defineInRange("meBioAePerTick",4.0,0.01,1000000.0);
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_LOOT_ADDITIONS = B.comment("Additional biological essence outputs: entity_id=item_id,item_id. Empty by default. Restart the world to rebuild cached loot recipes.").defineListAllowEmpty("bioLootAdditions", java.util.List.of(), value -> value instanceof String);
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_COLORS = B.comment("Species tint overrides: entity_id=RRGGBB,RRGGBB (two hexadecimal colors). Empty uses existing spawn-egg colors. Restart the world to rebuild color caches.").defineListAllowEmpty("bioColors", java.util.List.of(), value -> value instanceof String);
    public static final ForgeConfigSpec.BooleanValue REQUIRE_EFFECTIVE_FARMLAND = B
            .comment("Require the correct Mystical Agriculture farmland in the basic planter.")
            .define("requiresEffectiveFarmland", false);
    public static final ForgeConfigSpec SPEC = B.build();
    private Balance() {}
}
