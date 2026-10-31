package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Cultivations;
import io.github.sudabake.appliedagristics.block.EarlyPlanter;
import io.github.sudabake.appliedagristics.logic.CropSources;
import io.github.sudabake.appliedagristics.logic.PlantingRecipe;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Real menu clicks and deterministic harvests for the video-demo regressions. */
@PrefixGameTestTemplate(false)
public final class VideoDemoTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void machineMenusKeepSlotOrderAndPlayerInventory(GameTestHelper h) {
        var blocks=java.util.List.of(Agristics.EARLY.get(),Agristics.CULTIVATOR.get(),Agristics.ME_INFUSION.get(),
            Agristics.INPUT_PORT.get(),Agristics.OUTPUT_PORT.get(),Agristics.SEEDS.get(),Agristics.NETWORK.get(),Agristics.ESSENCE_VAULT.get());
        int[] counts={11,6,6,16,16,1,0,0};
        int[] inventoryX={8,8,8,8,8,79,79,8};
        int[] inventoryY={136,112,100,116,116,160,160,116};
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var pos=new BlockPos(1,1,1);
        for(int i=0;i<blocks.size();i++) {
            h.setBlock(pos,blocks.get(i));
            io.github.sudabake.appliedagristics.block.MachineEntity machine=h.getBlockEntity(pos);
            var menu=new MachineMenu(1,player.getInventory(),machine);
            h.assertTrue(menu.slots.size()==counts[i]+36,"Machine and player slot counts stay compatible: "+i);
            for(int slot=0;slot<counts[i];slot++)
                h.assertTrue(menu.slots.get(slot).getContainerSlot()==slot,"Machine inventory synchronization keeps slot order: "+i);
            var firstPlayerSlot=menu.slots.get(counts[i]);
            var firstHotbarSlot=menu.slots.get(counts[i]+27);
            h.assertTrue(firstPlayerSlot.container==player.getInventory() && firstPlayerSlot.getContainerSlot()==9
                && firstPlayerSlot.x==inventoryX[i] && firstPlayerSlot.y==inventoryY[i],"Player inventory retains position and backing slots: "+i);
            h.assertTrue(firstHotbarSlot.container==player.getInventory() && firstHotbarSlot.getContainerSlot()==0
                && firstHotbarSlot.x==inventoryX[i] && firstHotbarSlot.y==inventoryY[i]+58,"Hotbar retains position and backing slots: "+i);
        }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void shiftClickNeverMergesIntoPlanterOutputs(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);
        h.setBlock(pos,Agristics.EARLY.get());
        EarlyPlanter planter=h.getBlockEntity(pos);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var menu=new MachineMenu(1,player.getInventory(),planter);
        planter.items.setStackInSlot(2,new ItemStack(Items.WHEAT_SEEDS,5));
        player.getInventory().setItem(9,new ItemStack(Items.WHEAT_SEEDS,10));
        menu.quickMoveStack(player,11);
        h.assertTrue(planter.items.getStackInSlot(0).getCount()==10 && planter.items.getStackInSlot(2).getCount()==5,
            "Shift-click fills the seed input without merging into matching output seeds");
        planter.items.setStackInSlot(0,new ItemStack(Items.WHEAT_SEEDS,64));
        player.getInventory().setItem(9,new ItemStack(Items.WHEAT_SEEDS,10));
        h.assertTrue(menu.quickMoveStack(player,11).isEmpty() && player.getInventory().getItem(9).getCount()==10
            && planter.items.getStackInSlot(2).getCount()==5 && planter.items.getStackInSlot(1).isEmpty(),
            "Full seed input leaves the player stack and outputs unchanged, without using the soil slot");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void manualSeedRemovalPausesButPipeProbesDoNot(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        var seed=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
        f.input().add(seed,8);
        h.runAtTickTime(100,()->{
            var controller=f.controller();
            var id=Cultivations.id(seed.toStack(1),h.getLevel());
            controller.configure(id,true,1234); controller.step();
            var task=controller.tasks.get(id);
            h.assertTrue(task.occupied==1,"Ordinary storage has a live seed reservation");
            f.input().automation(null).extractItem(0,64,true);
            h.assertTrue(task.enabled && task.occupied==1,"Simulated pipe extraction never pauses production");
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            var menu=new MachineMenu(1,player.getInventory(),f.input());
            menu.clicked(0,1,ClickType.PICKUP,player);
            h.assertTrue(!task.enabled && task.occupied==0 && task.target==1234 && menu.getCarried().getCount()==4
                && f.input().count(seed)==4,"Right-click takes half and disables the crop while preserving its target");
            controller.step();
            h.assertTrue(!task.enabled && task.occupied==0,"Remaining seeds do not restart the crop");
            menu.setCarried(ItemStack.EMPTY);
            controller.configure(id,true,1234); controller.step();
            menu.clicked(0,0,ClickType.SWAP,player);
            h.assertTrue(!task.enabled && task.occupied==0 && f.input().count(seed)==0
                && player.getInventory().getItem(0).getCount()==4,"Hotbar removal also releases the reservation exactly once");
            f.input().add(seed,8);
            controller.configure(id,true,1234); controller.step();
            menu.quickMoveStack(player,0);
            h.assertTrue(!task.enabled && task.occupied==0 && f.input().count(seed)==0,"Shift-click also pauses before future production");
            h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void failedShiftRemovalKeepsCropEnabled(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        var seed=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));f.input().add(seed,8);
        h.runAtTickTime(100,()->{
            var id=Cultivations.id(seed.toStack(1),h.getLevel());
            f.controller().configure(id,true,-1);f.controller().step();
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
            var menu=new MachineMenu(1,player.getInventory(),f.input());
            h.assertTrue(menu.quickMoveStack(player,0).isEmpty() && f.controller().tasks.get(id).enabled
                && f.input().count(seed)==8 && f.input().used(seed)==1,"Failed transfer to a full inventory neither pauses nor removes seeds");
            h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void outputOnlyArrayEmitsNewSeeds(GameTestHelper h) { harvestToPort(h,false); }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void meSeedBankWithoutVaultCollectsNewSeeds(GameTestHelper h) { harvestToPort(h,true); }

    private static void harvestToPort(GameTestHelper h,boolean meSeeds) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),meSeeds);
        if(meSeeds) {
            h.getLevel().setBlockAndUpdate(f.vault().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
            f.controller().structure.refresh();f.access().ensureNode();
        }
        var seed=AEItemKey.of(new ItemStack(Items.APPLE));
        var product=AEItemKey.of(new ItemStack(Items.DIAMOND));
        var bank=meSeeds ? (io.github.sudabake.appliedagristics.multiblock.SeedBank)f.seeds() : f.input();
        h.runAtTickTime(100,()->{
            var manager=h.getLevel().getRecipeManager();
            var originals=new ArrayList<>(manager.getRecipes());
            var recipes=new ArrayList<RecipeHolder<?>>(originals);
            var recipe=new PlantingRecipe(Ingredient.of(Items.APPLE),Ingredient.of(Blocks.DIRT),
                List.of(new ItemStack(Items.DIAMOND,2),new ItemStack(Items.APPLE,3)),Blocks.WHEAT.defaultBlockState(),false,200,true);
            recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID,"test/demo_output"),recipe));
            try {
                manager.replaceRecipes(recipes);CropSources.invalidate();
                bank.add(seed,1);
                var controller=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());
                controller.configure(id,true,-1);controller.tasks.get(id).progress=200;controller.step();
                h.assertTrue(bank.count(seed)==(meSeeds?4:1) && f.output().count(seed)==(meSeeds?0:3) && f.output().count(product)==2,
                    "ME banks collect seeds directly; ordinary inputs never refill; harvests reach the output");
                // Force the next batch to block after its primary output fits.
                for(int i=0;i<16;i++)f.output().items.setStackInSlot(i,new ItemStack(Items.DIRT,64));
                f.output().items.setStackInSlot(0,new ItemStack(Items.DIAMOND,62));
                controller.tasks.get(id).progress=200;controller.step();
                h.assertTrue(bank.count(seed)==(meSeeds?7:1) && f.output().count(product)==64,"Seed routing remains independent of output blockage");
                f.output().items.setStackInSlot(1,ItemStack.EMPTY);
                controller.configure(id,false,-1);controller.step();
                h.assertTrue(f.output().count(seed)==(meSeeds?0:3) && bank.count(seed)==(meSeeds?7:1),"Unblocking drains only pending seeds, including after disabling");
                h.succeed();
            } finally {manager.replaceRecipes(originals);CropSources.invalidate();}
        });
    }
}
