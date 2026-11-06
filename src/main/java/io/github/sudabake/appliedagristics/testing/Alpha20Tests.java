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
    public static void unboundedStoragePersistsCounts(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.SEEDS.get());SeedStorage bank=h.getBlockEntity(pos);
        for(int i=0;i<20;i++) {
            var key=AEItemKey.of(new ItemStack(Crops.all().get(i).getSeedsItem()));
            h.assertTrue(bank.add(key,1000000000L+i)==1000000000L+i,"Seed storage has no sixteen-type cap");
        }
        var wheat=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
        h.assertTrue(bank.add(wheat,10000000000L)==10000000000L,"Seed storage accepts large counts");
        var save=bank.saveWithFullMetadata(h.getLevel().registryAccess());bank.loadWithComponents(save,h.getLevel().registryAccess());
        for(int i=0;i<20;i++) h.assertTrue(bank.count(AEItemKey.of(new ItemStack(Crops.all().get(i).getSeedsItem())))==1000000000L+i,"Every current count survives saving");
        h.assertTrue(bank.count(wheat)==10000000000L,"Large counts survive saving");h.succeed();
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
