package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Native AE processing IO, acceleration slots and result delivery shared by the two processors. */
public abstract class AEProcessingMachine extends GridMachine
    implements AltarControls, appeng.api.implementations.blockentities.ICraftingMachine {
  protected final int outputSlot;
  private final int firstUpgrade;
  private final int[] sides = {1, 1, 1, 1, 1, 1};
  protected boolean autoOutput;
  protected boolean craftingDelivery;

  protected AEProcessingMachine(
      BlockEntityType<?> type,
      BlockPos pos,
      BlockState state,
      int slots,
      int outputSlot,
      int firstUpgrade) {
    super(type, pos, state, slots);
    this.outputSlot = outputSlot;
    this.firstUpgrade = firstUpgrade;
  }

  @Override
  protected int slotLimit(int slot) {
    return slot >= firstUpgrade ? 1 : 64;
  }

  protected final boolean isAcceleration(ItemStack stack) {
    return stack.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem());
  }

  @Override
  public int accelerationCards() {
    int count = 0;
    for (int slot = firstUpgrade; slot < firstUpgrade + 4; slot++)
      if (isAcceleration(items.getStackInSlot(slot))) count++;
    return count;
  }

  public final boolean insertAcceleration(ItemStack held, boolean simulate) {
    if (!isAcceleration(held)) return false;
    for (int slot = firstUpgrade; slot < firstUpgrade + 4; slot++)
      if (items.getStackInSlot(slot).isEmpty()) {
        if (!simulate) items.setStackInSlot(slot, held.copyWithCount(1));
        return true;
      }
    return false;
  }

  public final int speed() {
    return 1 << accelerationCards();
  }

  @Override
  public final void configureSide(int side, int mode) {
    if (side >= 0 && side < 6 && mode >= 0 && mode <= 3) {
      // All faces accept input. Bit 2 enables output on the selected face.
      sides[side] = (mode & 2) == 0 ? 1 : 3;
      setChanged();
      invalidateCapabilities();
    }
  }

  @Override
  public final int sideMode(int side) {
    return sides[side];
  }

  @Override
  public final void toggleAutoOutput() {
    autoOutput = !autoOutput;
    setChanged();
  }

  protected final void deliverOutput() {
    if (!(autoOutput || craftingDelivery) || items.getStackInSlot(outputSlot).isEmpty()) return;
    exportOutput();
    if (craftingDelivery && items.getStackInSlot(outputSlot).isEmpty()) {
      craftingDelivery = false;
      setChanged();
    }
  }

  @Override
  public appeng.api.implementations.blockentities.PatternContainerGroup getCraftingMachineInfo() {
    return new appeng.api.implementations.blockentities.PatternContainerGroup(
        AEItemKey.of(new ItemStack(getBlockState().getBlock())), getDisplayName(), List.of());
  }

  public IItemHandler automation(Direction side) {
    return new IItemHandler() {
      private int mode() {
        return side == null ? 3 : sides[side.ordinal()];
      }

      @Override
      public int getSlots() {
        return outputSlot + 1;
      }

      @Override
      public ItemStack getStackInSlot(int slot) {
        return items.getStackInSlot(slot);
      }

      @Override
      public int getSlotLimit(int slot) {
        return items.getSlotLimit(slot);
      }

      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0
            && slot < outputSlot
            && items.isItemValid(slot, stack)
            && items.getStackInSlot(outputSlot).isEmpty();
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == outputSlot && (mode() == 2 || mode() == 3)
            ? items.extractItem(slot, amount, simulate)
            : ItemStack.EMPTY;
      }
    };
  }

  protected final void exportOutput() {
    var output = items.getStackInSlot(outputSlot);
    if (output.isEmpty()) return;
    var grid = getMainNode().getGrid();
    if (grid != null && getMainNode().isActive()) {
      long accepted =
          grid.getStorageService()
              .getInventory()
              .insert(
                  AEItemKey.of(output),
                  output.getCount(),
                  Actionable.MODULATE,
                  IActionSource.ofMachine(this));
      items.extractItem(outputSlot, (int) accepted, false);
    }
    for (var side : Direction.values()) {
      if (sides[side.ordinal()] != 2 && sides[side.ordinal()] != 3) continue;
      var pos = worldPosition.relative(side);
      if (!level.hasChunkAt(pos)) continue;
      var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side.getOpposite());
      if (target == null) continue;
      for (int slot = 0; slot < target.getSlots(); slot++) {
        var stack = items.getStackInSlot(outputSlot);
        if (stack.isEmpty()) return;
        var rest = target.insertItem(slot, stack.copy(), false);
        int accepted = stack.getCount() - rest.getCount();
        if (accepted > 0) items.extractItem(outputSlot, accepted, false);
      }
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putBoolean("autoOutput", autoOutput);
    tag.putBoolean("craftingDelivery", craftingDelivery);
    tag.putIntArray("sides", sides);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    autoOutput = tag.getBoolean("autoOutput");
    craftingDelivery = tag.getBoolean("craftingDelivery");
    var modes = tag.getIntArray("sides");
    if (modes.length == 6)
      for (int side = 0; side < 6; side++) sides[side] = (Math.clamp(modes[side], 0, 3) & 2) == 0 ? 1 : 3;
  }

  @Override
  public CompoundTag snapshot(int page, String search) {
    var tag = super.snapshot(page, search);
    tag.putBoolean("autoOutput", autoOutput);
    tag.putIntArray("sides", sides);
    tag.putInt("acceleration", accelerationCards());
    return tag;
  }
}
