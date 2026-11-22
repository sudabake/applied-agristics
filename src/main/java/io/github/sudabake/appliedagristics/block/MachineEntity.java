package io.github.sudabake.appliedagristics.block;

import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public abstract class MachineEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler items;
    private final int inventorySize;
    private final java.util.Map<Direction, LazyOptional<IItemHandler>> itemPorts = new java.util.HashMap<>();
    private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidPort = LazyOptional.empty();
    public String status = "idle";
    private boolean workingVisual;
    public boolean workingVisual() { return workingVisual; }
    public void updateWorkVisual() {
        boolean next=status.equals("growing") || status.equals("culturing");
        if (workingVisual==next) return;
        workingVisual=next;
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    @Override public CompoundTag getUpdateTag() { var tag=new CompoundTag(); tag.putBoolean("working",workingVisual); return tag; }
    @Override public void handleUpdateTag(CompoundTag tag) { workingVisual=tag.getBoolean("working"); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        var tag = packet.getTag();
        handleUpdateTag(tag == null ? new CompoundTag() : tag);
    }
    protected MachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state); inventorySize = slots;
        items = new ItemStackHandler(slots) {
            @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) { return valid(slot, stack); }
            @Override public int getSlotLimit(int slot) { return MachineEntity.this.slotLimit(slot); }
            @Override protected void onContentsChanged(int slot) { MachineEntity.this.inventoryChanged(slot); setChanged(); }
            @Override public net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate) { return super.extractItem(slot,MachineEntity.this.extractable(slot,amount),simulate); }
        };
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemPorts.computeIfAbsent(side, direction -> {
                var handler = automation(direction);
                return handler == null ? LazyOptional.empty() : LazyOptional.of(() -> handler);
            }).cast();
        }
        if (!isRemoved() && capability == ForgeCapabilities.FLUID_HANDLER && this instanceof EarlyPlanter early) {
            if (!fluidPort.isPresent()) fluidPort = LazyOptional.of(() -> early.water);
            return fluidPort.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemPorts.values().forEach(LazyOptional::invalidate);
        itemPorts.clear();
        fluidPort.invalidate();
        fluidPort = LazyOptional.empty();
    }

    public IItemHandler automation(Direction side) {
        return this instanceof SeedStorage seeds ? seeds.port : null;
    }

    protected boolean valid(int slot, net.minecraft.world.item.ItemStack stack) { return true; }
    protected int slotLimit(int slot) { return 64; }
    protected void inventoryChanged(int slot) {}
    protected int extractable(int slot,int requested) { return requested; }
    public java.util.List<net.minecraft.world.item.ItemStack> breakContents() {
        var drops=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for (int i=0;i<items.getSlots();i++) if (!items.getStackInSlot(i).isEmpty()) drops.add(items.getStackInSlot(i).copy());
        return drops;
    }
    public void dropContents() {
        for(var drop:breakContents()) net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,drop);
    }
    public abstract void step();
    public boolean networkMachine() { return false; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    public void open(net.minecraft.server.level.ServerPlayer player) {
        long token=java.util.concurrent.ThreadLocalRandom.current().nextLong();
        var provider=new net.minecraft.world.SimpleMenuProvider(
            (id, inventory, owner) -> new MachineMenu(id, inventory, this, token), getDisplayName());
        net.minecraftforge.network.NetworkHooks.openScreen(player, provider, buffer -> {
            buffer.writeBlockPos(worldPosition);
            buffer.writeVarInt(((MachineBlock) getBlockState().getBlock()).kind().menuId);
            buffer.writeLong(token);
        });
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MachineMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("inventory", items.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); var inventory = tag.getCompound("inventory").copy(); inventory.putInt("Size", inventorySize); items.deserializeNBT(inventory);
    }
    public CompoundTag snapshot(int page, String search) {
        var tag = new CompoundTag(); tag.putString("status", status); return tag;
    }
}
