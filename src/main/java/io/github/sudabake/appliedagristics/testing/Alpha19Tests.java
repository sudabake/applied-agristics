package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Alpha19Tests {
    private static final ResourceLocation COW=ResourceLocation.parse("minecraft:cow");
    private static ItemEntity drop(GameTestHelper h,BiologicalAltar altar,ItemStack stack) {
        var entity=new ItemEntity(h.getLevel(),altar.getBlockPos().getX()+1.2,altar.getBlockPos().getY()+.2,altar.getBlockPos().getZ()+.5,stack);h.getLevel().addFreshEntity(entity);return entity;
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void biologicalRitualTransaction(GameTestHelper h) {
        var pos=new BlockPos(3,1,3);h.setBlock(pos,Agristics.BIO_ALTAR.get());BiologicalAltar altar=h.getBlockEntity(pos);
        var seed=new ItemStack(Agristics.EMPTY_BIO_SEED.get());
        {var before=altar.status;h.assertTrue(!altar.tryInsertSeed(seed,true) && altar.status.equals(before),"Failed insertion simulation is side effect free");}
        h.assertTrue(!altar.tryInsertSeed(seed,false),"Missing ground materials refuse the blank seed");
        var extract=drop(h,altar,BioData.withIdentity(Agristics.EXTRACT.get(),COW).copyWithCount(4));
        h.assertTrue(!altar.tryInsertSeed(seed,false) && extract.getItem().getCount()==4,"A partial recipe changes no dropped materials");
        var essence=drop(h,altar,new ItemStack(Agristics.CONDENSED_ESSENCE.get(),4));
        h.assertTrue(altar.tryInsertSeed(seed,true) && extract.getItem().getCount()==4,"Simulation does not consume dropped items");
        h.assertTrue(altar.tryInsertSeed(seed,false) && extract.getItem().getCount()==3 && essence.getItem().getCount()==3,"Trigger consumes exactly one of each ground material");
        {var before=altar.status;h.assertTrue(!altar.tryInsertSeed(seed,true) && altar.status.equals(before),"Failed insertion simulation is side effect free");}
        h.assertTrue(!altar.tryInsertSeed(seed,false),"A running altar cannot accept a second job");
        altar.step();var save=altar.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new BiologicalAltar(altar.getBlockPos(),altar.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(save,h.getLevel().registryAccess());
        h.assertTrue(restored.breakContents().stream().mapToInt(ItemStack::getCount).sum()==3,"Breaking returns only the three paid inputs");
        restored.items.setStackInSlot(1,new ItemStack(Items.STONE,64));int progress=restored.snapshot(0,"").getInt("progress");restored.step();
        h.assertTrue(restored.snapshot(0,"").getInt("progress")==progress,"Blocked output pauses a paid ritual");restored.items.setStackInSlot(1,ItemStack.EMPTY);
        for(int i=progress;i<save.getInt("duration");i++)restored.step();
        h.assertTrue(restored.items.getStackInSlot(1).getCount()==1 && COW.equals(BioData.identity(restored.items.getStackInSlot(1))),"Reloaded ritual completes exactly one identified seed");
        h.assertTrue(restored.breakContents().stream().mapToInt(ItemStack::getCount).sum()==1,"Completed ritual has no duplicated refundable materials");
        h.assertTrue(restored.automation(Direction.DOWN).extractItem(1,1,false).getCount()==1 && restored.automation(Direction.DOWN).extractItem(1,1,false).isEmpty(),"Output can be extracted only once");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void cultureImpactAndSavedIdentity(GameTestHelper h) {
        var bottle=new ThrownCulture(Agristics.THROWN_CULTURE.get(),h.getLevel());bottle.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());bottle.setItem(BioData.withIdentity(Agristics.CULTURE.get(),COW));
        var tag=new net.minecraft.nbt.CompoundTag();bottle.addAdditionalSaveData(tag);
        var restored=new ThrownCulture(Agristics.THROWN_CULTURE.get(),h.getLevel());restored.readAdditionalSaveData(tag);restored.setPos(bottle.position());
        h.assertTrue(COW.equals(BioData.identity(restored.getItem())),"In-flight save preserves the species component");
        var box=restored.getBoundingBox().inflate(1);int before=h.getLevel().getEntitiesOfClass(ItemEntity.class,box).size();
        restored.resolveImpact();restored.resolveImpact();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box);
        h.assertTrue(drops.size()==before+1 && drops.stream().anyMatch(e->e.getItem().is(Agristics.EXTRACT.get()) && e.getItem().getCount()==1 && COW.equals(BioData.identity(e.getItem()))),"Duplicate impact callbacks produce exactly one identified extract, no capsule");
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.getFoodData().setFoodLevel(10);Agristics.NOURISHMENT.get().applyEffectTick(player,0);
        h.assertTrue(player.getFoodData().getFoodLevel()==11,"Nourishment restores one food point per effect pulse");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=100)
    public static void altarCardsAndNoChannel(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.ME_INFUSION.get());MEInfusionAltar altar=h.getBlockEntity(pos);altar.ensureNode();
        h.assertTrue(!altar.getMainNode().getNode().hasFlag(appeng.api.networking.GridFlags.REQUIRE_CHANNEL),"ME infusion altar does not consume a channel");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var cards=appeng.core.definitions.AEItems.SPEED_CARD.stack(6);player.setItemInHand(InteractionHand.MAIN_HAND,cards);
        var hit=new BlockHitResult(h.absolutePos(pos).getCenter(),Direction.UP,h.absolutePos(pos),false);
        for(int i=0;i<6;i++) altar.getBlockState().useItemOn(cards,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(cards.getCount()==2 && altar.accelerationCards()==4,"Right click fills four independent slots and never consumes a fifth card");
        for(int i=2;i<6;i++) h.assertTrue(altar.items.getStackInSlot(i).getCount()==1,"Each acceleration slot holds exactly one card");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=60)
    public static void dispenserCulture(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING,Direction.EAST));
        var dispenser=(net.minecraft.world.level.block.entity.DispenserBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        dispenser.setItem(0,BioData.withIdentity(Agristics.CULTURE.get(),COW));
        h.setBlock(pos.below(),Blocks.REDSTONE_BLOCK);
        h.runAtTickTime(8,()->{
            var bottles=h.getLevel().getEntitiesOfClass(ThrownCulture.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(6));
            h.assertTrue(dispenser.getItem(0).isEmpty() && bottles.size()==1 && COW.equals(BioData.identity(bottles.getFirst().getItem())),"Native dispenser consumes one culture and preserves its identity");h.succeed();
        });
    }
}
