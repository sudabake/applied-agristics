package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Alpha20Tests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void unboundedStorageAndAlpha20Migration(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.LEGACY_SEEDS_FIRST.get());SeedStorage bank=h.getBlockEntity(pos);
        var tag=bank.saveWithFullMetadata(h.getLevel().registryAccess());tag.remove("seeds");tag.putInt("seedStorageVersion",2);var cells=new ListTag();var overflow=new ListTag();
        for(int i=0;i<20;i++) {var e=new CompoundTag();e.put("key",AEItemKey.of(new ItemStack(Crops.all().get(i).getSeedsItem())).toTag(h.getLevel().registryAccess()));e.putLong("count",1000000000L+i);e.putInt("slot",i);if(i<16)cells.add(e);else overflow.add(e);}
        tag.put("seedCells",cells);tag.put("overflow",overflow);bank.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(bank.keys().size()==20,"Alpha20 cells and overflow merge into one unbounded map");bank.step();bank=h.getBlockEntity(pos);
        h.assertTrue(bank.getBlockState().is(Agristics.SEEDS.get()),"A placed retired tier becomes a normal ME seed storage");
        for(int i=0;i<20;i++)h.assertTrue(bank.count(AEItemKey.of(new ItemStack(Crops.all().get(i).getSeedsItem())))==1000000000L+i,"Every migrated count survives block conversion");
        var wheat=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));h.assertTrue(bank.add(wheat,10000000000L)==10000000000L,"ME storage accepts more than sixteen types with no gameplay slot cap");
        var save=bank.saveWithFullMetadata(h.getLevel().registryAccess());bank.loadWithComponents(save,h.getLevel().registryAccess());h.assertTrue(bank.count(wheat)==10000000000L,"Merged map survives subsequent restart");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void cultureDeferredCommitAndSampleSwap(GameTestHelper h) {
        var cow=ResourceLocation.parse("minecraft:cow");var pig=ResourceLocation.parse("minecraft:pig");
        var be=new CellCultivator(h.absolutePos(new BlockPos(1,1,1)),Agristics.CULTIVATOR.get().defaultBlockState());be.setLevel(h.getLevel());
        be.items.setStackInSlot(0,BioData.withIdentity(Agristics.SAMPLER.get(),cow));be.items.setStackInSlot(1,new ItemStack(Agristics.NUTRIENT.get()));be.items.setStackInSlot(2,new ItemStack(Agristics.CAPSULE.get()));
        be.water.fill(new FluidStack(Fluids.WATER,100),FluidAction.EXECUTE);
        for(int i=0;i<30;i++)be.step();h.assertTrue(be.items.getStackInSlot(0).getCount()==1 && be.water.getFluidAmount()==100,"Inputs and water stay visible during cultivation");
        var drops=be.breakContents();h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum()==3,"Breaking cannot duplicate the visible unpaid sample");
        be.items.setStackInSlot(0,BioData.withIdentity(Agristics.SAMPLER.get(),pig));h.assertTrue(be.snapshot(0,"").getInt("progress")==0,"Swapping species resets paid progress");
        for(int i=0;i<Balance.CULTURE_TICKS.get()-1;i++)be.step();h.assertTrue(be.items.getStackInSlot(3).isEmpty() && be.items.getStackInSlot(0).getCount()==1,"No early output or material consumption");
        be.step();h.assertTrue(be.items.getStackInSlot(0).isEmpty() && be.items.getStackInSlot(1).isEmpty() && be.items.getStackInSlot(2).isEmpty() && be.water.getFluidAmount()==0 && pig.equals(BioData.identity(be.items.getStackInSlot(3))),"Completion atomically consumes the pig inputs exactly once");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=180)
    public static void planterHasNoDiversityBonus(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.runAtTickTime(100,()->{
            var a=f.controller();var crops=Crops.all().stream().filter(c->c.getCruxBlock()==null&&c.getRequiredBiomes().isEmpty()).limit(3).toList();
            for(var crop:crops){f.seeds().add(AEItemKey.of(new ItemStack(crop.getSeedsItem())),1);a.configure(crop.getId(),true,-1);}a.step();
            for(var crop:crops) {
                h.assertTrue(Math.abs(a.tasks.get(crop.getId()).power-Balance.POWER.get())<1e-8,"Power remains the original per-crop cost");
                h.assertTrue(a.tasks.get(crop.getId()).progress==5,"Multiple species do not grant any production bonus");
            }
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.getPersistentData().putBoolean("appliedagristics.hideOrdinary",true);
            h.assertTrue(new MachineMenu(1,player.getInventory(),a).search.equals("hide_ordinary"),"Reopening restores the player's crop filter");h.succeed();
        });
    }
}
