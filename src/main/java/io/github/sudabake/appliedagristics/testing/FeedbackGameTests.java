package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Balance;
import io.github.sudabake.appliedagristics.block.EarlyPlanter;
import io.github.sudabake.appliedagristics.block.NetworkPlanter;
import io.github.sudabake.appliedagristics.multiblock.ArrayConstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class FeedbackGameTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void legacyConfigMigratesWithoutLosingValues(GameTestHelper helper) {
        var config=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        config.set("earlyCycleTicks",340);
        config.set("networkCycleTicks",440);
        config.set("agriculturalArray",config.createSubConfig());
        config.set("agriculturalArray.networkCycleTicks",600);
        config.set("cacheNetworkLoot",false);
        Balance.SPEC.correct(config);
        helper.assertTrue(config.getInt("basicPlanter.earlyCycleTicks")==340
                && config.getInt("agriculturalArray.networkCycleTicks")==600
                && Boolean.FALSE.equals(config.get("performance.cacheNetworkLoot"))
                && Long.valueOf(256L).equals(config.get("storage.meSeedsPerType"))
                && Long.valueOf(-1L).equals(config.get("storage.cropVaultCapacity"))
                && !config.contains("earlyCycleTicks"),"Categorized settings preserve legacy edits and prefer explicit grouped overrides");
        helper.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void buildMinimumWithMaterials(GameTestHelper helper) {
        var pos=new BlockPos(2,2,2);
        helper.setBlock(pos,Agristics.NETWORK.get());
        NetworkPlanter controller=(NetworkPlanter)helper.getBlockEntity(pos);
        var player=player(helper,false);
        player.setPos(controller.getBlockPos().getCenter().add(0,0,-2));
        var bag=player.getInventory();
        bag.add(new ItemStack(Agristics.PLANTER_FRAME.get(),30));
        bag.add(new ItemStack(Agristics.CULTIVATION_CHAMBER.get(),2));
        bag.add(new ItemStack(Agristics.NETWORK_ACCESS.get()));
        bag.add(new ItemStack(Agristics.INPUT_PORT.get()));
        ArrayConstruction.build(controller,player);
        helper.assertTrue(!controller.structure.formed() && bag.countItem(Agristics.PLANTER_FRAME.get().asItem())==30,
                "Missing output leaves the structure and stock unchanged");
        bag.add(new ItemStack(Agristics.OUTPUT_PORT.get()));
        ArrayConstruction.build(controller,player);
        helper.assertTrue(controller.structure.formed() && controller.structure.speedModules()==0
                && bag.countItem(Agristics.PLANTER_FRAME.get().asItem())==0,"Survival forms the minimum build and consumes resources");
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void creativeBuildHasAllUpgrades(GameTestHelper helper) {
        var pos=new BlockPos(2,2,2);
        helper.setBlock(pos,Agristics.NETWORK.get());
        NetworkPlanter controller=(NetworkPlanter)helper.getBlockEntity(pos);
        var player=player(helper,true);
        player.setPos(controller.getBlockPos().getCenter().add(0,0,-2));
        ArrayConstruction.build(controller,player);
        helper.assertTrue(controller.structure.formed() && controller.structure.speedModules()==4
                && controller.structure.parallelModules()==4 && controller.structure.conversion()
                && controller.structure.vaults().size()==1 && controller.structure.seeds().size()==1,
                "Creative uses the full upgraded example");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, boolean creative) {
        var level=helper.getLevel();
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Array Test");
        return new ConstructionPlayer(level,profile,creative);
    }

    private static final class ConstructionPlayer extends ServerPlayer {
        private final boolean creative;

        private ConstructionPlayer(net.minecraft.server.level.ServerLevel level,
                com.mojang.authlib.GameProfile profile, boolean creative) {
            super(level.getServer(),level,profile);
            this.creative=creative;
        }

        @Override public boolean isCreative() { return creative; }
        @Override public void sendSystemMessage(Component message) {}
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void basicPlanterShowsCompletionWhenHarvestAppears(GameTestHelper helper) {
        var pos=new BlockPos(2,2,2);
        helper.setBlock(pos,Agristics.EARLY.get());
        EarlyPlanter planter=(EarlyPlanter)helper.getBlockEntity(pos);
        // Use the fixed sugar-cane recipe so the test does not depend on field lighting or loot rolls.
        planter.items.setStackInSlot(0,new ItemStack(Items.SUGAR_CANE));
        planter.items.setStackInSlot(1,new ItemStack(Blocks.FARMLAND));
        int earlySteps=(planter.snapshot(0,"").getInt("baseTicks")+19)/20;
        for (long i=1;i<earlySteps;i++) planter.step();
        helper.assertTrue(planter.snapshot(0,"").getInt("progress")<1000 && planter.items.getStackInSlot(2).isEmpty(),
                "The final pre-harvest state has no harvest");
        planter.step();
        helper.assertTrue(planter.harvestFlash() && planter.snapshot(0,"").getInt("progress")==1000
                && planter.items.getStackInSlot(2).is(Items.SUGAR_CANE),"The completed visual accompanies committed output");
        helper.succeed();
    }
}
