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

}
