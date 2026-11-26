package io.github.sudabake.appliedagristics;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import java.util.List;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned production and storage settings. */
public final class Balance {
    public static final ModConfigSpec.IntValue EARLY_TICKS;
    public static final ModConfigSpec.IntValue WATER_TICKS;
    public static final ModConfigSpec.IntValue WATER_COST;
    public static final ModConfigSpec.IntValue LATE_TICKS;
    public static final ModConfigSpec.DoubleValue POWER;
    public static final ModConfigSpec.IntValue PARALLEL_MAX;
    public static final ModConfigSpec.IntValue SPEED_MAX;
    public static final ModConfigSpec.DoubleValue FULL_POWER;
    public static final ModConfigSpec.DoubleValue FALLBACK_SEED_CHANCE;
    public static final ModConfigSpec.LongValue ME_SEEDS_PER_TYPE;
    public static final ModConfigSpec.LongValue CROP_VAULT_CAPACITY;
    public static final ModConfigSpec.IntValue ALTAR_TICKS;
    public static final ModConfigSpec.IntValue ME_BIO_TICKS;
    public static final ModConfigSpec.DoubleValue ME_BIO_POWER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BIO_LOOT_ADDITIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BIO_COLORS;
    public static final ModConfigSpec.BooleanValue CACHE_NETWORK_LOOT;
    public static final ModConfigSpec.IntValue NETWORK_LOOT_SAMPLES;
    public static final ModConfigSpec.IntValue LOOT_LEARNING_SAMPLES;
    public static final ModConfigSpec.IntValue LOOT_REFRESH_TICKS;
    public static final IConfigSpec SPEC;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.comment("Basic planter / 基础种植机",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("basicPlanter");
        EARLY_TICKS = builder.comment("Dry growth cycle in ticks (20 ticks = 1 second).",
                "无水生产周期，单位 tick（20 tick = 1 秒）。")
                .defineInRange("earlyCycleTicks", 200, 20, 1200000);
        WATER_TICKS = builder.comment("Watered cycle in ticks. Provider recipes can have their own cycles.",
                "有水生产周期，单位 tick。兼容配方可使用自己的周期。")
                .defineInRange("wateredCycleTicks", 100, 20, 1200000);
        WATER_COST = builder.comment("Water per accelerated harvest in mB.",
                "每次加速收获的耗水量，单位 mB。")
                .defineInRange("waterPerHarvestMb", 100, 1, 16000);
        builder.pop();

        builder.comment("ME agricultural array / ME 农业阵列",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("agriculturalArray");
        LATE_TICKS = builder.comment("Base harvest cycle in ticks. Datapack and provider recipes can override it.",
                "基础收获周期，单位 tick。数据包和兼容配方可覆盖。")
                .defineInRange("networkCycleTicks", 200, 20, 1200000);
        POWER = builder.comment("Base power per occupied seed in AE/t.",
                "每颗正在工作的种子的基础耗电，单位 AE/t。")
                .defineInRange("aePerCropPerTick", 4.0, 0.01, 1000000.0);
        PARALLEL_MAX = builder.comment("Seed capacity per crop with four parallel modules.",
                "四个并行模块时，每种作物可同时工作的种子数量。")
                .defineInRange("maximumParallel", 256, 256, 65536);
        SPEED_MAX = builder.comment("Speed multiplier with four speed modules. Intermediate tiers follow the existing curve.",
                "四个速度模块时的速度倍率，中间档沿用原有曲线。")
                .defineInRange("maximumSpeedMultiplier", 256, 256, 65536);
        FULL_POWER = builder.comment("Power per fully occupied crop with all upgrades, in AE/t before batch and output limits.",
                "满速满并行时每种作物的耗电，单位 AE/t，实际值受批量和输出限制。")
                .defineInRange("fullUpgradeAePerCropPerTick", 5000.0, 0.01, 1000000.0);
        FALLBACK_SEED_CHANCE = builder.comment("Extra seed probability when the recipe cannot reproduce. 0 disables it; 1 guarantees it.",
                "配方无法增殖时额外返种的概率。0 为关闭，1 为必定返种。")
                .defineInRange("fallbackSeedChance", 0.10, 0, 1);
        builder.pop();

        builder.comment("Storage / 仓储",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("storage");
        ME_SEEDS_PER_TYPE = builder.comment("Maximum items of each seed type per ME Seed Vault. Seed types are unlimited; existing excess can still be withdrawn.",
                "每个 ME 种子仓内每种种子的数量上限。种类不限，旧库存超限仍可取出。")
                .defineInRange("meSeedsPerType", 256L, 1L, Long.MAX_VALUE);
        CROP_VAULT_CAPACITY = builder.comment("Total items per Crop Vault. -1 is unlimited, 0 routes all new harvests to ME. Overflow tries ME first, then Output Ports.",
                "每个作物仓的物品总量上限。-1 为无限，0 时新产物直接尝试进入 ME，网络收不下再走输出口。")
                .defineInRange("cropVaultCapacity", -1L, -1L, Long.MAX_VALUE);
        builder.pop();

        builder.comment("ME processing machines / ME 加工机器",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("meProcessing");
        ALTAR_TICKS = builder.comment("Base ME Infusion Altar cycle in ticks before acceleration cards.",
                "ME 注魔祭坛的基础加工周期，单位 tick，加速卡效果另计。")
                .defineInRange("altarCycleTicks", 100, 20, 1200000);
        ME_BIO_TICKS = builder.comment("Base ME Biological Cultivator cycle in ticks. Each acceleration card doubles speed and working power.",
                "ME 生物培养器的基础周期，单位 tick。每张加速卡使速度和工作耗电翻倍。")
                .defineInRange("meBioCycleTicks", 200, 20, 1200000);
        ME_BIO_POWER = builder.comment("Base ME Biological Cultivator power in AE/t.",
                "ME 生物培养器的基础耗电，单位 AE/t。")
                .defineInRange("meBioAePerTick", 4.0, 0.01, 1000000.0);
        builder.pop();

        builder.comment("Biological crops / 生物作物",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("biology");
        BIO_LOOT_ADDITIONS = builder.comment("Additional biological essence outputs, formatted entity_id=item_id,item_id.",
                "追加生物精华产物，格式为实体 ID=物品 ID,物品 ID。")
                .defineListAllowEmpty("bioLootAdditions", List.of(), value -> value instanceof String);
        BIO_COLORS = builder.comment("Species tint overrides, formatted entity_id=RRGGBB,RRGGBB. Empty uses spawn egg colors.",
                "覆盖物种染色，格式为实体 ID=RRGGBB,RRGGBB。留空沿用刷怪蛋配色。")
                .defineListAllowEmpty("bioColors", List.of(), value -> value instanceof String);
        builder.pop();

        builder.comment("Harvest performance / 收获性能",
                "Restart the world after editing settings. / 修改后请重新进入世界或重启服务器。").push("performance");
        CACHE_NETWORK_LOOT = builder.comment("Reuse learned average loot yields in ME arrays. Small batches and external harvest callbacks are unaffected.",
                "农业阵列复用学习得到的平均掉落。小批量和外部收获回调沿用原有逻辑。")
                .define("cacheNetworkLoot", true);
        NETWORK_LOOT_SAMPLES = builder.comment("Fresh samples per update. More samples reduce variation and cost more CPU time.",
                "每次更新的新样本数。越高波动越小，计算开销越大。")
                .defineInRange("networkLootSamples", 4, 1, 64);
        LOOT_LEARNING_SAMPLES = builder.comment("Initial samples shared by matching crops in one dimension. Statistics persist in saves.",
                "同一维度同类作物共享的初始学习样本数，统计随存档保存。")
                .defineInRange("lootLearningSamples", 4096, 64, 65536);
        LOOT_REFRESH_TICKS = builder.comment("Ticks between sample updates after initial learning.",
                "初始学习完成后，两次采样更新之间的间隔，单位 tick。")
                .defineInRange("lootRefreshTicks", 200, 20, 12000);
        builder.pop();

        SPEC = new MigratingSpec(builder.build());
    }

    private record MigratingSpec(ModConfigSpec delegate) implements IConfigSpec {
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public void validateSpec(ModConfig config) { delegate.validateSpec(config); }
        @Override public boolean isCorrect(UnmodifiableCommentedConfig config) { return delegate.isCorrect(config); }
        @Override public void acceptConfig(ILoadedConfig config) { delegate.acceptConfig(config); }

        @Override
        public void correct(CommentedConfig config) {
            // The loader backs up the file and calls correct before removing obsolete keys.
            delegate.getValues().valueMap().forEach((section, values) -> {
                if (!(values instanceof com.electronwill.nightconfig.core.UnmodifiableConfig group)) return;
                group.valueMap().keySet().forEach(key -> {
                    var path = List.of(section, key);
                    if (config.contains(key) && !config.contains(path)) {
                        config.set(path, config.getRaw(key));
                    }
                });
            });
            delegate.correct(config);
        }
    }

    private Balance() {}
}
