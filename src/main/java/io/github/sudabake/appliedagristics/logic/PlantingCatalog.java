package io.github.sudabake.appliedagristics.logic;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioLootCatalog;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.StemBlock;

/** Read-only possible-output catalog. It never grows blocks or performs a random harvest. */
public final class PlantingCatalog {
  public record Entry(ItemStack seed, List<ItemStack> outputs) {}

  private static final Map<net.minecraft.world.item.crafting.RecipeManager, ListTag> SERVER =
      new WeakHashMap<>();
  private static ListTag client = new ListTag();
  private static int clientVersion;

  private PlantingCatalog() {}

  public static void invalidate() {
    SERVER.clear();
  }

  public static ListTag serialize(ServerLevel level) {
    return SERVER.computeIfAbsent(level.getRecipeManager(), ignored -> build(level)).copy();
  }

  public static int clientVersion() {
    return clientVersion;
  }

  public static ListTag clientTag() {
    return client.copy();
  }

  public static void loadClient(ListTag tag) {
    client = tag.copy();
    clientVersion++;
  }

  public static List<Entry> clientEntries(HolderLookup.Provider registries) {
    var entries = new ArrayList<Entry>();
    for (var raw : client) {
      var entry = (CompoundTag) raw;
      var seed = ItemStack.parseOptional(registries, entry.getCompound("seed"));
      if (seed.isEmpty()) continue;
      var outputs = new ArrayList<ItemStack>();
      for (var output : entry.getList("outputs", Tag.TAG_COMPOUND)) {
        var stack = ItemStack.parseOptional(registries, (CompoundTag) output);
        if (!stack.isEmpty()) outputs.add(stack.copyWithCount(1));
      }
      entries.add(new Entry(seed, List.copyOf(outputs)));
    }
    return entries;
  }

  private static ListTag build(ServerLevel level) {
    var candidates = new LinkedHashMap<AEItemKey, ItemStack>();
    for (var item : BuiltInRegistries.ITEM) {
      if (item == Agristics.BIO_SEED.get()) continue;
      var stack = new ItemStack(item);
      if (Harvests.seed(stack)) candidates.put(AEItemKey.of(stack), stack);
    }
    for (var holder : level.getRecipeManager().getAllRecipesFor(Agristics.PLANTING_TYPE.get()))
      for (var stack : holder.value().seed().getItems())
        if (!stack.isEmpty()) candidates.put(AEItemKey.of(stack), stack.copyWithCount(1));
    var entries = new ListTag();
    for (var seed : candidates.values()) {
      var source = CropSources.find(level, seed);
      var outputs = new LinkedHashMap<AEItemKey, ItemStack>();
      add(outputs, seed);
      if (source != null) {
        add(outputs, source.product());
        if (source.fixedOutputs())
          for (var stack : source.harvest(level, BlockPos.ZERO, source.networkSoil()))
            add(outputs, stack);
      }
      var mature = source == null ? Harvests.mature(seed) : source.display();
      if (mature != null) {
        var block = mature.getBlock();
        if (block instanceof StemBlock stem)
          block =
              BuiltInRegistries.BLOCK.get(
                  ((io.github.sudabake.appliedagristics.mixin.StemAccessor) stem).agrFruit());
        for (var item : BioLootCatalog.possibleTableItems(level, block.getLootTable().location()))
          add(outputs, new ItemStack(item));
      }
      var crop = Crops.seed(seed);
      if (crop != null) {
        add(outputs, new ItemStack(crop.getEssenceItem()));
        if (crop != com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM
            && com.blakebr0.mysticalagriculture.config.ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE
                    .get()
                > 0)
          add(
              outputs,
              new ItemStack(
                  com.blakebr0.mysticalagriculture.init.ModItems.FERTILIZED_ESSENCE.get()));
      }
      var entry = new CompoundTag();
      entry.put("seed", seed.save(level.registryAccess()));
      var list = new ListTag();
      for (var stack : outputs.values()) list.add(stack.save(level.registryAccess()));
      entry.put("outputs", list);
      entries.add(entry);
    }
    return entries;
  }

  private static void add(Map<AEItemKey, ItemStack> outputs, ItemStack stack) {
    if (!stack.isEmpty()) outputs.put(AEItemKey.of(stack), stack.copyWithCount(1));
  }
}
