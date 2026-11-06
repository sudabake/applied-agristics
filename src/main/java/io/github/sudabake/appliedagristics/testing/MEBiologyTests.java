package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class MEBiologyTests {
  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void retiredContentIsUnregistered(GameTestHelper h) {
    for (var name : List.of("seed_storage_tier1", "seed_storage_tier2", "cell_cultivator",
        "biological_altar", "culture_capsule", "mature_culture", "bio_extract", "condensed_bio_essence")) {
      var id = ResourceLocation.fromNamespaceAndPath(Agristics.ID, name);
      h.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)
          && !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id),
          "Retired content has no registry entry: " + name);
    }
    h.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(
            ResourceLocation.fromNamespaceAndPath(Agristics.ID, "thrown_culture"))
        && !net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.containsKey(
            ResourceLocation.fromNamespaceAndPath(Agristics.ID, "nourishment"))
        && !net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.containsKey(
            ResourceLocation.fromNamespaceAndPath(Agristics.ID, "bio_seed")),
        "Retired projectile, effect and recipe serializer are unregistered");
    h.assertTrue(h.getLevel().getRecipeManager().getRecipes().stream()
        .noneMatch(recipe -> recipe.id().getNamespace().equals(Agristics.ID)
            && recipe.id().getPath().startsWith("legacy_")), "No migration recipes remain");
    h.succeed();
  }

  private static final ResourceLocation COW = ResourceLocation.parse("minecraft:cow"),
      PIG = ResourceLocation.parse("minecraft:pig");

  private static MEBioCultivator install(GameTestHelper h, PlanterFixture fixture) {
    var pos = fixture.power().above();
    h.getLevel().setBlockAndUpdate(pos, Agristics.ME_BIO.get().defaultBlockState());
    return (MEBioCultivator) h.getLevel().getBlockEntity(pos);
  }

  private static List<ItemStack> inputs(ResourceLocation id) {
    return List.of(
        BioData.withIdentity(Agristics.SAMPLER.get(), id),
        new ItemStack(Agristics.NUTRIENT.get()),
        new ItemStack(Agristics.EMPTY_BIO_SEED.get()));
  }

  private static void fill(MEBioCultivator machine, ResourceLocation id) {
    var inputs = inputs(id);
    for (int i = 0; i < 3; i++) machine.items.setStackInSlot(i, inputs.get(i));
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void processingBlocksPersistsAndConsumesOnce(GameTestHelper h) {
    var fixture = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    var machine = install(h, fixture);
    h.runAtTickTime(
        100,
        () -> {
          fill(machine, COW);
          machine.items.setStackInSlot(3, new ItemStack(Items.WHEAT, 64));
          machine.step();
          h.assertTrue(
              machine.snapshot(0, "").getInt("progress") == 0
                  && machine.items.getStackInSlot(0).getCount() == 1,
              "Full output prevents paid work and consumes no input");
          machine.items.setStackInSlot(3, ItemStack.EMPTY);
          for (int i = 0; i < 37; i++) machine.step();
          var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
          machine.loadWithComponents(saved, h.getLevel().registryAccess());
          h.assertTrue(
              machine.snapshot(0, "").getInt("progress") == 37, "Paid progress survives a reload");
          for (int i = 37; i < Balance.ME_BIO_TICKS.get(); i++) machine.step();
          h.assertTrue(
              machine.items.getStackInSlot(3).getCount() == 1
                  && COW.equals(BioData.identity(machine.items.getStackInSlot(3)))
                  && java.util.stream.IntStream.range(0, 3)
                      .allMatch(i -> machine.items.getStackInSlot(i).isEmpty()),
              "One complete batch consumes exactly three inputs and yields one matching seed");
          machine.step();
          h.assertTrue(
              machine.items.getStackInSlot(3).getCount() == 1, "Completed batch is not repeated");
          h.assertTrue(
              machine
                      .getLevel()
                      .getCapability(
                          net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                          machine.getBlockPos(),
                          Direction.UP)
                  == null,
              "Fresh processor has no water port");
          for (int i = 4; i < 8; i++) {
            machine.items.setStackInSlot(i, appeng.core.definitions.AEItems.SPEED_CARD.stack());
            h.assertTrue(
                machine.items.getSlotLimit(i) == 1, "Each native acceleration slot holds one card");
          }
          machine.items.setStackInSlot(3, ItemStack.EMPTY);
          fill(machine, PIG);
          for (int i = 0; i < 12; i++) machine.step();
          h.assertTrue(
              machine.items.getStackInSlot(3).isEmpty(),
              "Four-card cycle retains its last partial work tick");
          machine.step();
          h.assertTrue(
              PIG.equals(BioData.identity(machine.items.getStackInSlot(3)))
                  && machine.snapshot(0, "").getDouble("power") == 64,
              "Four cards give sixteen times speed and 64 AE/t active demand");
          h.succeed();
        });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void encodedPatternFeedsNetworkSeedBankAndStartsNewSpecies(GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    var machine = install(h, f);
    h.runAtTickTime(
        100,
        () -> {
          var stacks = inputs(COW);
          var output = BioData.withIdentity(Agristics.BIO_SEED.get(), COW);
          var encoded =
              PatternDetailsHelper.encodeProcessingPattern(
                  stacks.stream().map(GenericStack::fromItemStack).toList(),
                  List.of(GenericStack.fromItemStack(output)));
          var pattern = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
          var counters = new KeyCounter[3];
          for (int i = 0; i < 3; i++) {
            counters[i] = new KeyCounter();
            counters[i].add(AEItemKey.of(stacks.get(i)), 1);
          }
          var wrong =
              PatternDetailsHelper.decodePattern(
                  PatternDetailsHelper.encodeProcessingPattern(
                      stacks.stream().map(GenericStack::fromItemStack).toList(),
                      List.of(
                          GenericStack.fromItemStack(
                              BioData.withIdentity(Agristics.BIO_SEED.get(), PIG)))),
                  h.getLevel());
          h.assertTrue(
              !machine.pushPattern(wrong, counters, Direction.NORTH)
                  && machine.items.getStackInSlot(0).isEmpty(),
              "A mismatched species request rejects atomically");
          machine.configureSide(Direction.NORTH.ordinal(), 0);
          h.assertTrue(machine.sideMode(Direction.NORTH.ordinal()) == 1,
              "Disabling face output retains its input capability");
          f.seeds().toggleAutoCollect();
          f.controller().toggleAutoEnableNewSeeds();
          h.assertTrue(
              machine.pushPattern(pattern, counters, Direction.NORTH),
              "Standard AE processing pattern accepts component-bearing samples");
          for (int tick = 0; tick < Balance.ME_BIO_TICKS.get(); tick++) machine.step();
          var key = AEItemKey.of(output);
          h.assertTrue(
              f.seeds().count(key) == 1 && machine.items.getStackInSlot(3).isEmpty(),
              "Pattern result enters the network and the preferred ME seed bank exactly once");
          f.controller().step();
          var id = Cultivations.id(output, h.getLevel());
          var task = f.controller().tasks.get(id);
          h.assertTrue(
              task != null && task.enabled && task.occupied == 1,
              "First arriving species starts automatically when configured");
          f.controller().configure(id, false, -1);
          f.seeds().add(key, 1);
          f.controller().step();
          h.assertTrue(!task.enabled, "A manually paused existing species remains paused");
          var save = f.controller().saveWithFullMetadata(h.getLevel().registryAccess());
          f.controller().loadWithComponents(save, h.getLevel().registryAccess());
          f.controller().step();
          h.assertTrue(
              !f.controller().tasks.get(id).enabled
                  && f.controller().snapshot(0, "").getBoolean("autoEnableNewSeeds"),
              "Preference and manual pause persist through restart");
          h.succeed();
        });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void nutrientReturnsBucket(GameTestHelper h) {
    var input =
        CraftingInput.of(
            3,
            1,
            List.of(
                new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.BONE_MEAL),
                new ItemStack(
                    com.blakebr0.mysticalagriculture.init.ModItems.INFERIUM_ESSENCE.get())));
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
            .orElseThrow()
            .value();
    h.assertTrue(
        recipe.assemble(input, h.getLevel().registryAccess()).is(Agristics.NUTRIENT.get()),
        "Water bucket, bone meal and Inferium produce nutrient reagent");
    var rest = recipe.getRemainingItems(input);
    h.assertTrue(
        rest.get(0).is(Items.BUCKET) && rest.stream().mapToInt(ItemStack::getCount).sum() == 1,
        "Vanilla recipe remainders return exactly one empty bucket");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void autoCollectionMovesNetworkSeedsWithoutReclaimingOwnInventory(
      GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    h.runAtTickTime(
        100,
        () -> {
          f.drive()
              .getInternalInventory()
              .setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
          var key = AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
          var cell = f.drive().getCellInventory(0);
          h.assertTrue(
              cell != null
                  && cell.insert(
                          key,
                          7,
                          Actionable.MODULATE,
                          appeng.api.networking.security.IActionSource.ofMachine(f.controller()))
                      == 7,
              "Seed stock can reside in a normal AE storage cell");
          // Collection must not repeatedly extract the same bank's own free inventory.
          f.seeds().add(key, 11);
          f.seeds().toggleAutoCollect();
          f.seeds().step();
          f.seeds().step();
          h.assertTrue(
              f.seeds().count(key) == 18 && cell.getAvailableStacks().get(key) == 0,
              "Auto collection excludes its own contents and conserves amounts");
          var saved = f.seeds().saveWithFullMetadata(h.getLevel().registryAccess());
          f.seeds().loadWithComponents(saved, h.getLevel().registryAccess());
          h.assertTrue(
              f.seeds().snapshot(0, "").getBoolean("autoCollect"),
              "Auto collection preference survives saves");
          h.succeed();
        });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void collectionFiltersAndCapacitySafeEjection(GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    h.runAtTickTime(
        100,
        () -> {
          var bank = f.seeds();
          var vanilla = AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
          var mystical = AEItemKey.of(new ItemStack(Crops.all().getFirst().getSeedsItem()));
          var bio = AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(), COW));
          bank.setCollectFilter(1);
          h.assertTrue(
              bank.add(vanilla, 3) == 3 && bank.add(mystical, 1) == 0 && bank.add(bio, 1) == 0,
              "Vanilla filter accepts only vanilla incoming seeds");
          bank.setCollectFilter(2);
          h.assertTrue(
              bank.add(mystical, 2) == 2 && bank.add(bio, 1) == 0,
              "Mystical filter accepts registered MA seeds");
          bank.setCollectFilter(3);
          h.assertTrue(
              bank.add(bio, 4) == 4 && bank.add(vanilla, 1) == 0,
              "Biological filter accepts component-bearing biological seeds");
          var save = bank.saveWithFullMetadata(h.getLevel().registryAccess());
          bank.loadWithComponents(save, h.getLevel().registryAccess());
          h.assertTrue(
              bank.count(vanilla) == 3
                  && bank.count(mystical) == 2
                  && bank.snapshot(0, "").getInt("collectFilter") == 3,
              "Changing filters preserves existing stock and saves the selected category");
          bank.setCollectFilter(0);
          var id = Cultivations.id(vanilla.toStack(1), h.getLevel());
          f.controller().configure(id, true, -1);
          f.controller().step();
          bank.toggleAutoCollect();
          bank.ejectSeeds();
          h.assertTrue(
              bank.count(vanilla) == 3
                  && bank.count(bio) == 4
                  && !bank.snapshot(0, "").getBoolean("autoCollect")
                  && !f.controller().tasks.get(id).enabled
                  && bank.used(vanilla) == 0,
              "Full network retains every seed, disables automatic collection and pauses/releases"
                  + " the active crop");
          f.drive()
              .getInternalInventory()
              .setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
          bank.ejectSeeds();
          var cell = f.drive().getCellInventory(0).getAvailableStacks();
          h.assertTrue(
              bank.count(vanilla) == 0
                  && bank.count(mystical) == 0
                  && bank.count(bio) == 0
                  && cell.get(vanilla) == 3
                  && cell.get(mystical) == 2
                  && cell.get(bio) == 4,
              "Ejection bypasses ME seed banks, deposits exact counts into AE storage and preserves"
                  + " species");
          h.succeed();
        });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void plantingPreviewListsWheatTypesAndNormalizesIcons(GameTestHelper h) {
    var rows = io.github.sudabake.appliedagristics.logic.PlantingCatalog.serialize(h.getLevel());
    boolean wheat = false;
    for (var raw : rows) {
      var row = (CompoundTag) raw;
      var seed = ItemStack.parseOptional(h.getLevel().registryAccess(), row.getCompound("seed"));
      var outputs = new ArrayList<ItemStack>();
      for (var output : row.getList("outputs", Tag.TAG_COMPOUND))
        outputs.add(ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) output));
      h.assertTrue(
          outputs.stream().allMatch(stack -> stack.getCount() == 1),
          "Informational output icons do not advertise batch amounts");
      if (seed.is(Items.WHEAT_SEEDS))
        wheat =
            outputs.stream().anyMatch(stack -> stack.is(Items.WHEAT_SEEDS))
                && outputs.stream().anyMatch(stack -> stack.is(Items.WHEAT));
    }
    h.assertTrue(wheat, "Wheat page lists the retained seed and wheat");
    h.succeed();
  }


}
