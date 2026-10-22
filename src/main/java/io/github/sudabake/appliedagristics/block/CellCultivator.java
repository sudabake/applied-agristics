package io.github.sudabake.appliedagristics.block;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

/** One active batch. Inputs are held by the persisted job and are never reusable parents. */
public final class CellCultivator extends MachineEntity {
    public static final int CAPACITY = 64000;
    public final EnergyStorage energy = new EnergyStorage(CAPACITY, 2000, 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate); if (!simulate && received > 0) setChanged(); return received;
        }
    };
    public final FluidTank water = new FluidTank(16000, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private ItemStack work = ItemStack.EMPTY;
    private int progress;
    private boolean extracting;
    private boolean autoExtract;
    public CellCultivator(BlockPos pos, BlockState state) { super(Agristics.CULTIVATOR_BE.get(), pos, state, 6); }
    @Override protected boolean valid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(Agristics.SAMPLER.get()) && BioData.identified(stack);
            case 1 -> stack.is(Agristics.NUTRIENT.get());
            case 2 -> stack.is(Agristics.CAPSULE.get());
            case 3 -> stack.is(Agristics.CULTURE.get()) && BioData.identified(stack);
            default -> false;
        };
    }
    public void toggleAutoExtract() { autoExtract = !autoExtract; setChanged(); }
    public boolean autoExtract() { return autoExtract; }
    @Override public java.util.List<ItemStack> breakContents() {
        var drops=super.breakContents();
        if (!work.isEmpty()) {
            drops.add(work.copy());
            if (!extracting) { drops.add(new ItemStack(Agristics.NUTRIENT.get())); drops.add(new ItemStack(Agristics.CAPSULE.get())); }
        }
        return drops;
    }
    public IItemHandler automation(Direction side) {
        return new IItemHandler() {
            @Override public int getSlots() { return 6; }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return side != Direction.DOWN && valid(slot, stack); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                boolean output = slot >= 4 || slot == 3 && !autoExtract;
                return (side == null || side == Direction.DOWN) && output ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
        };
    }
    private int duration() { return extracting ? Balance.EXTRACT_TICKS.get() : Balance.CULTURE_TICKS.get(); }
    private int cost() { return extracting ? Balance.EXTRACT_POWER.get() : Balance.CULTURE_POWER.get(); }
    private ItemStack product() { return BioData.withIdentity(extracting ? Agristics.EXTRACT.get() : Agristics.CULTURE.get(), BioData.identity(work)); }
    private boolean fits(int slot, ItemStack stack) {
        var existing = items.getStackInSlot(slot);
        return (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)) && existing.getCount() + stack.getCount() <= stack.getMaxStackSize();
    }
    private void put(int slot, ItemStack stack) { items.setStackInSlot(slot, stack.copyWithCount(items.getStackInSlot(slot).getCount() + stack.getCount())); }
    private boolean room() { return fits(extracting ? 4 : 3, product()) && (!extracting || fits(5, new ItemStack(Agristics.CAPSULE.get()))); }
    @Override public void step() {
        if (work.isEmpty()) {
            extracting = autoExtract && valid(3, items.getStackInSlot(3));
            if (extracting) {
                if (energy.getEnergyStored() < cost()) { status = "no_power"; return; }
                work = items.extractItem(3, 1, false);
            } else {
                if (!valid(0, items.getStackInSlot(0)) || !valid(1, items.getStackInSlot(1)) || !valid(2, items.getStackInSlot(2))) { status = "missing_inputs"; return; }
                if (water.getFluidAmount() < Balance.CULTURE_WATER.get()) { status = "no_water"; return; }
                if (energy.getEnergyStored() < cost()) { status = "no_power"; return; }
                work = items.extractItem(0, 1, false);
                items.extractItem(1, 1, false); items.extractItem(2, 1, false);
                water.drain(Balance.CULTURE_WATER.get(), FluidAction.EXECUTE);
            }
            progress = 0; setChanged();
        }
        if (!BioData.identified(work)) { status = "no_recipe"; return; }
        if (!room()) { status = "output_full"; setChanged(); return; }
        int duration = duration();
        if (progress < duration) {
            if (energy.getEnergyStored() < cost()) { status = "no_power"; return; }
            // Storage exposes receive-only FE to pipes; internal expenditure uses its saved value.
            energy.deserializeNBT(level.registryAccess(), net.minecraft.nbt.IntTag.valueOf(energy.getEnergyStored() - cost()));
            progress++; status = extracting ? "extracting" : "culturing"; setChanged();
        }
        if (progress >= duration) {
            put(extracting ? 4 : 3, product());
            if (extracting) put(5, new ItemStack(Agristics.CAPSULE.get()));
            work = ItemStack.EMPTY; progress = 0; status = "idle"; setChanged();
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy.getEnergyStored()); tag.putInt("progress", progress);
        tag.putBoolean("extracting", extracting); tag.putBoolean("autoExtract", autoExtract);
        if (!work.isEmpty()) tag.put("work", work.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); water.readFromNBT(registries, tag.getCompound("water"));
        energy.deserializeNBT(registries, net.minecraft.nbt.IntTag.valueOf(Math.clamp(tag.getInt("energy"), 0, CAPACITY)));
        work = ItemStack.parseOptional(registries, tag.getCompound("work")); extracting = tag.getBoolean("extracting"); autoExtract = tag.getBoolean("autoExtract");
        progress = work.isEmpty() ? 0 : Math.clamp(tag.getInt("progress"), 0, duration());
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, search); tag.putInt("energy", energy.getEnergyStored()); tag.putInt("water", water.getFluidAmount());
        tag.putInt("progress", progress); tag.putInt("duration", duration()); tag.putInt("cost", work.isEmpty() ? 0 : cost()); tag.putBoolean("autoExtract", autoExtract);
        if (!work.isEmpty()) tag.put("work", work.save(level.registryAccess())); return tag;
    }
}
