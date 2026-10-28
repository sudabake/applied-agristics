package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.PlanterPartBlock;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@PrefixGameTestTemplate(false)
public final class InteractionRevisionTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void topLayerPortsAndModulesBindToRealArray(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        var level=h.getLevel();var center=f.controller().getBlockPos();var origin=center.offset(-1,-1,0);
        for(var pos:List.of(center.east(),center.west().below(),center.below()))level.setBlockAndUpdate(pos,Agristics.PLANTER_FRAME.get().defaultBlockState());
        var access=origin.offset(2,3,0);var input=origin.offset(0,3,0);var output=origin.offset(1,3,1);
        level.setBlockAndUpdate(access,Agristics.NETWORK_ACCESS.get().defaultBlockState());
        level.setBlockAndUpdate(input,Agristics.INPUT_PORT.get().defaultBlockState());
        level.setBlockAndUpdate(output,Agristics.OUTPUT_PORT.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(0,2,0),Agristics.SPEED_MODULE.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(0,3,2),Agristics.PARALLEL_MODULE.get().defaultBlockState());
        var structure=f.controller().structure;structure.refresh();
        h.assertTrue(structure.formed(),"Upper shell accepts relocated required ports and upgrades");
        h.assertTrue(structure.speedModules()==1 && structure.parallelModules()==1,"Upper modules contribute to production");
        h.assertTrue(structure.access()==level.getBlockEntity(access),"Upper network access binds to controller");
        h.assertTrue(structure.ports().contains(level.getBlockEntity(input)) && structure.ports().contains(level.getBlockEntity(output)),"Roof input/output ports bind to controller");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void passivePartsPassInteractionBeforeAndAfterFormation(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=player.containerMenu;
        var relative=new BlockPos(2,1,2);var pos=h.absolutePos(relative);
        var hit=new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false);
        for (var block:List.of(Agristics.PLANTER_FRAME.get(),Agristics.CULTIVATION_CHAMBER.get(),Agristics.SPEED_MODULE.get(),Agristics.PARALLEL_MODULE.get(),Agristics.CONVERSION_MODULE.get(),Agristics.NETWORK_ACCESS.get())) {
            for (boolean formed:new boolean[]{false,true}) {
                var state=block.defaultBlockState().setValue(PlanterPartBlock.FORMED,formed);h.setBlock(relative,state);
                h.assertTrue(state.useWithoutItem(h.getLevel(),player,hit)==InteractionResult.PASS,"Passive part passes empty-hand interaction: "+block+" formed="+formed);
                h.assertTrue(state.useItemOn(new ItemStack(Blocks.DIRT),h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"Passive part does not consume the held block's placement action");
                h.assertTrue(player.containerMenu==menu,"Passive part never opens a machine menu");
            }
        }
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        h.assertTrue(f.controller().structure.formed(),"Real array forms");
        var origin=f.controller().getBlockPos().offset(-1,-1,0);
        for (int y=0;y<4;y++) for (int z=0;z<3;z++) for (int x=0;x<3;x++) {
            var member=origin.offset(x,y,z);var state=h.getLevel().getBlockState(member);
            if (state.getBlock() instanceof PlanterPartBlock || state.is(Agristics.NETWORK_ACCESS.get())) {
                var memberHit=new BlockHitResult(member.getCenter(),Direction.NORTH,member,false);
                h.assertTrue(state.useWithoutItem(h.getLevel(),player,memberHit)==InteractionResult.PASS && player.containerMenu==menu,"Real formed casing does not redirect to the controller");
            }
        }
        h.succeed();
    }
}
