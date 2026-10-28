package io.github.sudabake.appliedagristics.block;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

/** One active batch. Inputs are held by the persisted job and are never reusable parents. */
public final class CellCultivator extends MachineEntity {
    public final FluidTank water = new FluidTank(16000, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private ItemStack work = ItemStack.EMPTY;
    private int progress;
    private boolean extracting;
    private boolean legacyPaid;
    private boolean finishing;

    public CellCultivator(BlockPos pos, BlockState state) { super(Agristics.CULTIVATOR_BE.get(), pos, state, 6); }
    @Override protected boolean valid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(Agristics.SAMPLER.get()) && BioData.identified(stack);
            case 1 -> stack.is(Agristics.NUTRIENT.get());
            case 2 -> stack.is(Agristics.CAPSULE.get());

            default -> false;
        };
    }
    /** Legacy control packets are ignored; only already paid extraction jobs may finish. */
    public void toggleAutoExtract() {}
    public boolean autoExtract() { return false; }
    @Override public java.util.List<ItemStack> breakContents() {
        var drops=super.breakContents();
        if (legacyPaid && !work.isEmpty()) {
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
                boolean output = slot >= 4 || slot == 3;
                return (side == null || side == Direction.DOWN) && output ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
        };
    }
    @Override protected void inventoryChanged(int slot) {
        if(slot<3 && !legacyPaid && !finishing && !work.isEmpty() && !matchesInputs()) { work=ItemStack.EMPTY;progress=0;extracting=false; }
    }
    private boolean matchesInputs() { return valid(0,items.getStackInSlot(0)) && ItemStack.isSameItemSameComponents(work,items.getStackInSlot(0)) && valid(1,items.getStackInSlot(1)) && valid(2,items.getStackInSlot(2)); }
    private int duration() { return extracting ? Balance.EXTRACT_TICKS.get() : Balance.CULTURE_TICKS.get(); }
    private ItemStack product() { return BioData.withIdentity(extracting ? Agristics.EXTRACT.get() : Agristics.CULTURE.get(), BioData.identity(work)); }
    private boolean fits(int slot, ItemStack stack) {
        var existing = items.getStackInSlot(slot);
        return (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)) && existing.getCount() + stack.getCount() <= stack.getMaxStackSize();
    }
    private void put(int slot, ItemStack stack) { items.setStackInSlot(slot, stack.copyWithCount(items.getStackInSlot(slot).getCount() + stack.getCount())); }
    private boolean room() { return fits(extracting ? 4 : 3, product()) && (!extracting || fits(5, new ItemStack(Agristics.CAPSULE.get()))); }
    @Override public void step() {
        if (work.isEmpty()) {
            extracting = false;
            if (!valid(0, items.getStackInSlot(0)) || !valid(1, items.getStackInSlot(1)) || !valid(2, items.getStackInSlot(2))) { status = "missing_inputs"; return; }
            if (water.getFluidAmount() < Balance.CULTURE_WATER.get()) { status = "no_water"; return; }
            work = items.getStackInSlot(0).copyWithCount(1);legacyPaid=false;
            progress = 0; setChanged();
        }
        if(!legacyPaid && !matchesInputs()) {work=ItemStack.EMPTY;progress=0;status="missing_inputs";setChanged();return;}
        if(!legacyPaid && water.getFluidAmount()<Balance.CULTURE_WATER.get()) {status="no_water";return;}
        if (!BioData.identified(work)) { status = "no_recipe"; return; }
        if (!room()) { status = "output_full"; setChanged(); return; }
        int duration = duration();
        if (progress < duration) {
            progress++; status = extracting ? "extracting" : "culturing"; setChanged();
        }
        if (progress >= duration) {
            finishing=true;
            if(!legacyPaid) {
                items.extractItem(0,1,false);items.extractItem(1,1,false);items.extractItem(2,1,false);
                water.drain(Balance.CULTURE_WATER.get(),FluidAction.EXECUTE);
            }
            put(extracting ? 4 : 3, product());
            if (extracting) put(5, new ItemStack(Agristics.CAPSULE.get()));
            work = ItemStack.EMPTY; progress = 0; legacyPaid=false;finishing=false;status = "idle"; setChanged();
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putBoolean("extracting", extracting);tag.putBoolean("deferredInputs",!legacyPaid);
        if (!work.isEmpty()) tag.put("work", work.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); water.readFromNBT(registries, tag.getCompound("water"));
        work = ItemStack.parseOptional(registries, tag.getCompound("work")); extracting = tag.getBoolean("extracting");legacyPaid=!work.isEmpty() && !tag.getBoolean("deferredInputs");
        progress = work.isEmpty() ? 0 : Math.clamp(tag.getInt("progress"), 0, duration());
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, search); tag.putInt("water", water.getFluidAmount());
        tag.putInt("progress", progress); tag.putInt("duration", duration()); tag.putInt("cost", 0);
        if (!work.isEmpty()) tag.put("work", work.save(level.registryAccess())); return tag;
    }
}
