package io.github.sudabake.appliedagristics.block;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Vector3f;
import java.util.*;

/** Standalone ritual. Paid materials and the planned result travel together across saves. */
public final class BiologicalAltar extends MachineEntity {
    private List<Vec3> sources=List.of();
    private List<ItemStack> escrow=List.of();
    private ItemStack result=ItemStack.EMPTY;
    private int progress,duration=120;
    private boolean running;
    private ItemStack clientVisual=ItemStack.EMPTY;
    public BiologicalAltar(BlockPos pos,BlockState state) { super(Agristics.BIO_ALTAR_BE.get(),pos,state,2); }
    @Override protected boolean valid(int slot,ItemStack stack) { return false; }
    @Override protected int slotLimit(int slot) { return slot==0?1:64; }
    /** Direct altar interaction; this block never creates a container menu. */
    public void interact(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        if(level==null || level.isClientSide || player.isSpectator())return;
        int output=!items.getStackInSlot(1).isEmpty()?1:!items.getStackInSlot(0).isEmpty()?0:-1;
        if(output>=0) {
            var stack=items.getStackInSlot(output).copy();var drop=new ItemEntity(level,player.getX(),player.getY(),player.getZ(),stack);drop.setNoPickUpDelay();
            if(level.addFreshEntity(drop))items.extractItem(output,stack.getCount(),false);return;
        }
        var held=player.getItemInHand(hand);
        if(held.is(Agristics.EMPTY_BIO_SEED.get())) {
            if(tryInsertSeed(held,false)) {held.consume(1,player);level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
            else if(escrow.isEmpty())player.displayClientMessage(net.minecraft.network.chat.Component.translatable("status.appliedagristics.missing_ground_materials"),true);
        }
    }
    @Override public void open(net.minecraft.server.level.ServerPlayer player) { interact(player,net.minecraft.world.InteractionHand.MAIN_HAND); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.entity.player.Player player) { return null; }
    public ItemStack visualItem() {if(level!=null&&level.isClientSide)return clientVisual;return !items.getStackInSlot(1).isEmpty()?items.getStackInSlot(1):!items.getStackInSlot(0).isEmpty()?items.getStackInSlot(0):escrow.isEmpty()?ItemStack.EMPTY:escrow.getFirst();}
    public boolean tryInsertSeed(ItemStack seed,boolean simulate) {
        if(level==null || level.isClientSide || !escrow.isEmpty() || !items.getStackInSlot(0).isEmpty() || !items.getStackInSlot(1).isEmpty() || seed.isEmpty())return false;
        var nearby=level.getEntitiesOfClass(ItemEntity.class,new AABB(worldPosition).inflate(3),e->e.isAlive() && !e.getItem().isEmpty());
        nearby.sort(Comparator.comparingDouble((ItemEntity e)->e.distanceToSqr(worldPosition.getCenter())).thenComparingInt(ItemEntity::getId));
        for(var holder:level.getRecipeManager().getAllRecipesFor(Agristics.BIO_INFUSION_TYPE.get())) {
            var recipe=holder.value();if(!recipe.center().test(seed))continue;
            for(var extract:nearby) {
                if(!extract.getItem().is(Agristics.EXTRACT.get()) || !BioData.identified(extract.getItem()))continue;
                for(var essence:nearby) {
                    if(essence==extract || !recipe.essence().test(essence.getItem()))continue;
                    var input=CraftingInput.of(3,1,List.of(seed.copyWithCount(1),extract.getItem().copyWithCount(1),essence.getItem().copyWithCount(1)));
                    if(!recipe.matches(input,level))continue;
                    var output=recipe.assemble(input,level.registryAccess());if(!outputFits(output))continue;
                    if(simulate)return true;
                    escrow=List.of(input.getItem(0).copy(),input.getItem(1).copy(),input.getItem(2).copy());
                    result=output.copy();duration=recipe.ticks();progress=0;sources=List.of(extract.position(),essence.position());
                    consumeOne(extract);consumeOne(essence);status="bio_ritual";setChanged();sync();return true;
                }
            }
        }
        if(!simulate)status="missing_ground_materials";return false;
    }
    private static void consumeOne(ItemEntity entity) { var rest=entity.getItem().copy();rest.shrink(1);if(rest.isEmpty())entity.discard();else entity.setItem(rest); }
    private boolean outputFits(ItemStack output) {
        var existing=items.getStackInSlot(1);
        return !output.isEmpty() && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing,output)) && existing.getCount()+output.getCount()<=output.getMaxStackSize();
    }
    @Override protected void inventoryChanged(int slot) { sync(); }
    private void sync() { if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS); }
    @Override public void step() {
        if (!(level instanceof ServerLevel server)) return;
        boolean previous=running; running=false;
        if(escrow.isEmpty()) {status="add_seed";if(previous)sync();return;}
        if(!outputFits(result)) { status="output_full"; if(previous) sync(); return; }
        running=true; status="bio_ritual"; progress++; setChanged();
        if(progress%5==0) { ritualParticles(server); sync(); }
        if(progress>=duration) {
            var output=result.copyWithCount(items.getStackInSlot(1).getCount()+result.getCount());
            var identity=BioData.identity(result);
            // Clear the paid job before publishing the output and notifying adjacent automation.
            escrow=List.of(); result=ItemStack.EMPTY; progress=0; running=false;
            items.setStackInSlot(1,output); status="idle"; setChanged(); sync();
            if(identity!=null) {
                try {
                    var mob=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(identity).create(server);
                    if(mob instanceof Mob living) { living.setPos(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5); living.playAmbientSound(); }
                } catch(RuntimeException ignored) { /* Optional modded ambient sound must not cancel a paid recipe. */ }
            }
        }
    }
    private void ritualParticles(ServerLevel server) {
        double t=progress/(double)Math.max(1,duration);var color=new DustParticleOptions(new Vector3f(.48f,.85f,.55f),.8f);var center=worldPosition.getCenter();
        for(int i=0;i<12;i++) {
            double angle=2*Math.PI*i/12;Vec3 point;
            if(t<.2) {
                var origin=sources.isEmpty()?center:sources.get(i%sources.size());var ring=new Vec3(center.x+2*Math.cos(angle),worldPosition.getY()+1,center.z+2*Math.sin(angle));
                point=origin.lerp(ring,t/.2);
            } else {
                double a=Math.min(1,(t-.2)/.55),radius=2*(1-a);angle+=2*Math.PI*a;
                point=new Vec3(center.x+Math.cos(angle)*radius,worldPosition.getY()+1+.3*a,center.z+Math.sin(angle)*radius);
            }
            server.sendParticles(color,point.x,point.y,point.z,2,.025,.025,.025,0);
        }
    }
    public float ritualProgress(float partial) { return Math.min(1,(progress+(running?partial:0))/(float)Math.max(1,duration)); }
    public ResourceLocation ritualSpecies() { return BioData.identity(result); }
    public IItemHandler automation(Direction side) {
        return new IItemHandler() {
            @Override public int getSlots() { return 2; }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot,ItemStack stack) { return slot==0 && side!=Direction.DOWN && tryInsertSeed(stack,true); }
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return slot==0 && side!=Direction.DOWN && tryInsertSeed(stack,simulate)?stack.copyWithCount(stack.getCount()-1):stack; }
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return slot==1 && (side==null || side==Direction.DOWN)?items.extractItem(slot,amount,simulate):ItemStack.EMPTY; }
        };
    }
    @Override public List<ItemStack> breakContents() { var drops=super.breakContents(); for(var stack:escrow) if(!stack.isEmpty()) drops.add(stack.copy()); return drops; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); var paid=new ListTag(); for(var stack:escrow) paid.add(stack.saveOptional(registries));
        var points=new ListTag();for(var pos:sources){var e=new CompoundTag();e.putDouble("x",pos.x);e.putDouble("y",pos.y);e.putDouble("z",pos.z);points.add(e);}tag.put("sources",points);tag.put("paid",paid); tag.put("ritualResult",result.saveOptional(registries)); tag.putInt("progress",progress); tag.putInt("duration",duration);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); var paid=new ArrayList<ItemStack>(); for(var raw:tag.getList("paid",10)) paid.add(ItemStack.parseOptional(registries,(CompoundTag)raw));
        var points=new ArrayList<Vec3>();for(var raw:tag.getList("sources",10)){var e=(CompoundTag)raw;points.add(new Vec3(e.getDouble("x"),e.getDouble("y"),e.getDouble("z")));}sources=List.copyOf(points);
        escrow=List.copyOf(paid); result=ItemStack.parseOptional(registries,tag.getCompound("ritualResult")); duration=Math.clamp(tag.getInt("duration"),40,1200000); progress=Math.clamp(tag.getInt("progress"),0,duration);
    }
    @Override public CompoundTag snapshot(int page,String search) { var tag=super.snapshot(page,search); tag.putInt("progress",progress);tag.putInt("duration",duration);return tag; }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag=snapshot(0,"");tag.putBoolean("running",running);tag.put("ritualResult",result.saveOptional(registries));tag.put("center",items.getStackInSlot(0).saveOptional(registries));tag.put("visual",visualItem().saveOptional(registries));tag.put("output",items.getStackInSlot(1).saveOptional(registries));return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries) {
        clientVisual=ItemStack.parseOptional(registries,tag.getCompound("visual"));progress=tag.getInt("progress");duration=tag.getInt("duration");running=tag.getBoolean("running");result=ItemStack.parseOptional(registries,tag.getCompound("ritualResult"));
        items.setStackInSlot(0,ItemStack.parseOptional(registries,tag.getCompound("center")));items.setStackInSlot(1,ItemStack.parseOptional(registries,tag.getCompound("output")));
    }
}
