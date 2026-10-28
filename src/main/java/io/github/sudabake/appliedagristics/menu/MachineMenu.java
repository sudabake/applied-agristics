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

public final class MachineMenu extends appeng.menu.AEBaseMenu implements appeng.menu.interfaces.IProgressProvider {
    public final MachineEntity machine;
    private final Player player;
    private final int machineSlots;
    public CompoundTag state = new CompoundTag();
    public int page;
    public String search = "";
    @Override public int getCurrentProgress() { return state.getInt("progress"); }
    @Override public int getMaxProgress() { return Math.max(1, state.getInt("duration")); }
    private long lastSync = -1;
    public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientMachine(inventory, buffer));
    }
    private static MachineEntity clientMachine(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos(); int kind = buffer.readVarInt();
        var block = switch (kind) { case 0 -> Agristics.EARLY.get(); case 1 -> Agristics.SEEDS.get(); case 2 -> Agristics.NETWORK.get(); case 4 -> Agristics.CULTIVATOR.get(); case 6 -> Agristics.ME_INFUSION.get(); case 7 -> Agristics.INPUT_PORT.get(); case 8 -> Agristics.OUTPUT_PORT.get(); case 9 -> Agristics.ESSENCE_VAULT.get(); default -> throw new IllegalArgumentException("Unknown machine kind"); };
        // Container slots synchronize separately; the chunk's block entity may not exist yet on this client.
        var machine = (MachineEntity) block.newBlockEntity(pos, block.defaultBlockState()); machine.setLevel(inventory.player.level()); return machine;
    }
    public MachineMenu(int id, Inventory inventory, MachineEntity machine) {
        super(Agristics.MENU.get(), id, inventory, machine); this.machine = machine; this.player = inventory.player;
        if(machine instanceof NetworkPlanter && inventory.player.getPersistentData().getBoolean("appliedagristics.hideOrdinary")) search="hide_ordinary";
        if (machine instanceof CellCultivator) {
            addMachineSlot(0, 44, 29, false); addMachineSlot(1, 68, 29, false); addMachineSlot(2, 92, 29, false);
            addMachineSlot(3, 116, 48, true); addMachineSlot(4, 116, 70, true); addMachineSlot(5, 140, 70, true);

        } else if (machine instanceof ItemPort) {
            for (int i=0;i<16;i++) addMachineSlot(i,52+i%4*18,24+i/4*18,false);
        } else if (machine instanceof MEInfusionAltar) {
            addMachineSlot(0, 48, 40, false); addMachineSlot(1, 116, 40, true);
            for (int i=2;i<6;i++) addMachineSlot(i,179,5+(i-2)*18,false);
        } else if (machine instanceof EarlyPlanter) {
            addMachineSlot(0, 26, 40, false); addMachineSlot(1, 62, 40, false);
            for (int i = 2; i < 11; i++) addMachineSlot(i, 108 + (i - 2) % 3 * 18, 34 + (i - 2) / 3 * 18, true);
        } else if(machine instanceof SeedStorage) addMachineSlot(0,24,29,false);
        // ME planter upgrades now belong to physical structure modules, not inventory slots.
        machineSlots = slots.size();
        int inventoryX = machine instanceof CellCultivator || machine instanceof EarlyPlanter || machine instanceof AltarControls || machine instanceof ItemPort || machine instanceof EssenceVault ? 8 : 79;
        int inventoryY = machine instanceof CellCultivator ? 112 : machine instanceof EarlyPlanter ? 136 : machine instanceof MEInfusionAltar ? 100 : machine instanceof ItemPort || machine instanceof EssenceVault ? 116 : 160;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, inventoryX + column * 18, inventoryY + row * 18), appeng.menu.SlotSemantics.PLAYER_INVENTORY);
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, inventoryX + column * 18, inventoryY + 58), appeng.menu.SlotSemantics.PLAYER_HOTBAR);
    }
    private void addMachineSlot(int index, int x, int y, boolean output) {
        if (machine instanceof AltarControls) {
            boolean upgrade=index>=2;
            var inventory = new appeng.api.inventories.InternalInventory() {
                @Override public int size() { return machine.items.getSlots(); }
                @Override public ItemStack getStackInSlot(int slot) { return machine.items.getStackInSlot(slot); }
                @Override public void setItemDirect(int slot, ItemStack stack) { machine.items.setStackInSlot(slot, stack); }
                @Override public boolean isItemValid(int slot, ItemStack stack) { return machine.items.isItemValid(slot, stack); }
                @Override public int getSlotLimit(int slot) { return machine.items.getSlotLimit(slot); }
                @Override public void sendChangeNotification(int slot) { machine.items.setStackInSlot(slot, machine.items.getStackInSlot(slot)); }
            };
            var slot = new appeng.menu.slot.AppEngSlot(inventory, index) {
                @Override public boolean mayPlace(ItemStack stack) { return !output && super.mayPlace(stack); }
            };
            if (upgrade) slot.setIcon(appeng.client.gui.Icon.BACKGROUND_UPGRADE);
            slot.setEmptyTooltip(() -> java.util.List.of(net.minecraft.network.chat.Component.translatable(upgrade ? "gui.appliedagristics.altar_acceleration" : output ? "gui.appliedagristics.output" : "gui.appliedagristics.altar_inputs")));
            addSlot(slot, upgrade ? appeng.menu.SlotSemantics.UPGRADE : output ? appeng.menu.SlotSemantics.MACHINE_OUTPUT : appeng.menu.SlotSemantics.MACHINE_INPUT);
            return;
        }
        addSlot(new SlotItemHandler(machine.items, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return !output && super.mayPlace(stack); }
            @Override public boolean isActive() { return !(machine instanceof CellCultivator && index>=4) || hasItem(); }
            @Override public boolean mayPickup(Player player) {
                if (machine instanceof ItemPort port && port.isInput()) {
                    var locked=state.getIntArray("lockedSlots");
                    if (player.level().isClientSide ? locked.length==16 && locked[index]!=0 : port.slotInUse(index)) return false;
                }
                return super.mayPickup(player);
            }
            @Override public int getMaxStackSize(ItemStack stack) {
                if (machine instanceof NetworkPlanter planter) return index == 0 ? io.github.sudabake.appliedagristics.logic.ProductionMath.cardLimit(io.github.sudabake.appliedagristics.Balance.SPEED_MAX.get()) : index == 1 ? io.github.sudabake.appliedagristics.logic.ProductionMath.cardLimit(io.github.sudabake.appliedagristics.Balance.PARALLEL_MAX.get()) : 1;
                return super.getMaxStackSize(stack);
            }
        });
    }
    @Override public boolean stillValid(Player player) {
        return !machine.isRemoved() && player.level().getBlockEntity(machine.getBlockPos()) == machine && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer server && server.level().getGameTime() - lastSync >= 20) sync(server);
    }
    public void sync(ServerPlayer server) {
        lastSync = server.level().getGameTime(); state = machine.snapshot(page, search);
        PacketDistributor.sendToPlayer(server, new Messages.Snapshot(machine.getBlockPos(), state));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < machineSlots) { if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY; }
        else if (machineSlots == 0 || !moveItemStackTo(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
