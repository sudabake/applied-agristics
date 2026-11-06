package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class NextPhaseTests {
    private static final ResourceLocation COW=ResourceLocation.parse("minecraft:cow");

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void realPortPlacementUsesPlayerView(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var placed=new ArrayList<BlockPos>();
        for(int index=0;index<3;index++) {
            var block=List.of(Agristics.NETWORK_ACCESS.get(),Agristics.INPUT_PORT.get(),Agristics.OUTPUT_PORT.get()).get(index);
            for (float pitch : new float[]{75,-75,65,-65,0}) {
                var support=h.absolutePos(new BlockPos(1+index*2,1,2));
                var pos=support.south();
                h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
                player.setPos(support.getX()+.5,support.getY()+2,support.getZ()+2);
                player.setXRot(pitch);player.setYRot(180);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(block));
                // Deliberately click a side wall even when looking down/up.
                var hit=new net.minecraft.world.phys.BlockHitResult(support.getCenter().add(0,0,.5),Direction.SOUTH,support,false);
                var result=player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
                var state=h.getLevel().getBlockState(pos);
                var expected=pitch>65 ? Direction.UP : pitch<-65 ? Direction.DOWN : Direction.SOUTH;
                h.assertTrue(result.consumesAction() && state.is(block),"Real item-use places the directional port");
                h.assertTrue(PlanterOrientation.front(state)==expected,"Port follows the AE view threshold independently of clicked face");
            }
            var pos=h.absolutePos(new BlockPos(1+index*2,1,3));
            h.getLevel().setBlockAndUpdate(pos,PlanterOrientation.withFacing(block.defaultBlockState(),Direction.UP));
            placed.add(pos);
        }
        h.runAtTickTime(30,()-> {
            for(var pos:placed) h.assertTrue(PlanterOrientation.front(h.getLevel().getBlockState(pos))==Direction.UP,"Unformed port keeps its upward facing after neighbor and server ticks");
            h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void ordinarySeedsIgnoreConversionAndContinueCollecting(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);f.modules(0,1,true);
        var seed=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));var product=AEItemKey.of(new ItemStack(Items.WHEAT));f.seeds().add(seed,1);
        h.runAtTickTime(100,()-> {
            var manager=h.getLevel().getRecipeManager();var originals=new ArrayList<>(manager.getRecipes());var recipes=new ArrayList<RecipeHolder<?>>(originals);
            recipes.add(new RecipeHolder<>(ResourceLocation.parse("appliedagristics:test/ordinary_conversion"),new PlantingRecipe(Ingredient.of(Items.WHEAT_SEEDS),Ingredient.of(Blocks.DIRT),List.of(new ItemStack(Items.WHEAT,2),new ItemStack(Items.WHEAT_SEEDS,3)),Blocks.WHEAT.defaultBlockState(),false,200,true)));
            manager.replaceRecipes(recipes);CropSources.invalidate();
            try {
                var a=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());a.configure(id,true,-1);var task=a.tasks.get(id);
                task.progress=200;a.step();h.assertTrue(f.seeds().count(seed)==4 && f.producedCount(product)==2,"Ordinary seeds and produce stay separate with conversion installed");
                f.seeds().add(seed,10);task.progress=400;a.step();
                h.assertTrue(f.seeds().count(seed)==20 && f.producedCount(product)==6,"All surplus ordinary seeds keep collecting after parallel is full");h.succeed();
            } finally { manager.replaceRecipes(originals);CropSources.invalidate(); }
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void biologicalGroundHarvestAndSeedTag(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,Agristics.BIO_CROP.get().defaultBlockState().setValue(BlockStateProperties.AGE_7,7));
        var plant=(BioCropEntity)h.getLevel().getBlockEntity(pos);plant.setIdentity(COW);
        double old=Balance.FALLBACK_SEED_CHANCE.get();
        try {
            Balance.FALLBACK_SEED_CHANCE.set(0.0);var state=h.getLevel().getBlockState(pos);
            var drops=Block.getDrops(state,h.getLevel(),pos,plant);
            h.assertTrue(count(drops,Agristics.BIO_SEED.get())==1 && count(drops,Agristics.BIO_ESSENCE.get())==1,"Mature crop returns one retained seed and one essence at zero bonus chance");
            Balance.FALLBACK_SEED_CHANCE.set(1.0);drops=Block.getDrops(state,h.getLevel(),pos,plant);
            h.assertTrue(count(drops,Agristics.BIO_SEED.get())==2,"Configured probability one adds exactly one new seed");
            drops=Block.getDrops(state.setValue(BlockStateProperties.AGE_7,0),h.getLevel(),pos,plant);
            h.assertTrue(count(drops,Agristics.BIO_ESSENCE.get())==0 && count(drops,Agristics.BIO_SEED.get())==1,"Immature plants never grant essence or bonus seeds");
            var seed=BioData.withIdentity(Agristics.BIO_SEED.get(),COW);
            h.assertTrue(seed.is(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:seeds"))),"Biological seed belongs to the common seed tag");
            h.assertTrue(plant.identity().equals(COW),"Harvest inspection does not alter stored species");h.succeed();
        } finally { Balance.FALLBACK_SEED_CHANCE.set(old); }
    }
    private static int count(List<ItemStack> drops,Item item) { return drops.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum(); }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void productionMatrixUsesOccupiedSeedsAndRetainedWork(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        var seed=AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),COW));var output=AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),COW));f.seeds().add(seed,256);
        h.runAtTickTime(100,()-> {
            double chance=Balance.FALLBACK_SEED_CHANCE.get();Balance.FALLBACK_SEED_CHANCE.set(0.0);
            try {
                var a=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());
                for(int speed=0;speed<=4;speed++) for(int parallel=0;parallel<=4;parallel++) {
                    a.configure(id,false,-1);f.modules(speed,parallel,false);a.configure(id,true,-1);var task=a.tasks.get(id);task.progress=0;
                    long before=f.producedCount(output),started=System.nanoTime();
                    for(int step=0;step<240;step++) a.step();
                    long produced=f.producedCount(output)-before;
                    double expected=240.0*5*ProductionMath.moduleParallel(parallel,Balance.PARALLEL_MAX.get())*ProductionMath.moduleSpeed(speed,Balance.SPEED_MAX.get())/Balance.LATE_TICKS.get();
                    h.assertTrue(Math.abs(produced-expected)<=1.00001,"Observed production follows occupied seeds and speed within one retained fractional harvest");
                    com.mojang.logging.LogUtils.getLogger().info("YIELD_PROFILE speedModules={} parallelModules={} simulatedTicks=1200 produced={} expected={} elapsedNs={}",speed,parallel,produced,expected,System.nanoTime()-started);
                }
                h.succeed();
            } finally { Balance.FALLBACK_SEED_CHANCE.set(chance); }
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void outputPortBudgetRemainsSixtyFourHarvestsPerStep(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);f.modules(4,4,false);
        var stack=BioData.withIdentity(Agristics.BIO_SEED.get(),COW);for(int slot=0;slot<4;slot++) f.input().items.setStackInSlot(slot,stack.copyWithCount(64));
        h.runAtTickTime(100,()-> {
            double chance=Balance.FALLBACK_SEED_CHANCE.get();Balance.FALLBACK_SEED_CHANCE.set(0.0);
            try {
                var a=f.controller();a.configure(Cultivations.id(stack,h.getLevel()),true,-1);var output=AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),COW));long produced=0;
                for(int step=0;step<240;step++) {
                    a.step();
                    for(int slot=0;slot<f.output().items.getSlots();slot++) if(output.matches(f.output().items.getStackInSlot(slot)))
                        produced+=f.output().items.extractItem(slot,Integer.MAX_VALUE,false).getCount();
                }
                h.assertTrue(produced==15360,"Output-port-only production respects 64 harvests per five-tick step without artificial storage blockage");
                com.mojang.logging.LogUtils.getLogger().info("YIELD_PORT_PROFILE simulatedTicks=1200 produced={} expected=15360",produced);h.succeed();
            } finally { Balance.FALLBACK_SEED_CHANCE.set(chance); }
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void portDirectionsRetainOldHorizontalValues(GameTestHelper h) {
        for(var block:List.of(Agristics.NETWORK_ACCESS.get(),Agristics.INPUT_PORT.get(),Agristics.OUTPUT_PORT.get())) {
            var state=block.defaultBlockState();h.assertTrue(PlanterOrientation.property(state).getPossibleValues().size()==6,"Only directional ports expand to six faces");
            for(var direction:Direction.values()) {
                var oriented=PlanterOrientation.withFacing(state,direction);
                h.assertTrue(PlanterOrientation.facing(oriented)==direction && PlanterOrientation.front(oriented)==direction,"Unformed functional face follows all six saved directions");
            }
            var visited=EnumSet.noneOf(Direction.class);
            for(int i=0;i<6;i++) { visited.add(PlanterOrientation.facing(state));state=PlanterOrientation.withFacing(state,PlanterOrientation.next(state)); }
            h.assertTrue(visited.size()==6,"Wrench cycle visits six different faces");
        }
        h.assertTrue(PlanterOrientation.property(Agristics.NETWORK.get().defaultBlockState()).getPossibleValues().size()==4,"Controller keeps horizontal layout semantics");
        h.assertTrue(PlanterOrientation.property(Agristics.SEEDS.get().defaultBlockState()).getPossibleValues().size()==4,"ME seed storage stays horizontal");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void topOutputFaceMatchesRealExtractionSide(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);var pos=f.controller().getBlockPos().offset(0,2,1);
        h.getLevel().setBlockAndUpdate(pos,PlanterOrientation.withFacing(Agristics.OUTPUT_PORT.get().defaultBlockState(),Direction.DOWN));f.controller().structure.refresh();
        var port=(ItemPort)h.getLevel().getBlockEntity(pos);port.items.setStackInSlot(0,new ItemStack(Items.WHEAT,4));
        h.assertTrue(port.outputFace()==Direction.UP && PlanterOrientation.front(port.getBlockState())==Direction.UP,"Top port exposes the same side in its model and I/O");
        h.assertTrue(port.automation(Direction.UP).extractItem(0,1,true).getCount()==1 && port.automation(Direction.DOWN).extractItem(0,1,true).isEmpty(),"Only the real outward face allows output extraction");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void onlineRowsDoNotRetainOfflineStatus(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);var seed=AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),COW));f.seeds().add(seed,1);
        h.runAtTickTime(100,()-> {
            var a=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());var task=new NetworkPlanter.Task();task.status="offline";a.tasks.put(id,task);
            var row=a.snapshot(0,"").getList("rows",10).getCompound(0);h.assertTrue(row.getString("status").equals("disabled"),"Disabled crops show disabled despite a stale execution status");
            task.enabled=true;row=a.snapshot(0,"").getList("rows",10).getCompound(0);
            h.assertTrue(row.getString("status").equals("waiting") && task.occupied==0 && f.seeds().count(seed)==1 && task.target==-1,"Online waiting status is read-only and never claims seeds");h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void jdteRetainsSpeciesAndOnlyOutputsNetSeeds(GameTestHelper h) {
        if(!net.neoforged.fml.ModList.get().isLoaded("jdte")) { h.succeed();return; }
        double old=Balance.FALLBACK_SEED_CHANCE.get();
        try {
            var resolver=Class.forName("com.jdte.common.recipes.GreenhouseCropResolver");
            for(var id:List.of(COW,ResourceLocation.parse("minecraft:chicken"))) {
                var seed=BioData.withIdentity(Agristics.BIO_SEED.get(),id);var definition=resolver.getMethod("findGeneric",ItemStack.class).invoke(null,seed);
                var generate=definition.getClass().getMethod("generateHarvest",net.minecraft.server.level.ServerLevel.class,BlockPos.class,ItemStack.class);
                Balance.FALLBACK_SEED_CHANCE.set(0.0);
                @SuppressWarnings("unchecked") var result=(List<ItemStack>)generate.invoke(definition,h.getLevel(),h.absolutePos(BlockPos.ZERO),ItemStack.EMPTY);
                h.assertTrue(count(result,Agristics.BIO_ESSENCE.get())==1 && count(result,Agristics.BIO_SEED.get())==0 && result.stream().allMatch(s->id.equals(BioData.identity(s))),"JDTE produces the matching essence without copying its retained seed");
                Balance.FALLBACK_SEED_CHANCE.set(1.0);
                @SuppressWarnings("unchecked") var bonus=(List<ItemStack>)generate.invoke(definition,h.getLevel(),h.absolutePos(BlockPos.ZERO),ItemStack.EMPTY);
                h.assertTrue(count(bonus,Agristics.BIO_SEED.get())==1 && bonus.stream().allMatch(s->id.equals(BioData.identity(s))),"JDTE adds exactly the configured new seed and preserves species");
            }
            h.succeed();
        } catch(ReflectiveOperationException exception) { throw new IllegalStateException("JDTE integration test failed",exception); }
        finally { Balance.FALLBACK_SEED_CHANCE.set(old); }
    }
}
