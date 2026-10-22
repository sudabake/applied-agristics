package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class AgristicsGameTests {
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void earlyWaterAndPersistence(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, Agristics.EARLY.get());
        EarlyPlanter be = h.getBlockEntity(pos); var crop = Crops.all().getFirst();
        be.items.setStackInSlot(0, new ItemStack(crop.getSeedsItem())); be.items.setStackInSlot(1, new ItemStack(Blocks.FARMLAND));
        for (int i = 0; i < 9; i++) be.step();
        h.assertTrue(be.items.getStackInSlot(2).isEmpty(), "Dry cycle does not finish before ten seconds"); be.step();
        h.assertTrue(be.items.getStackInSlot(2).getCount() == 1, "Dry harvest must work without water");
        be.water.fill(new FluidStack(Fluids.WATER, 200), FluidAction.EXECUTE);
        for (int i = 0; i < 5; i++) be.step();
        h.assertTrue(be.items.getStackInSlot(2).getCount() == 2, "Water must shorten the cycle");
        h.assertTrue(be.water.getFluidAmount() == 100, "Only the boosted harvest consumes water");
        for (int slot = 2; slot < 11; slot++) be.items.setStackInSlot(slot, new ItemStack(crop.getEssenceItem(), 64));
        for (int i = 0; i < 10; i++) be.step();
        h.assertTrue(be.water.getFluidAmount() == 100 && be.items.getStackInSlot(2).getCount() == 64, "Blocked output must not consume water or produce");
        var drop = new ItemStack(Agristics.EARLY.get()); be.saveToItem(drop, h.getLevel().registryAccess());
        var restored = new EarlyPlanter(be.getBlockPos(), be.getBlockState()); restored.setLevel(h.getLevel());
        restored.loadWithComponents(drop.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(), h.getLevel().registryAccess());
        h.assertTrue(restored.items.getStackInSlot(2).getCount() == 64 && restored.water.getFluidAmount() == 100, "Saved machine retains inventory and water");
        h.assertTrue(be.items.getStackInSlot(0).getCount() == 1, "Seed is preserved");
        var saved = drop.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        h.assertTrue(saved.getList("pending", 10).equals(restored.saveWithFullMetadata(h.getLevel().registryAccess()).getList("pending", 10)), "Blocked random harvest survives save/load without rerolling");
        var legacy = new net.neoforged.neoforge.items.ItemStackHandler(3);
        legacy.setStackInSlot(0, new ItemStack(crop.getSeedsItem())); legacy.setStackInSlot(1, new ItemStack(Blocks.FARMLAND)); legacy.setStackInSlot(2, new ItemStack(crop.getEssenceItem(), 4));
        saved.put("inventory", legacy.serializeNBT(h.getLevel().registryAccess())); restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(restored.items.getSlots() == 12 && restored.items.getStackInSlot(2).getCount() == 4, "Legacy three-slot inventory expands without losing items"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void nativeHarvestRules(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, Agristics.EARLY.get()); EarlyPlanter be = h.getBlockEntity(pos);
        for (var seed : java.util.List.of(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS)) {
            for (int i = 0; i < 11; i++) be.items.setStackInSlot(i, ItemStack.EMPTY);
            be.items.setStackInSlot(0, new ItemStack(seed)); be.items.setStackInSlot(1, new ItemStack(Blocks.FARMLAND));
            for (int i = 0; i < 10; i++) be.step();
            int count = 0; for (int i = 2; i < 11; i++) count += be.items.getStackInSlot(i).getCount();
            h.assertTrue(count > 0 && be.items.getStackInSlot(0).getCount() == 1, "Native seed harvest/replant: " + seed);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")) {
            for (var id : java.util.List.of("cabbage_seeds", "tomato_seeds", "onion")) {
                var seed = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("farmersdelight", id));
                h.assertTrue(seed != Items.AIR, "Compatibility fixture seed exists: " + id);
                for (int i = 0; i < 11; i++) be.items.setStackInSlot(i, ItemStack.EMPTY);
                be.items.setStackInSlot(0, new ItemStack(seed)); be.items.setStackInSlot(1, new ItemStack(Blocks.FARMLAND));
                for (int i = 0; i < 10; i++) be.step();
                int outputs = 0; for (int i = 2; i < 11; i++) outputs += be.items.getStackInSlot(i).getCount();
                h.assertTrue(outputs > 0, "Farmer's Delight native harvest: " + id + " status=" + be.status);
            }
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("FARMERS_DELIGHT_COMPAT_PASSED crops=3");
        }
        if (net.neoforged.fml.ModList.get().isLoaded("mysticalagradditions")) {
            var star = Crops.all().stream().filter(c -> c.getId().getPath().equals("nether_star")).findFirst().orElseThrow();
            for (int i = 0; i < 12; i++) be.items.setStackInSlot(i, ItemStack.EMPTY);
            be.items.setStackInSlot(0, new ItemStack(star.getSeedsItem())); be.items.setStackInSlot(1, io.github.sudabake.appliedagristics.logic.Harvests.networkSoil());
            h.setBlock(pos.below(), star.getCruxBlock()); for (int i = 0; i < 10; i++) be.step();
            h.assertTrue(be.items.getStackInSlot(2).is(star.getEssenceItem()), "Actual Mystical Agradditions nether star seed grows with its crux in the basic planter");
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("NETHER_STAR_CRUX_COMPAT_PASSED");
        }
        var config = com.blakebr0.mysticalagriculture.config.ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE;
        var secondary = com.blakebr0.mysticalagriculture.config.ModConfigs.SECONDARY_SEED_DROPS;
        double previous = config.get(); boolean oldSecondary = secondary.get();
        var crop = Crops.all().stream().filter(c -> c != com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM && c.getCruxBlock() == null && c.getRequiredBiomes().isEmpty()).findFirst().orElseThrow();
        double oldChance = crop.getBaseSecondaryChance();
        try {
            config.set(1.0); secondary.set(true); crop.setBaseSecondaryChance(1.0);
            var soil = new ItemStack(crop.getTier().getFarmland());
            var drops = io.github.sudabake.appliedagristics.logic.Harvests.harvest(h.getLevel(), be.getBlockPos(), new ItemStack(crop.getSeedsItem()), soil);
            h.assertTrue(drops.stream().anyMatch(d -> d.is(com.blakebr0.mysticalagriculture.init.ModItems.FERTILIZED_ESSENCE.get())), "Fertilizer follows upstream config");
            h.assertTrue(drops.stream().anyMatch(d -> d.is(crop.getSeedsItem()) && d.getCount() == 1), "Extra seed remains after replanting reserve");
            h.assertTrue(h.getLevel().getBlockEntity(be.getBlockPos()) == be, "Virtual soil leaves the real machine intact");
            var inferium = com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM;
            var tiers = com.blakebr0.mysticalagriculture.block.InfusedFarmlandBlock.FARMLANDS;
            var highest = tiers.stream().max(java.util.Comparator.comparingInt(f -> f.getTier().getValue())).orElseThrow();
            var boosted = io.github.sudabake.appliedagristics.logic.Harvests.harvest(h.getLevel(), be.getBlockPos(), new ItemStack(inferium.getSeedsItem()), new ItemStack(highest));
            h.assertTrue(boosted.stream().filter(d -> d.is(inferium.getEssenceItem())).mapToInt(ItemStack::getCount).sum() >= 3, "High-tier farmland increases inferium yield");
        } finally { config.set(previous); secondary.set(oldSecondary); crop.setBaseSecondaryChance(oldChance); }
        h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void ordinaryNetworkAndOwnedList(GameTestHelper h) {
        MultiblockScenarios.ordinaryOwnedList(h);
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void networkReservationAndStockTarget(GameTestHelper h) {
        MultiblockScenarios.reservationAndTargets(h);
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void networkUpgradesConversionAndTerminalStorage(GameTestHelper h) {
        MultiblockScenarios.modulesAndConversion(h);
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void basicCruxAndLargeBatches(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, Agristics.EARLY.get()); EarlyPlanter early = h.getBlockEntity(pos);
        var crop = Crops.all().getFirst(); var original = crop.getCruxBlock();
        try {
            crop.setCruxBlock(() -> Blocks.DIAMOND_BLOCK);
            var seed = new ItemStack(crop.getSeedsItem()); var soil = io.github.sudabake.appliedagristics.logic.Harvests.networkSoil();
            early.items.setStackInSlot(0, seed); early.items.setStackInSlot(1, soil); early.step();
            h.assertTrue(early.status.equals("wrong_farmland"), "Basic special seed requires its crux");
            h.setBlock(pos.below(), Blocks.DIAMOND_BLOCK); for (int i = 0; i < 10; i++) early.step();
            h.assertTrue(!early.items.getStackInSlot(2).isEmpty(), "Basic planter requires the physically placed crux below it");
            var oldInventory = new net.neoforged.neoforge.items.ItemStackHandler(1); oldInventory.setStackInSlot(0, soil);
            var legacy = new net.minecraft.nbt.CompoundTag(); legacy.put("inventory", oldInventory.serializeNBT(h.getLevel().registryAccess()));
            var restored = new NetworkPlanter(early.getBlockPos(), Agristics.NETWORK.get().defaultBlockState()); restored.setLevel(h.getLevel()); restored.loadWithComponents(legacy, h.getLevel().registryAccess());
            h.assertTrue(restored.items.getSlots() == 4 && restored.items.getStackInSlot(0).isEmpty() && ItemStack.isSameItemSameComponents(restored.items.getStackInSlot(3), soil), "Old ME soil moves to the reserved slot without deletion");
            long start = System.nanoTime();
            var large = io.github.sudabake.appliedagristics.logic.Harvests.networkBatch(h.getLevel(), early.getBlockPos(), seed, 107_374_182L, new java.util.HashMap<>());
            h.assertTrue(large.get(AEItemKey.of(new ItemStack(crop.getEssenceItem()))) >= 107_374_182L, "Large parallel batches use long counts instead of item entities");
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("LARGE_ALGEBRA_BATCH cycles=107374182 elapsedMs={}", (System.nanoTime() - start) / 1_000_000.0);
            h.succeed();
        } finally { crop.setCruxBlock(original == null ? null : () -> original); }
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void craftingProgressionAndSafeMachineUpgrade(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        for (var id : java.util.List.of("early_planter", "seed_storage", "network_planter")) {
            var recipe = recipes.stream().filter(r -> r.id().toString().equals(Agristics.ID + ":" + id)).findFirst().orElseThrow().value();
            h.assertTrue(!recipe.isIncomplete(), "All progression ingredients are registered: " + id);
        }
        for (var id : java.util.List.of("compact_altar", "essence_cell", "awakening_upgrade", "speed_upgrade", "parallel_upgrade", "seed_conversion_upgrade"))
            h.assertTrue(recipes.stream().noneMatch(r -> r.id().toString().equals(Agristics.ID + ":" + id)), "Retired item has no new crafting recipe: " + id);
        var recipe = (net.minecraft.world.item.crafting.ShapedRecipe)recipes.stream().filter(r -> r.id().toString().equals(Agristics.ID + ":network_planter")).findFirst().orElseThrow().value();
        var stacks = new java.util.ArrayList<ItemStack>(); for (var ingredient : recipe.getIngredients()) stacks.add(ingredient.getItems()[0].copyWithCount(1));
        int machineSlot = -1; for (int i = 0; i < stacks.size(); i++) if (stacks.get(i).is(Agristics.EARLY.get().asItem())) machineSlot = i;
        h.assertTrue(machineSlot >= 0, "ME recipe uses the basic planter");
        var machineItem = stacks.get(machineSlot); var early = new EarlyPlanter(h.absolutePos(new BlockPos(1, 1, 1)), Agristics.EARLY.get().defaultBlockState()); early.setLevel(h.getLevel());
        early.saveToItem(machineItem, h.getLevel().registryAccess());
        var input = net.minecraft.world.item.crafting.CraftingInput.of(recipe.getWidth(), recipe.getHeight(), stacks);
        h.assertTrue(recipe.matches(input, h.getLevel()), "An empty placed-and-retrieved basic planter can be upgraded");
        early.items.setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS)); early.saveToItem(machineItem, h.getLevel().registryAccess());
        h.assertTrue(!recipe.matches(input, h.getLevel()), "Crafting cannot destroy seeds inside a basic planter");
        early.items.setStackInSlot(0, ItemStack.EMPTY); early.water.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE); early.saveToItem(machineItem, h.getLevel().registryAccess());
        h.assertTrue(!recipe.matches(input, h.getLevel()), "Crafting cannot destroy stored water");
        h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void manyCropsBatchPerformance(GameTestHelper h) {
        MultiblockScenarios.manyCrops(h);
    }
}
