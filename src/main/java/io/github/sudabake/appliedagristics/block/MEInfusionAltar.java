package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.blakebr0.mysticalagriculture.init.ModRecipeTypes;
import com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity;
import io.github.sudabake.appliedagristics.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

/** Replacement center for the ordinary MA infusion altar: eight real external pedestals. */
public final class MEInfusionAltar extends GridMachine implements AltarControls,appeng.api.implementations.blockentities.ICraftingMachine {
    // The public MA altar layout: cardinal distance 3; diagonals distance 2.
    private static final int[][] OFFSETS={{3,0},{0,3},{-3,0},{0,-3},{2,2},{2,-2},{-2,2},{-2,-2}};
    private final int[] sides={2,1,1,1,1,1};
    private boolean autoOutput;
    private boolean craftingDelivery;
    private int progress;
    private ResourceLocation recipeId;
    private List<ItemStack> workInput=List.of();
    private com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity visualView;

    public MEInfusionAltar(BlockPos pos,BlockState state) { super(Agristics.INFUSION_BE.get(),pos,state,6); }
    @Override protected boolean valid(int slot,ItemStack stack) { return slot==0 || slot>=2 && stack.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem()); }
    @Override protected int slotLimit(int slot) { return slot==0 || slot>=2 ? 1 : 64; }
    @Override protected void inventoryChanged(int slot) {
        if (slot==0) reset();
        if (slot==1 && items.getStackInSlot(1).isEmpty()) craftingDelivery=false;
        if (slot<2 && level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    public com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity visualView() {
        if (visualView==null) visualView=new com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity(worldPosition,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_ALTAR.get().defaultBlockState());
        visualView.setLevel(level);
        visualView.getInventory().getStacks().set(0,items.getStackInSlot(0));
        visualView.getInventory().getStacks().set(1,items.getStackInSlot(1));
        return visualView;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag=new CompoundTag(); tag.put("center",items.getStackInSlot(0).saveOptional(registries)); tag.put("result",items.getStackInSlot(1).saveOptional(registries)); return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries) { items.setStackInSlot(0,ItemStack.parseOptional(registries,tag.getCompound("center"))); items.setStackInSlot(1,ItemStack.parseOptional(registries,tag.getCompound("result"))); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries) { handleUpdateTag(packet.getTag(),registries); }
    @Override public int accelerationCards() { int count=0; for (int i=2;i<6;i++) if (items.getStackInSlot(i).is(appeng.core.definitions.AEItems.SPEED_CARD.asItem())) count++; return count; }
    public boolean insertAcceleration(ItemStack held, boolean simulate) {
        if (!held.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem())) return false;
        for (int i=2;i<6;i++) if (items.getStackInSlot(i).isEmpty()) {
            if (!simulate) items.setStackInSlot(i,held.copyWithCount(1));
            return true;
        }
        return false;
    }
    public int speed() { return 1 << accelerationCards(); }
    @Override public void configureSide(int side,int mode) { if (side>=0 && side<6 && mode>=0 && mode<=3) { sides[side]=mode; setChanged(); } }
    @Override public int sideMode(int side) { return sides[side]; }
    @Override public void toggleAutoOutput() { autoOutput=!autoOutput; setChanged(); }
    public IItemHandler automation(Direction side) {
        return new IItemHandler() {
            private int mode() { return side==null ? 3 : sides[side.ordinal()]; }
            @Override public int getSlots() { return 2; }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return slot==0 ? 1 : 64; }
            @Override public boolean isItemValid(int slot,ItemStack stack) { return slot==0 && (mode()==1 || mode()==3) && items.getStackInSlot(1).isEmpty(); }
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return isItemValid(slot,stack) ? items.insertItem(slot,stack,simulate) : stack; }
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return slot==1 && (mode()==2 || mode()==3) ? items.extractItem(slot,amount,simulate) : ItemStack.EMPTY; }
        };
    }
    private void reset() { progress=0; recipeId=null; workInput=List.of(); }
    public List<BlockPos> pedestalPositions() { return Arrays.stream(OFFSETS).map(o -> worldPosition.offset(o[0],0,o[1])).toList(); }
    @Override public void step() {
        if ((autoOutput || craftingDelivery) && !items.getStackInSlot(1).isEmpty()) {
            exportOutput(); if (items.getStackInSlot(1).isEmpty()) { craftingDelivery=false; setChanged(); }
        }
        if (!items.getStackInSlot(1).isEmpty()) { status="output_full"; return; }
        if (!getMainNode().isActive()) { status="offline"; return; }
        if (items.getStackInSlot(0).isEmpty()) { reset(); status="missing_inputs"; return; }
        var pedestals=new ArrayList<InfusionPedestalTileEntity>();
        var inputItems=new ArrayList<ItemStack>(); inputItems.add(items.getStackInSlot(0).copy());
        for (var pos:pedestalPositions()) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof InfusionPedestalTileEntity pedestal)) {
                reset(); status="missing_infusion_pedestals"; return;
            }
            pedestals.add(pedestal); inputItems.add(pedestal.getInventory().getStackInSlot(0).copy());
        }
        var positioned=CraftingInput.ofPositioned(3,3,inputItems);var input=positioned.input();
        var holder=level.getRecipeManager().getRecipeFor(ModRecipeTypes.INFUSION.get(),input,level).orElse(null);
        if (holder==null) { reset(); status="missing_inputs"; return; }
        boolean same=holder.id().equals(recipeId) && workInput.size()==inputItems.size();
        if (same) for (int i=0;i<inputItems.size();i++) if (!ItemStack.matches(inputItems.get(i),workInput.get(i))) { same=false; break; }
        if (!same) { progress=0; recipeId=holder.id(); workInput=inputItems.stream().map(ItemStack::copy).toList(); }
        status="infusing"; progress=Math.min(Balance.ALTAR_TICKS.get(),progress+speed()); setChanged();
        if (progress<Balance.ALTAR_TICKS.get()) {
            for (var pedestal:pedestals) InfusionEffects.converge((net.minecraft.server.level.ServerLevel)level,worldPosition,pedestal.getBlockPos(),pedestal.getInventory().getStackInSlot(0));
            return;
        }
        // Calculate every result before mutating the real pedestal inventories.
        var recipe=holder.value(); var output=recipe.assemble(input,level.registryAccess()); var compactRemaining=recipe.getRemainingItems(input);
        if (output.isEmpty() || compactRemaining.size()!=input.size()) { reset(); status="missing_inputs"; return; }
        // CraftingInput trims empty rows/columns. Map remainders back to the nine
        // physical slots so short recipes do not fail or move returned containers.
        var remaining=NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i=0;i<compactRemaining.size();i++)
            remaining.set((i/input.width()+positioned.top())*3+i%input.width()+positioned.left(),compactRemaining.get(i));
        if (output.getCount()>output.getMaxStackSize()) { status="output_full"; return; }
        if (remaining.getFirst().getCount()>1) { status="output_full"; return; }
        for (int i=0;i<8;i++) {
            var rest=remaining.get(i+1);
            if (rest.getCount()>pedestals.get(i).getInventory().getSlotLimit(0)) { status="output_full"; return; }
        }
        for (int i=0;i<8;i++) {
            pedestals.get(i).getInventory().setStackInSlot(0,remaining.get(i+1).copy());
            InfusionEffects.finishPedestal((net.minecraft.server.level.ServerLevel)level,pedestals.get(i).getBlockPos());
        }
        items.setStackInSlot(0,remaining.getFirst().copy()); items.setStackInSlot(1,output.copy()); reset(); setChanged();
        InfusionEffects.finishCenter((net.minecraft.server.level.ServerLevel)level,worldPosition);
    }
    @Override public appeng.api.implementations.blockentities.PatternContainerGroup getCraftingMachineInfo() {
        return new appeng.api.implementations.blockentities.PatternContainerGroup(AEItemKey.of(new ItemStack(Agristics.ME_INFUSION.get())),getDisplayName(),List.of());
    }
    @Override public boolean acceptsPlans() {
        if (level==null || !getMainNode().isActive() || !items.getStackInSlot(0).isEmpty() || !items.getStackInSlot(1).isEmpty()) return false;
        for (var pos:pedestalPositions()) if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof InfusionPedestalTileEntity pedestal) || !pedestal.getInventory().getStackInSlot(0).isEmpty()) return false;
        return true;
    }
    @Override public boolean pushPattern(appeng.api.crafting.IPatternDetails pattern,appeng.api.stacks.KeyCounter[] inputs,Direction side) {
        if (!acceptsPlans() || pattern==null || pattern.getOutputs().size()!=1) return false;
        var pool=new LinkedHashMap<AEItemKey,Long>(); long total=0;
        for (var counter:inputs) for (var entry:counter) {
            if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue()<=0 || entry.getLongValue()>9) return false;
            total+=entry.getLongValue(); if (total>9) return false; pool.merge(key,entry.getLongValue(),Long::sum);
        }
        for (var holder:level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            var recipe=holder.value(); var output=recipe.getResultItem(level.registryAccess()); var requested=pattern.getPrimaryOutput();
            if (!(requested.what() instanceof AEItemKey key) || key.getItem()!=output.getItem() || requested.amount()!=output.getCount() || recipe.getIngredients().size()>8) continue;
            for (var center:List.copyOf(pool.keySet())) {
                if (!recipe.getAltarIngredient().test(center.toStack(1))) continue;
                var remaining=new LinkedHashMap<>(pool); remaining.put(center,remaining.get(center)-1);
                var arranged=new ArrayList<ItemStack>(Collections.nCopies(8,ItemStack.EMPTY));
                if (!arrange(recipe.getIngredients(),0,remaining,arranged) || remaining.values().stream().anyMatch(n->n!=0)) continue;
                var all=new ArrayList<ItemStack>(); all.add(center.toStack(1)); all.addAll(arranged);
                if (!recipe.matches(CraftingInput.of(3,3,all),level)) continue;
                var actual=recipe.assemble(CraftingInput.of(3,3,all),level.registryAccess());
                if (!key.matches(actual) || actual.getCount()!=requested.amount()) continue;
                // Publish all nine physical slots only after a complete recipe match. Failed pushes mutate nothing.
                for (int i=0;i<8;i++) ((InfusionPedestalTileEntity)level.getBlockEntity(pedestalPositions().get(i))).getInventory().setStackInSlot(0,arranged.get(i).copy());
                items.setStackInSlot(0,center.toStack(1)); craftingDelivery=true; setChanged(); return true;
            }
        }
        return false;
    }
    private static boolean arrange(List<net.minecraft.world.item.crafting.Ingredient> ingredients,int index,Map<AEItemKey,Long> pool,List<ItemStack> result) {
        if (index==ingredients.size()) return true;
        var ingredient=ingredients.get(index);
        if (ingredient.isEmpty()) { result.set(index,ItemStack.EMPTY); return arrange(ingredients,index+1,pool,result); }
        for (var key:pool.keySet()) if (pool.get(key)>0 && ingredient.test(key.toStack(1))) {
            pool.put(key,pool.get(key)-1); result.set(index,key.toStack(1));
            if (arrange(ingredients,index+1,pool,result)) return true;
            pool.put(key,pool.get(key)+1);
        }
        result.set(index,ItemStack.EMPTY); return false;
    }
    private void exportOutput() {
        var output=items.getStackInSlot(1); if (output.isEmpty()) return;
        var grid=getMainNode().getGrid();
        if (grid!=null && getMainNode().isActive()) {
            long accepted=grid.getStorageService().getInventory().insert(AEItemKey.of(output),output.getCount(),Actionable.MODULATE,IActionSource.ofMachine(this));
            items.extractItem(1,(int)accepted,false);
        }
        for (var side:Direction.values()) {
            if (sides[side.ordinal()]!=2 && sides[side.ordinal()]!=3) continue;
            var pos=worldPosition.relative(side); if (!level.hasChunkAt(pos)) continue;
            var target=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side.getOpposite()); if (target==null) continue;
            for (int slot=0;slot<target.getSlots();slot++) {
                var stack=items.getStackInSlot(1); if (stack.isEmpty()) return;
                var rest=target.insertItem(slot,stack.copy(),false); int accepted=stack.getCount()-rest.getCount();
                if (accepted>0) items.extractItem(1,accepted,false);
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); tag.putInt("progress",progress); tag.putBoolean("autoOutput",autoOutput); tag.putBoolean("craftingDelivery",craftingDelivery); tag.putIntArray("sides",sides);
        if (recipeId!=null) tag.putString("recipe",recipeId.toString());
        var saved=new net.minecraft.nbt.ListTag(); for (var stack:workInput) saved.add(stack.saveOptional(registries)); tag.put("workInput",saved);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); progress=Math.max(0,tag.getInt("progress")); recipeId=ResourceLocation.tryParse(tag.getString("recipe")); autoOutput=tag.getBoolean("autoOutput"); craftingDelivery=tag.getBoolean("craftingDelivery");
        var modes=tag.getIntArray("sides"); if (modes.length==6) for (int i=0;i<6;i++) sides[i]=Math.clamp(modes[i],0,3);
        var saved=new ArrayList<ItemStack>(); for (var raw:tag.getList("workInput",10)) saved.add(ItemStack.parseOptional(registries,(CompoundTag)raw)); workInput=List.copyOf(saved);
    }
    @Override public CompoundTag snapshot(int page,String search) {
        var tag=super.snapshot(page,search); tag.putInt("progress",progress); tag.putInt("duration",Balance.ALTAR_TICKS.get());
        tag.putBoolean("autoOutput",autoOutput); tag.putIntArray("sides",sides); tag.putInt("acceleration",accelerationCards()); return tag;
    }
}
