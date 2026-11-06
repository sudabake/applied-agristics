package io.github.sudabake.appliedagristics;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Development defaults; all production costs are server-owned and configurable. */
public final class Balance {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue EARLY_TICKS = B.comment("Dry basic-planter cycle, in game ticks (20 ticks = 1 second). Restart the world after changing production settings.").defineInRange("earlyCycleTicks", 200, 20, 1200000);
    public static final ModConfigSpec.IntValue WATER_TICKS = B.comment("Watered basic-planter cycle in ticks. Provider-defined planting recipes retain their own cycle rules.").defineInRange("wateredCycleTicks", 100, 20, 1200000);
    public static final ModConfigSpec.IntValue WATER_COST = B.comment("Water consumed for one accelerated basic-planter harvest, in mB.").defineInRange("waterPerHarvestMb", 100, 1, 16000);
    public static final ModConfigSpec.IntValue LATE_TICKS = B.comment("Work per network harvest in ticks, before speed and occupied parallelism. Data-pack and explicit provider recipes can override it.").defineInRange("networkCycleTicks", 200, 20, 1200000);
    public static final ModConfigSpec.DoubleValue POWER = B.comment("AE/t for one occupied seed at base speed, per running crop; not FE/t.").defineInRange("aePerCropPerTick", 4.0, 0.01, 1000000.0);
    public static final ModConfigSpec.IntValue PARALLEL_MAX = B.comment("Per-crop seed capacity with four parallel modules. Empty capacity produces nothing.").defineInRange("maximumParallel", 256, 256, 65536);
    public static final ModConfigSpec.IntValue SPEED_MAX = B.comment("Speed multiplier with four speed modules. Intermediate module counts follow the existing curved progression.").defineInRange("maximumSpeedMultiplier", 256, 256, 65536);
    public static final ModConfigSpec.DoubleValue FULL_POWER = B.comment("AE/t per fully occupied, fully upgraded crop before actual batch/output limits. Running crops add their costs.").defineInRange("fullUpgradeAePerCropPerTick", 5000.0, 0.01, 1000000.0);
    public static final ModConfigSpec.DoubleValue FALLBACK_SEED_CHANCE = B.comment("Extra-seed probability per harvest only when the native/provider route cannot reproduce. 0 disables, 1 guarantees one extra seed. Default 0.10 preserves existing behavior.").defineInRange("fallbackSeedChance", .10, 0, 1);
    public static final ModConfigSpec.IntValue ALTAR_TICKS = B.comment("Base ME infusion work in ticks, before acceleration cards. Restart the world to apply production changes.").defineInRange("altarCycleTicks", 100, 20, 1200000);
    public static final ModConfigSpec.IntValue ME_BIO_TICKS = B.comment("Base ME biological seed work, in ticks. Four acceleration cards each double work and active power.").defineInRange("meBioCycleTicks",200,20,1200000);
    public static final ModConfigSpec.DoubleValue ME_BIO_POWER = B.comment("AE per work tick for the ME biological cultivator. Base 4 AE/t, speed cards multiply active AE/t.").defineInRange("meBioAePerTick",4.0,0.01,1000000.0);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_LOOT_ADDITIONS = B.comment("Additional biological essence outputs: entity_id=item_id,item_id. Empty by default. Restart the world to rebuild cached loot recipes.").defineListAllowEmpty("bioLootAdditions", java.util.List.of(), value -> value instanceof String);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIO_COLORS = B.comment("Species tint overrides: entity_id=RRGGBB,RRGGBB (two hexadecimal colors). Empty uses existing spawn-egg colors. Restart the world to rebuild color caches.").defineListAllowEmpty("bioColors", java.util.List.of(), value -> value instanceof String);
    public static final ModConfigSpec SPEC = B.build();
    private Balance() {}
}
