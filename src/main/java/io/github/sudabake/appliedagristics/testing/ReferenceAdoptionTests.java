package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ReferenceAdoptionTests {
    private static CompoundTag entry(CompoundTag key,long count) {
        var entry=new CompoundTag();entry.put("key",key.copy());entry.putLong("count",count);return entry;
    }
    private static CompoundTag missing() {
        var key=new CompoundTag();key.putString("id","missing_mod:seed");return key;
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void unknownCountsSurvivePortableStorage(GameTestHelper h) {
        var registries=h.getLevel().registryAccess();
        for (var block:List.of(Agristics.SEEDS.get(),Agristics.ESSENCE_VAULT.get())) {
            var machine=(MachineEntity)block.newBlockEntity(h.absolutePos(new BlockPos(1,1,1)),block.defaultBlockState());machine.setLevel(h.getLevel());
            String name=machine instanceof SeedStorage ? "seeds" : "essences";
            var valid=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)).toTag(registries);
            var badComponent=valid.copy();var components=new CompoundTag();components.putInt("missing_mod:identity",123);badComponent.put("components",components);
            var raw=entry(missing(),9000000000L);raw.putString("futureMetadata","keep verbatim");
            var list=new ListTag();list.add(entry(valid,3));list.add(entry(valid,4));list.add(raw);list.add(entry(badComponent,17));
            var tag=machine.saveWithFullMetadata(registries);tag.put(name,list);machine.loadWithComponents(tag,registries);
            h.assertTrue(machine.snapshot(0,"").getInt("unresolved")==2,"Missing item and component are both visible as recovery entries");
            var item=new ItemStack(block);machine.saveToItem(item,registries);
            var restored=(MachineEntity)block.newBlockEntity(machine.getBlockPos(),block.defaultBlockState());restored.setLevel(h.getLevel());
            restored.loadWithComponents(item.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),registries);
            var saved=restored.saveWithFullMetadata(registries).getList(name,Tag.TAG_COMPOUND);
            h.assertTrue(saved.size()==3 && saved.getCompound(0).getLong("count")==7 && saved.contains(raw) && saved.contains(entry(badComponent,17)),"Pickup and reload preserve valid merged counts and exact unreadable records");
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void overflowRecordCanResolveAfterSpaceReturns(GameTestHelper h) {
        var registries=h.getLevel().registryAccess();var key=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
        var list=new ListTag();list.add(entry(key.toTag(registries),Long.MAX_VALUE));list.add(entry(key.toTag(registries),3));
        var map=new LinkedHashMap<AEItemKey,Long>();var unresolved=StoredItemCounts.load(list,map,registries);
        h.assertTrue(map.get(key)==Long.MAX_VALUE && unresolved.size()==1,"Duplicate overflow neither wraps nor crashes");
        map.put(key,Long.MAX_VALUE-10);var saved=StoredItemCounts.save(map,unresolved,registries);map.clear();
        unresolved=StoredItemCounts.load(saved,map,registries);
        h.assertTrue(unresolved.isEmpty() && map.get(key)==Long.MAX_VALUE-7,"Retained entry resolves on reload when representable");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void unreadablePaidOutputSurvivesDismantling(GameTestHelper h) {
        var registries=h.getLevel().registryAccess();var pos=h.absolutePos(new BlockPos(1,1,1));
        var machine=new NetworkPlanter(pos,Agristics.NETWORK.get().defaultBlockState());machine.setLevel(h.getLevel());
        var task=new CompoundTag();task.putString("id","minecraft:wheat");task.putBoolean("enabled",true);
        var raw=entry(missing(),97);raw.putString("futureMetadata","preserve");
        var batch=new ListTag();batch.add(raw);task.put("batch",batch);
        var unknownTask=new CompoundTag();unknownTask.putString("id","not a resource id");unknownTask.putString("future","preserve");
        var tasks=new ListTag();tasks.add(task);tasks.add(unknownTask);
        var tag=machine.saveWithFullMetadata(registries);tag.put("tasks",tasks);machine.loadWithComponents(tag,registries);
        var saved=machine.saveWithFullMetadata(registries);
        h.assertTrue(saved.getList("tasks",10).getCompound(0).getList("batch",10).equals(batch) && saved.getList("tasks",10).contains(unknownTask),"Paid batch and unreadable task survive save-load-save");
        machine.dropContents();var queue=DeferredDrops.get(h.getLevel()).save(new CompoundTag(),registries);
        h.assertTrue(queue.getList("sites",10).stream().map(CompoundTag.class::cast).anyMatch(site->site.getList("stacks",10).contains(raw)),"Dismantling transfers unresolved paid output into the world-owned drop queue");
        h.assertTrue(!queue.getList("recoveries",10).isEmpty(),"Unparseable task metadata remains in world recovery data");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void menuIdentityRejectsReopenedAndOtherMenus(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var machine=new MEInfusionAltar(h.absolutePos(new BlockPos(1,1,1)),Agristics.ME_INFUSION.get().defaultBlockState());machine.setLevel(h.getLevel());
        var menu=new MachineMenu(7,player.getInventory(),machine,123L);
        h.assertTrue(menu.matches(machine.getBlockPos(),7,123L) && !menu.matches(machine.getBlockPos(),7,124L) && !menu.matches(machine.getBlockPos(),8,123L) && !menu.matches(machine.getBlockPos().above(),7,123L),"Session, container and position must all match, even after container ID wraps");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var snapshot=new Messages.Snapshot(machine.getBlockPos(),7,123L,new CompoundTag());Messages.Snapshot.CODEC.encode(buffer,snapshot);var decoded=Messages.Snapshot.CODEC.decode(buffer);
            h.assertTrue(menu.matches(decoded.pos(),decoded.containerId(),decoded.sessionToken()) && !buffer.isReadable(),"Snapshot codec preserves menu identity");
        } finally {buffer.release();}
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void sharedOutputTargetsSeeSameBatchAndExternalConsumption(GameTestHelper h) {
        var fixture=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.runAtTickTime(100,()-> {
            var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
            var recipes=new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(original);
            for (var item:List.of(Items.COOKIE,Items.BREAD))
                recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ResourceLocation.parse("appliedagristics:test/shared_"+item.toString().replace(':','_')),
                    new PlantingRecipe(net.minecraft.world.item.crafting.Ingredient.of(item),net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.level.block.Blocks.DIRT),
                    List.of(new ItemStack(Items.DIAMOND,2)),net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(),false,200,true)));
            try {
                manager.replaceRecipes(recipes);CropSources.invalidate();
                var planter=fixture.controller();
                for(var item:List.of(Items.COOKIE,Items.BREAD)) {
                    var stack=new ItemStack(item);fixture.seeds().add(AEItemKey.of(stack),1);
                    var id=Cultivations.id(stack,h.getLevel());planter.configure(id,true,1);planter.tasks.get(id).progress=200;
                }
                planter.step();var product=AEItemKey.of(new ItemStack(Items.DIAMOND));
                h.assertTrue(fixture.producedCount(product)==2,"The second crop sees the first crop's complete batch and pauses at its target");
                var inventory=planter.getMainNode().getGrid().getStorageService().getInventory();
                h.assertTrue(inventory.getAvailableStacks().get(product)==2,"AE sees final batch amounts immediately after the production scope");
                inventory.extract(product,2,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.ofMachine(planter));
                planter.step();
                h.assertTrue(fixture.producedCount(product)==2,"External consumption wakes replenishment on the next five-tick check");
                h.succeed();
            } finally {manager.replaceRecipes(original);CropSources.invalidate();}
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void multipleCropWorkloadProfile(GameTestHelper h) {
        var fixture=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);fixture.modules(4,4,false);
        var crops=Crops.all().stream().filter(c->c.getCruxBlock()==null && c.getRequiredBiomes().isEmpty()).limit(32).toList();
        for(var crop:crops) fixture.seeds().add(AEItemKey.of(new ItemStack(crop.getSeedsItem())),256);
        h.runAtTickTime(100,()-> {
            var planter=fixture.controller();double chance=Balance.FALLBACK_SEED_CHANCE.get();Balance.FALLBACK_SEED_CHANCE.set(0.0);
            try {
                for(int count:new int[]{1,8,32}) {
                    planter.configureAll(false);for(var crop:crops.subList(0,count))planter.configure(crop.getId(),true,-1);
                    for(int step=0;step<60;step++)planter.step();
                    long[] samples=new long[120];long started=System.nanoTime();
                    for(int step=0;step<samples.length;step++){long start=System.nanoTime();planter.step();samples[step]=System.nanoTime()-start;}
                    long elapsed=System.nanoTime()-started;Arrays.sort(samples);
                    long growing=planter.tasks.values().stream().filter(t->t.enabled && t.status.equals("growing")).count();
                    h.assertTrue(growing==count,"All configured crops are actually producing during the measurement");
                    com.mojang.logging.LogUtils.getLogger().info("MULTICROP_PROFILE crops={} steps={} meanNs={} p50Ns={} p95Ns={} maxNs={} power={}",count,samples.length,elapsed/samples.length,samples[60],samples[114],samples[119],planter.tasks.values().stream().mapToDouble(t->t.power).sum());
                }
                h.succeed();
            } finally {Balance.FALLBACK_SEED_CHANCE.set(chance);}
        });
    }
}
