package io.github.sudabake.appliedagristics.block;

import appeng.util.InteractionUtil;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Uses AE's wrench recognition and the agreed per-block dismantling rules. */
@EventBusSubscriber(modid=Agristics.ID)
public final class MachineWrench {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void wrench(PlayerInteractEvent.RightClickBlock event) {
        var level=event.getLevel(); var player=event.getEntity(); var stack=event.getItemStack(); var pos=event.getPos(); var state=level.getBlockState(pos);
        if (player.isSpectator() || event.getHand()!=InteractionHand.MAIN_HAND || !InteractionUtil.canWrenchDisassemble(stack)
            || !(state.getBlock() instanceof MachineBlock || state.getBlock() instanceof PlanterPartBlock)) return;
        if (!level.mayInteract(player,pos) || !player.mayBuild()) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!(level instanceof ServerLevel server)) return;
        if (player.isShiftKeyDown()) {
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled()) return;
            var drops=Block.getDrops(state,server,pos,level.getBlockEntity(pos),player,stack);
            if (!level.removeBlock(pos,false)) return;
            server.playSound(null,pos,SoundEvents.ITEM_FRAME_REMOVE_ITEM,SoundSource.BLOCKS,.7f,1f);
            for (var drop:drops) { player.getInventory().add(drop); if (!drop.isEmpty()) player.drop(drop,false); }
        } else if (level.getBlockEntity(pos) instanceof IOControls io) {
            int side=event.getHitVec().getDirection().ordinal(); int next=(io.sideMode(side)+1)%4;
            io.configureSide(side,next);
            if (io.sideMode(side)!=next) return;
            server.playSound(null,pos,SoundEvents.ITEM_FRAME_ROTATE_ITEM,SoundSource.BLOCKS,.7f,1f);
            player.displayClientMessage(Component.translatable("gui.appliedagristics.side_"+side).append(": ").append(Component.translatable("gui.appliedagristics.io_"+next)),true);
        } else if (state.hasProperty(PlanterPartBlock.FACING) && !state.getValue(PlanterPartBlock.FORMED)) {
            PlanterStructure.changed(level,pos);
            if (level.setBlockAndUpdate(pos,state.setValue(PlanterPartBlock.FACING,state.getValue(PlanterPartBlock.FACING).getClockWise())))
                server.playSound(null,pos,SoundEvents.ITEM_FRAME_ROTATE_ITEM,SoundSource.BLOCKS,.7f,1f);
        }
    }
    private MachineWrench() {}
}
