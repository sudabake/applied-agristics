package io.github.sudabake.appliedagristics.menu;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MachineMenu extends appeng.menu.AEBaseMenu
    implements appeng.menu.interfaces.IProgressProvider {
  public final MachineEntity machine;
  private final Player player;
  private final int machineSlots;
  public CompoundTag state = new CompoundTag();
  public int page;
  public String search = "";

  @Override
  public int getCurrentProgress() {
    return state.getInt("progress");
  }

  @Override
  public int getMaxProgress() {
    return Math.max(1, state.getInt("duration"));
  }

  private long lastSync = -1;

  public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    this(id, inventory, clientMachine(inventory, buffer));
  }

  private static MachineEntity clientMachine(Inventory inventory, RegistryFriendlyByteBuf buffer) {
    var pos = buffer.readBlockPos();
    int kind = buffer.readVarInt();
    var block =
        switch (kind) {
          case 0 -> Agristics.EARLY.get();
          case 1 -> Agristics.SEEDS.get();
          case 2 -> Agristics.NETWORK.get();
          case 4 -> Agristics.CULTIVATOR.get();
          case 6 -> Agristics.ME_INFUSION.get();
          case 7 -> Agristics.INPUT_PORT.get();
          case 8 -> Agristics.OUTPUT_PORT.get();
          case 9 -> Agristics.ESSENCE_VAULT.get();
          default -> throw new IllegalArgumentException("Unknown machine kind");
        };
    // Container slots synchronize separately; the chunk's block entity may not exist yet on this
    // client.
    var machine = (MachineEntity) block.newBlockEntity(pos, block.defaultBlockState());
    machine.setLevel(inventory.player.level());
    return machine;
  }

  public MachineMenu(int id, Inventory inventory, MachineEntity machine) {
    super(Agristics.MENU.get(), id, inventory, machine);
    this.machine = machine;
    this.player = inventory.player;
    if (machine instanceof NetworkPlanter
        && inventory.player.getPersistentData().getBoolean("appliedagristics.hideOrdinary"))
      search = "hide_ordinary";
    var layout = MachineMenuLayout.forMachine(machine);
    for (var slot : layout.slots()) addMachineSlot(slot.index(), slot.x(), slot.y(), slot.output());
    machineSlots = slots.size();
    int inventoryX = layout.inventoryX(), inventoryY = layout.inventoryY();
    for (int row = 0; row < 3; row++)
      for (int column = 0; column < 9; column++)
        addSlot(
            new Slot(
                inventory, column + row * 9 + 9, inventoryX + column * 18, inventoryY + row * 18),
            appeng.menu.SlotSemantics.PLAYER_INVENTORY);
    for (int column = 0; column < 9; column++)
      addSlot(
          new Slot(inventory, column, inventoryX + column * 18, inventoryY + 58),
          appeng.menu.SlotSemantics.PLAYER_HOTBAR);
  }

  private void addMachineSlot(int index, int x, int y, boolean output) {
    if (machine instanceof AltarControls) {
      boolean upgrade = index >= 2;
      var inventory =
          new appeng.api.inventories.InternalInventory() {
            @Override
            public int size() {
              return machine.items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
              return machine.items.getStackInSlot(slot);
            }

            @Override
            public void setItemDirect(int slot, ItemStack stack) {
              machine.items.setStackInSlot(slot, stack);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
              return machine.items.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
              return machine.items.getSlotLimit(slot);
            }

            @Override
            public void sendChangeNotification(int slot) {
              machine.items.setStackInSlot(slot, machine.items.getStackInSlot(slot));
            }
          };
      var slot =
          new appeng.menu.slot.AppEngSlot(inventory, index) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return !output && super.mayPlace(stack);
            }
          };
      if (upgrade) slot.setIcon(appeng.client.gui.Icon.BACKGROUND_UPGRADE);
      slot.setEmptyTooltip(
          () ->
              java.util.List.of(
                  net.minecraft.network.chat.Component.translatable(
                      upgrade
                          ? "gui.appliedagristics.altar_acceleration"
                          : output
                              ? "gui.appliedagristics.output"
                              : "gui.appliedagristics.altar_inputs")));
      addSlot(
          slot,
          upgrade
              ? appeng.menu.SlotSemantics.UPGRADE
              : output
                  ? appeng.menu.SlotSemantics.MACHINE_OUTPUT
                  : appeng.menu.SlotSemantics.MACHINE_INPUT);
      return;
    }
    addSlot(
        new SlotItemHandler(machine.items, index, x, y) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return !output && super.mayPlace(stack);
          }

          @Override
          public boolean isActive() {
            return !(machine instanceof CellCultivator && index >= 4) || hasItem();
          }

          @Override
          public boolean mayPickup(Player player) {
            return machine instanceof ItemPort port && port.isInput()
                ? hasItem()
                : super.mayPickup(player);
          }

          @Override
          public ItemStack remove(int amount) {
            if (amount > 0 && !player.level().isClientSide && machine instanceof ItemPort port)
              port.releaseForManualExtraction(getItem());
            return super.remove(amount);
          }

          @Override
          public void set(ItemStack stack) {
            var previous = getItem();
            if (!player.level().isClientSide
                && machine instanceof ItemPort port
                && !previous.isEmpty()
                && (!ItemStack.isSameItemSameComponents(previous, stack)
                    || stack.getCount() < previous.getCount()))
              port.releaseForManualExtraction(previous);
            super.set(stack);
          }
        });
  }

  @Override
  public boolean stillValid(Player player) {
    return !machine.isRemoved()
        && player.level().getBlockEntity(machine.getBlockPos()) == machine
        && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64;
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    if (player instanceof ServerPlayer server && server.level().getGameTime() - lastSync >= 20)
      sync(server);
  }

  public void sync(ServerPlayer server) {
    lastSync = server.level().getGameTime();
    state = machine.snapshot(page, search);
    PacketDistributor.sendToPlayer(server, new Messages.Snapshot(machine.getBlockPos(), state));
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    var slot = slots.get(index);
    if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var original = stack.copy();
    if (index < machineSlots) {
      if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
    } else if (!moveToMachineInputs(stack)) return ItemStack.EMPTY;
    if (index < machineSlots && !player.level().isClientSide && machine instanceof ItemPort port)
      port.releaseForManualExtraction(original);
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, stack);
    return original;
  }

  private boolean moveToMachineInputs(ItemStack stack) {
    boolean moved = false;
    int start = 0, end = machineSlots;
    if (machine instanceof EarlyPlanter) {
      // Seeds are BlockItems too; a full seed slot must not redirect them to soil.
      if (io.github.sudabake.appliedagristics.logic.Harvests.seed(stack, machine.getLevel()))
        end = 1;
      else {
        start = 1;
        end = 2;
      }
    }
    // Vanilla first merges matching stacks without checking mayPlace; exclude
    // output slots before calling it, even when they contain the same seed.
    for (boolean empty : new boolean[] {false, true})
      for (int i = start; i < end && !stack.isEmpty(); i++) {
        var target = slots.get(i);
        if (target.getItem().isEmpty() != empty || !target.isActive() || !target.mayPlace(stack))
          continue;
        moved = moveItemStackTo(stack, i, i + 1, false) || moved;
      }
    return moved;
  }
}
