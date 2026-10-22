package io.github.sudabake.appliedagristics.logic;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** A bounded world-owned handoff for dismantled bulk batches. Machine items remain data-free. */
@EventBusSubscriber(modid=Agristics.ID)
public final class DeferredDrops extends SavedData {
    public static final String NAME="appliedagristics_deferred_drops";
    public static final int STACK_BUDGET=16,NEARBY_LIMIT=64;
    private static final Factory<DeferredDrops> FACTORY=new Factory<>(DeferredDrops::new,DeferredDrops::load);
    private static final class Pending {
        final CompoundTag key;
        long count;
        Pending(CompoundTag key,long count) { this.key=key.copy();this.count=count; }
    }
    private record Site(BlockPos pos,List<Pending> stacks) {}
    private final List<Site> sites=new ArrayList<>();
    private int cursor;

    public static DeferredDrops get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY,NAME); }
    public void enqueue(ServerLevel level,BlockPos pos,Map<AEItemKey,Long> amounts) {
        var stacks=new ArrayList<Pending>();
        amounts.forEach((key,count)->{if(count>0) stacks.add(new Pending(key.toTag(level.registryAccess()),count));});
        if(stacks.isEmpty()) return;
        sites.add(new Site(pos.immutable(),stacks));setDirty();
        drain(level);
    }
    public void drain(ServerLevel level) {
        int budget=STACK_BUDGET,visited=0,limit=Math.min(32,sites.size());
        while(!sites.isEmpty() && budget>0 && visited++<limit) {
            cursor=Math.floorMod(cursor,sites.size());var site=sites.get(cursor);
            if(!level.hasChunkAt(site.pos)) {cursor++;continue;}
            int nearby=level.getEntitiesOfClass(ItemEntity.class,new AABB(site.pos).inflate(4)).size();
            if(nearby>=NEARBY_LIMIT) {cursor++;continue;}
            var iterator=site.stacks.iterator();
            while(iterator.hasNext() && budget>0 && nearby<NEARBY_LIMIT) {
                var pending=iterator.next();
                var key=AEItemKey.fromTag(level.registryAccess(),pending.key);
                // Preserve the serialized key while its mod is absent; never silently discard its count.
                if(key==null || key.toStack(1).isEmpty()) continue;
                while(pending.count>0 && budget>0 && nearby<NEARBY_LIMIT) {
                    int take=(int)Math.min(pending.count,Math.max(1,key.getMaxStackSize()));
                    var entity=new ItemEntity(level,site.pos.getX()+.5,site.pos.getY()+.5,site.pos.getZ()+.5,key.toStack(take));
                    entity.setDefaultPickUpDelay();
                    if(!level.addFreshEntity(entity)) break;
                    pending.count-=take;budget--;nearby++;setDirty();
                }
                if(pending.count==0) iterator.remove();
            }
            if(site.stacks.isEmpty()) {sites.remove(cursor);setDirty();} else cursor++;
        }
    }
    public long queuedAt(BlockPos pos,AEItemKey key,HolderLookup.Provider registries) {
        long total=0;var tag=key.toTag(registries);
        for(var site:sites) if(site.pos.equals(pos)) for(var entry:site.stacks) if(entry.key.equals(tag)) total=Math.addExact(total,entry.count);
        return total;
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(event.getLevel() instanceof ServerLevel level && level.getGameTime()%5==0) {
            var data=level.getDataStorage().get(FACTORY,NAME);
            if(data!=null && !data.sites.isEmpty()) data.drain(level);
        }
    }
    public static DeferredDrops load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new DeferredDrops();
        for(var raw:tag.getList("sites",Tag.TAG_COMPOUND)) {
            var site=(CompoundTag)raw;var stacks=new ArrayList<Pending>();
            for(var value:site.getList("stacks",Tag.TAG_COMPOUND)) {
                var entry=(CompoundTag)value;long count=entry.getLong("count");
                if(count>0) stacks.add(new Pending(entry.getCompound("key"),count));
            }
            if(!stacks.isEmpty()) data.sites.add(new Site(BlockPos.of(site.getLong("pos")),stacks));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        var saved=new ListTag();
        for(var site:sites) {
            var entry=new CompoundTag();entry.putLong("pos",site.pos.asLong());var stacks=new ListTag();
            for(var pending:site.stacks) {var value=new CompoundTag();value.put("key",pending.key.copy());value.putLong("count",pending.count);stacks.add(value);}
            entry.put("stacks",stacks);saved.add(entry);
        }
        tag.put("sites",saved);return tag;
    }
}
