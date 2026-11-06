package io.github.sudabake.appliedagristics.block;

import appeng.api.config.*;
import appeng.api.stacks.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single-species AE processor. Inputs remain visible and are consumed only with the finished seed.
 */
public final class MEBioCultivator extends AEProcessingMachine {
  private List<ItemStack> work = List.of();
  private double progress;
  private boolean finishing;
  public MEBioCultivator(BlockPos pos, BlockState state) {
    super(Agristics.ME_BIO_BE.get(), pos, state, 8, 3, 4);
  }

  @Override
  protected boolean valid(int slot, ItemStack stack) {
    return switch (slot) {
      case 0 -> stack.is(Agristics.SAMPLER.get()) && BioData.identified(stack);
      case 1 -> stack.is(Agristics.NUTRIENT.get());
      case 2 -> stack.is(Agristics.EMPTY_BIO_SEED.get());
      default -> slot >= 4 && slot < 8 && isAcceleration(stack);
    };
  }

  private boolean ready() {
    return valid(0, items.getStackInSlot(0))
        && valid(1, items.getStackInSlot(1))
        && valid(2, items.getStackInSlot(2));
  }

  private List<ItemStack> inputs() {
    return java.util.stream.IntStream.range(0, 3)
        .mapToObj(i -> items.getStackInSlot(i).copyWithCount(1))
        .toList();
  }

  private boolean sameWork() {
    if (work.size() != 3 || !ready()) return false;
    for (int slot = 0; slot < 3; slot++)
      if (!ItemStack.isSameItemSameComponents(work.get(slot), items.getStackInSlot(slot)))
        return false;
    return true;
  }

  @Override
  protected void inventoryChanged(int slot) {
    if (!finishing && slot < 3 && !work.isEmpty() && !sameWork()) {
      work = List.of();
      progress = 0;
    }
    if (slot == 3 && items.getStackInSlot(3).isEmpty()) craftingDelivery = false;
  }

  private ItemStack result() {
    return BioData.withIdentity(
        Agristics.BIO_SEED.get(), BioData.identity(items.getStackInSlot(0)));
  }

  @Override
  public void step() {
    deliverOutput();
    if (!getMainNode().isActive()) {
      status = "offline";
      return;
    }
    if (!ready()) {
      status = "missing_inputs";
      return;
    }
    if (!sameWork()) {
      work = inputs();
      progress = 0;
      setChanged();
    }
    var output = result();
    var existing = items.getStackInSlot(3);
    if (!existing.isEmpty()
        && (!ItemStack.isSameItemSameComponents(existing, output)
            || existing.getCount() >= existing.getMaxStackSize())) {
      status = "output_full";
      return;
    }
    if (progress < Balance.ME_BIO_TICKS.get()) {
      double workTicks = Math.min(speed(), Balance.ME_BIO_TICKS.get() - progress);
      double cost = workTicks * Balance.ME_BIO_POWER.get();
      var energy = getMainNode().getGrid().getEnergyService();
      if (energy.extractAEPower(cost, Actionable.SIMULATE, PowerMultiplier.CONFIG) + 1e-7 < cost) {
        status = "no_power";
        return;
      }
      double paid = energy.extractAEPower(cost, Actionable.MODULATE, PowerMultiplier.CONFIG);
      progress += workTicks * Math.clamp(paid / cost, 0, 1);
      status = "culturing";
      setChanged();
    }
    if (progress + 1e-7 < Balance.ME_BIO_TICKS.get()) return;
    finishing = true;
    try {
      for (int slot = 0; slot < 3; slot++) items.extractItem(slot, 1, false);
      work = List.of();
      progress = 0;
      items.setStackInSlot(3, output.copyWithCount(existing.getCount() + 1));
    } finally {
      finishing = false;
    }
    status = "idle";
    setChanged();
    deliverOutput();
  }

  @Override
  public boolean acceptsPlans() {
    return getMainNode().isActive()
        && java.util.stream.IntStream.range(0, 4).allMatch(i -> items.getStackInSlot(i).isEmpty());
  }

  @Override
  public boolean pushPattern(
      appeng.api.crafting.IPatternDetails pattern, KeyCounter[] inputs, Direction side) {
    if (!acceptsPlans()
        || pattern == null
        || pattern.getOutputs().size() != 1)
      return false;
    var arranged = new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    for (var counter : inputs)
      for (var entry : counter) {
        if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue() != 1) return false;
        var stack = key.toStack(1);
        int slot =
            stack.is(Agristics.SAMPLER.get()) ? 0 : stack.is(Agristics.NUTRIENT.get()) ? 1 : 2;
        if (!arranged[slot].isEmpty() || !valid(slot, stack)) return false;
        arranged[slot] = stack;
      }
    if (Arrays.stream(arranged).anyMatch(ItemStack::isEmpty)) return false;
    var requested = pattern.getPrimaryOutput();
    var actual = BioData.withIdentity(Agristics.BIO_SEED.get(), BioData.identity(arranged[0]));
    if (!(requested.what() instanceof AEItemKey key)
        || requested.amount() != 1
        || !key.matches(actual)) return false;
    for (int slot = 0; slot < 3; slot++) items.setStackInSlot(slot, arranged[slot]);
    craftingDelivery = true;
    setChanged();
    return true;
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putDouble("bioProgress", progress);
    var saved = new ListTag();
    for (var stack : work) saved.add(stack.saveOptional(registries));
    tag.put("bioWork", saved);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    double value = tag.getDouble("bioProgress");
    progress = Double.isFinite(value) ? Math.clamp(value, 0, Balance.ME_BIO_TICKS.get()) : 0;
    var loaded = new ArrayList<ItemStack>();
    for (var raw : tag.getList("bioWork", Tag.TAG_COMPOUND))
      loaded.add(ItemStack.parseOptional(registries, (CompoundTag) raw));
    work = List.copyOf(loaded);
  }

  @Override
  public CompoundTag snapshot(int page, String search) {
    var tag = super.snapshot(page, search);
    tag.putInt("progress", (int) progress);
    tag.putInt("duration", Balance.ME_BIO_TICKS.get());
    tag.putDouble("power", Balance.ME_BIO_POWER.get() * speed());
    return tag;
  }
}
