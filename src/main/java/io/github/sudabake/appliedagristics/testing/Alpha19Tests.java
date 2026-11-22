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
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Alpha19Tests {
    private static final ResourceLocation COW=new ResourceLocation("minecraft:cow");

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=100)
    public static void shiftClickAccelerationThroughGameMode(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "card-test");
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        for (var block : java.util.List.of(Agristics.ME_INFUSION.get(), Agristics.ME_BIO.get())) {
            var pos = new BlockPos(1, 1, 1);
            h.setBlock(pos, block);
            AEProcessingMachine machine = (AEProcessingMachine) h.getBlockEntity(pos);
            var hit = new BlockHitResult(h.absolutePos(pos).getCenter(), Direction.UP, h.absolutePos(pos), false);
            for (var hand : InteractionHand.values()) {
                player.setItemInHand(hand, appeng.core.definitions.AEItems.SPEED_CARD.stack(3));
                for (int click = 0; click < 3; click++) {
                    player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(hand), hand, hit);
                }
            }
            h.assertTrue(machine.accelerationCards() == 4, "Sneaking inserts four cards through the real player interaction path");
            h.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().getCount() == 2, "Survival consumes only the four installed cards, including offhand use");
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            h.setBlock(pos, Blocks.AIR);
            h.setBlock(pos, block);
            machine = (AEProcessingMachine) h.getBlockEntity(pos);
            player.setItemInHand(InteractionHand.MAIN_HAND, appeng.core.definitions.AEItems.SPEED_CARD.stack());
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(machine.accelerationCards() == 1 && player.getMainHandItem().getCount() == 1, "Creative inserts one card without consuming the held card");
            var denied = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, h.absolutePos(pos), hit);
            denied.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            MachineBlock.allowAccelerationInteraction(denied);
            h.assertTrue(denied.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY, "An explicitly denied block interaction stays denied");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            var unrelated = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, h.absolutePos(pos), hit);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(unrelated);
            h.assertTrue(unrelated.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT, "Sneaking with other items keeps the normal interaction rules");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        }
        h.succeed();
    }


    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=100)
    public static void altarCardsAndNoChannel(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.ME_INFUSION.get());MEInfusionAltar altar = (MEInfusionAltar) h.getBlockEntity(pos);altar.ensureNode();
        h.assertTrue(!altar.getMainNode().getNode().hasFlag(appeng.api.networking.GridFlags.REQUIRE_CHANNEL),"ME infusion altar does not consume a channel");
        var player=h.makeMockSurvivalPlayer();var cards=appeng.core.definitions.AEItems.SPEED_CARD.stack(6);player.setItemInHand(InteractionHand.MAIN_HAND,cards);
        var hit=new BlockHitResult(h.absolutePos(pos).getCenter(),Direction.UP,h.absolutePos(pos),false);
        for(int i=0;i<6;i++) altar.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(cards.getCount()==2 && altar.accelerationCards()==4,"Right click fills four independent slots and never consumes a fifth card");
        for(int i=2;i<6;i++) h.assertTrue(altar.items.getStackInSlot(i).getCount()==1,"Each acceleration slot holds exactly one card");h.succeed();
    }

}
