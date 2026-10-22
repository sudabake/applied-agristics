package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.NetworkPlanter;
import io.github.sudabake.appliedagristics.logic.DeferredDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class AuditRegressionTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void lootSlotsSurviveReloadRemovalAndRestart(GameTestHelper h) {
        var id=ResourceLocation.parse("minecraft:iron_golem");var registries=h.getLevel().registryAccess();
        var order=new BioLootOrder();
        var original=order.reconcile(Map.of(id,List.of(Items.IRON_INGOT,Items.POPPY))).get(id);
        h.assertTrue(original.equals(List.of(Items.IRON_INGOT,Items.POPPY)),"First upgrade keeps the existing alphabetical mapping");
        var restored=BioLootOrder.load(order.save(new CompoundTag(),registries),registries);
        var added=restored.reconcile(Map.of(id,List.of(Items.DIAMOND,Items.POPPY,Items.IRON_INGOT))).get(id);
        h.assertTrue(added.equals(List.of(Items.IRON_INGOT,Items.POPPY,Items.DIAMOND)),"A newly added earlier ID appends without shifting old patterns");
        var removed=restored.reconcile(Map.of(id,List.of(Items.POPPY,Items.DIAMOND))).get(id);
        h.assertTrue(removed.equals(List.of(Items.AIR,Items.POPPY,Items.DIAMOND)),"Removed output leaves a disabled slot");
        restored=BioLootOrder.load(restored.save(new CompoundTag(),registries),registries);
        var readded=restored.reconcile(Map.of(id,List.of(Items.DIAMOND,Items.IRON_INGOT,Items.POPPY))).get(id);
        h.assertTrue(readded.equals(added),"Returning drops regain their original slots after restart");
        h.assertTrue(BioLootOrder.get(h.getLevel())==BioLootOrder.get(h.getLevel()),"All dimensions use the world's persisted order instance");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void samplingOnlyConsumesSuccessfulInteractions(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var cow=EntityType.COW.create(h.getLevel());
        var sample=BioData.withIdentity(Agristics.SAMPLER.get(),ResourceLocation.parse("minecraft:cow"));
        player.setItemInHand(InteractionHand.MAIN_HAND,sample);
        var existing=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,EntityType.VILLAGER.create(h.getLevel()));
        NeoForge.EVENT_BUS.post(existing);
        h.assertTrue(!existing.isCanceled() && existing.getCancellationResult()==InteractionResult.PASS,"Labeled swab leaves villager interaction intact");
        var specific=new PlayerInteractEvent.EntityInteractSpecific(player,InteractionHand.MAIN_HAND,cow,Vec3.ZERO);
        NeoForge.EVENT_BUS.post(specific);h.assertTrue(!specific.isCanceled(),"Labeled swab does not consume specific interaction");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Agristics.SAMPLER.get()));
        var other=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,EntityType.ARMOR_STAND.create(h.getLevel()));
        NeoForge.EVENT_BUS.post(other);h.assertTrue(!other.isCanceled() && BioData.identity(player.getMainHandItem())==null,"Non-Mob passes without consuming a blank swab");
        var success=new PlayerInteractEvent.EntityInteractSpecific(player,InteractionHand.MAIN_HAND,cow,Vec3.ZERO);
        NeoForge.EVENT_BUS.post(success);
        h.assertTrue(success.isCanceled() && ResourceLocation.parse("minecraft:cow").equals(BioData.identity(player.getMainHandItem())),"Successful sampling consumes exactly the intended interaction");
        var spectator=h.makeMockPlayer(GameType.SPECTATOR);spectator.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Agristics.SAMPLER.get()));
        h.assertTrue(BioData.sample(spectator,InteractionHand.MAIN_HAND,cow)==InteractionResult.PASS,"Spectators cannot acquire samples");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void bulkDismantleIsBoundedAndRestartConservesItems(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);h.setBlock(pos,Agristics.NETWORK.get());NetworkPlanter machine=h.getBlockEntity(pos);
        var level=h.getLevel();var registries=level.registryAccess();var key=AEItemKey.of(new ItemStack(Items.WHEAT));long total=100_000_000L;
        var saved=machine.saveWithFullMetadata(registries);var tasks=new ListTag();var task=new CompoundTag();task.putString("id","minecraft:wheat_seeds");
        var batch=new ListTag();var output=new CompoundTag();output.put("key",key.toTag(registries));output.putLong("count",total);batch.add(output);task.put("batch",batch);tasks.add(task);saved.put("tasks",tasks);
        machine.loadWithComponents(saved,registries);var absolute=machine.getBlockPos();
        level.destroyBlock(absolute,true);
        var area=new AABB(absolute).inflate(4);var entities=level.getEntitiesOfClass(ItemEntity.class,area);
        long first=entities.stream().map(ItemEntity::getItem).filter(s->s.is(Items.WHEAT)).mapToLong(ItemStack::getCount).sum();
        var queue=DeferredDrops.get(level);
        h.assertTrue(first>0 && first<=DeferredDrops.STACK_BUDGET*64,"One hundred million items do not expand to millions of stacks at dismantle");
        h.assertTrue(queue.queuedAt(absolute,key,registries)+first==total,"Dropped plus queued equals the exact original count");
        var blockers=new ArrayList<ItemEntity>();
        for(int i=0;i<DeferredDrops.NEARBY_LIMIT;i++) {var entity=new ItemEntity(level,absolute.getX()+.5,absolute.getY()+.5,absolute.getZ()+.5,new ItemStack(Items.STONE));level.addFreshEntity(entity);blockers.add(entity);}
        long before=queue.queuedAt(absolute,key,registries);queue.drain(level);
        h.assertTrue(queue.queuedAt(absolute,key,registries)==before,"Nearby entity pressure pauses delivery without deleting items");
        var restored=DeferredDrops.load(queue.save(new CompoundTag(),registries),registries);level.getDataStorage().set(DeferredDrops.NAME,restored);
        h.assertTrue(restored.queuedAt(absolute,key,registries)==before,"Unemitted count persists across serialization and world-data reload");
        blockers.forEach(ItemEntity::discard);
        entities.stream().filter(e->e.getItem().is(Items.WHEAT)).forEach(ItemEntity::discard); // Collected into the test's accounting.
        restored.drain(level);
        long second=level.getEntitiesOfClass(ItemEntity.class,area).stream().map(ItemEntity::getItem).filter(s->s.is(Items.WHEAT)).mapToLong(ItemStack::getCount).sum();
        h.assertTrue(second>0 && second<=DeferredDrops.STACK_BUDGET*64 && first+second+restored.queuedAt(absolute,key,registries)==total,"Restart delivery remains bounded and never duplicates the collected portion");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void temporarilyMissingModDropsRemainSaved(GameTestHelper h) {
        var registries=h.getLevel().registryAccess();var key=AEItemKey.of(new ItemStack(Items.WHEAT)).toTag(registries);key.putString("id","appliedagristics_absent:seed");
        var value=new CompoundTag();value.put("key",key);value.putLong("count",1234);var stacks=new ListTag();stacks.add(value);
        var site=new CompoundTag();site.putLong("pos",h.absolutePos(new BlockPos(2,1,2)).asLong());site.put("stacks",stacks);var sites=new ListTag();sites.add(site);var tag=new CompoundTag();tag.put("sites",sites);
        var queue=DeferredDrops.load(tag,registries);queue.drain(h.getLevel());
        h.assertTrue(queue.save(new CompoundTag(),registries).equals(tag),"Unavailable item keys are preserved, not discarded or turned into air");
        h.succeed();
    }
}
