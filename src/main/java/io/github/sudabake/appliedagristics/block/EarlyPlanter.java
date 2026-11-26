package io.github.sudabake.appliedagristics.block;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.GrowthClock;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;

public final class EarlyPlanter extends MachineEntity {
    private long lastHarvestTick = Long.MIN_VALUE;

    public boolean harvestFlash() {
        return level!=null && lastHarvestTick!=Long.MIN_VALUE && level.getGameTime()-lastHarvestTick<6;
    }

    private java.util.List<ItemStack> pending;
    private final GrowthClock clock = new GrowthClock();
    public final FluidTank water = new FluidTank(16000, stack -> stack.getFluid() == Fluids.WATER) {
    @Override protected void onContentsChanged() { setChanged(); }
    };
    public EarlyPlanter(BlockPos pos, BlockState state) { super(Agristics.EARLY_BE.get(), pos, state, 11); }
    @Override protected boolean valid(int slot, ItemStack stack) { return slot == 0 ? io.github.sudabake.appliedagristics.logic.Harvests.seed(stack,level) : slot == 1 && (stack.getItem() instanceof net.minecraft.world.item.BlockItem || io.github.sudabake.appliedagristics.logic.CropSources.soil(level,items.getStackInSlot(0),stack)); }
    @Override protected void inventoryChanged(int slot) {
        if (slot < 2 && clock != null && pending == null) { clock.reset(); lastHarvestTick=Long.MIN_VALUE; }
        if (slot<2 && level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0,1,0); }
    @Override public CompoundTag getUpdateTag() {
        var tag=new CompoundTag(); var seed=items.getStackInSlot(0);
        if (!seed.isEmpty()) tag.put("displaySeed",seed.copyWithCount(1).save(new CompoundTag()));
        var soil=items.getStackInSlot(1); if (!soil.isEmpty()) tag.put("displaySoil",soil.copyWithCount(1).save(new CompoundTag())); return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) { items.setStackInSlot(0,ItemStack.of(tag.getCompound("displaySeed"))); items.setStackInSlot(1,ItemStack.of(tag.getCompound("displaySoil"))); }
    @Override public java.util.List<ItemStack> breakContents() { var drops=super.breakContents(); if (pending!=null) pending.forEach(s->drops.add(s.copy())); return drops; }
    public IItemHandler automation(Direction side) {
        return new RangedWrapper(items, side == Direction.DOWN ? 2 : 0, side == Direction.DOWN ? 11 : 2);
    }
    @Override public void step() {
        var seed = items.getStackInSlot(0); var soil = items.getStackInSlot(1);
        if (!io.github.sudabake.appliedagristics.logic.Harvests.seed(seed,level) || soil.isEmpty()) { status = "missing_inputs"; clock.reset(); return; }
        if (!io.github.sudabake.appliedagristics.logic.CropSources.soil(level,seed,soil)) { status="wrong_farmland"; clock.reset(); return; }
        var server = (net.minecraft.server.level.ServerLevel) level;
        if (!io.github.sudabake.appliedagristics.logic.Harvests.canGrow(server, worldPosition, seed, soil)) { status = "wrong_farmland"; return; }
        boolean wet = water.getFluidAmount() >= Balance.WATER_COST.get();
        int dryTicks=io.github.sudabake.appliedagristics.logic.CropSources.ticks(level,worldPosition,seed,soil,Balance.EARLY_TICKS.get());
        int wetTicks=io.github.sudabake.appliedagristics.logic.CropSources.find(level,seed)==null ? Balance.WATER_TICKS.get() : Math.max(1,dryTicks/2);
        clock.advance(20, wet);
        boolean boosted = clock.wetReady(wetTicks, wet) && !clock.dryReady(dryTicks);
        status = wet ? "watered" : "growing";
        if (boosted || clock.dryReady(dryTicks)) {
            if (pending == null) pending = io.github.sudabake.appliedagristics.logic.Harvests.harvest(server, worldPosition, seed, soil);
            var trial = new net.minecraftforge.items.ItemStackHandler(9);
            for (int i = 0; i < 9; i++) trial.setStackInSlot(i, items.getStackInSlot(i + 2).copy());
            for (var drop : pending) {
                var rest = drop.copy();
                for (int i = 0; i < 9 && !rest.isEmpty(); i++) rest = trial.insertItem(i, rest, false);
                if (!rest.isEmpty()) { status = "output_full"; setChanged(); return; }
            }
            for (int i = 0; i < 9; i++) items.setStackInSlot(i + 2, trial.getStackInSlot(i));
            if (boosted) water.drain(Balance.WATER_COST.get(), FluidAction.EXECUTE);
            pending = null; clock.reset(); lastHarvestTick=level.getGameTime();
        }
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (pending != null) { var list = new net.minecraft.nbt.ListTag(); for (var stack : pending) list.add(stack.save(new CompoundTag())); tag.put("pending", list); } tag.put("water", water.writeToNBT(new CompoundTag())); tag.putInt("dry", clock.dry()); tag.putInt("wet", clock.wet());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); water.readFromNBT(tag.getCompound("water")); clock.restore(tag.getInt("dry"), tag.getInt("wet"));
        pending = tag.contains("pending") ? new java.util.ArrayList<>() : null; if (pending != null) for (var value : tag.getList("pending", 10)) pending.add(ItemStack.of( (CompoundTag)value));
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, search); tag.putInt("water", water.getFluidAmount());
        var seed=items.getStackInSlot(0);
        int dryTicks=io.github.sudabake.appliedagristics.logic.CropSources.ticks(level,worldPosition,seed,items.getStackInSlot(1),Balance.EARLY_TICKS.get());
        int wetTicks=io.github.sudabake.appliedagristics.logic.CropSources.find(level,seed)==null ? Balance.WATER_TICKS.get() : Math.max(1,dryTicks/2);
        long progress = (long) clock.dry() * 1000 / dryTicks;
        if (water.getFluidAmount() >= Balance.WATER_COST.get()) progress = Math.max(progress, (long) clock.wet() * 1000 / wetTicks);
        tag.putInt("baseTicks", dryTicks); tag.putInt("waterTicks", wetTicks);
        var crop = Crops.seed(items.getStackInSlot(0));
        if (crop != null && crop.getCruxBlock() != null) tag.putString("crux", crop.getCruxBlock().getName().getString());
        if (crop != null && items.getStackInSlot(1).getItem() instanceof net.minecraft.world.item.BlockItem ground) {
            tag.putBoolean("mystical", true); tag.putDouble("bonusChance", crop.getSecondaryChance(ground.getBlock()) * 100);
            tag.putDouble("seedChance", io.github.sudabake.appliedagristics.logic.NativeSeedYield.reproduces((net.minecraft.server.level.ServerLevel)level,items.getStackInSlot(0),items.getStackInSlot(1)) ? crop.getSecondaryChance(ground.getBlock()) * 100 : 10);
            tag.putDouble("fertilizerChance", crop == com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM ? 0 : com.blakebr0.mysticalagriculture.config.ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE.get() * 100);
        }
        boolean completed=harvestFlash();
        tag.putBoolean("harvestFlash",completed);
        tag.putInt("progress", completed || pending!=null ? 1000 : (int) Math.min(1000, progress));
        tag.putInt("duration", 1000); return tag;
    }
}
