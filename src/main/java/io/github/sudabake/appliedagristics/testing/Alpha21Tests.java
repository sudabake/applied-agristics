package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.BiologicalAltar;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Alpha21Tests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void altarDirectInteractionNeverOpensGui(GameTestHelper h) {
        var local=new BlockPos(3,1,3);h.setBlock(local,Agristics.BIO_ALTAR.get());BiologicalAltar altar=h.getBlockEntity(local);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(altar.getBlockPos().getCenter());var menu=player.containerMenu;
        var seed=new ItemStack(Agristics.EMPTY_BIO_SEED.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,seed);
        var hit=new BlockHitResult(altar.getBlockPos().getCenter(),Direction.UP,altar.getBlockPos(),false);
        altar.getBlockState().useItemOn(seed,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(seed.getCount()==2 && player.containerMenu==menu,"Missing materials refuse the seed without opening any screen");
        var cow=ResourceLocation.parse("minecraft:cow");
        for(var stack:java.util.List.of(BioData.withIdentity(Agristics.EXTRACT.get(),cow),new ItemStack(Agristics.CONDENSED_ESSENCE.get())))
            h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(),altar.getBlockPos().getX()+1.5,altar.getBlockPos().getY()+.2,altar.getBlockPos().getZ()+.5,stack));
        altar.getBlockState().useItemOn(seed,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(seed.getCount()==1 && altar.visualItem().is(Agristics.EMPTY_BIO_SEED.get()),"Right-click inserts one seed and displays it above the altar");
        altar.getBlockState().useItemOn(seed,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(seed.getCount()==1 && player.containerMenu==menu,"Repeated clicks during processing do not consume another seed or open a menu");
        for(int i=0;i<120;i++)altar.step();
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        altar.getBlockState().useWithoutItem(h.getLevel(),player,hit);
        h.assertTrue(altar.items.getStackInSlot(1).isEmpty() && player.containerMenu==menu,"Empty-hand right-click takes the output without a screen");
        var results=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(altar.getBlockPos()).inflate(2),e->e.getItem().is(Agristics.BIO_SEED.get()));
        h.assertTrue(results.size()==1 && results.getFirst().getItem().getCount()==1 && cow.equals(BioData.identity(results.getFirst().getItem())),"Exactly one identified seed is returned at the player");
        h.assertTrue(altar.createMenu(1,player.getInventory(),player)==null,"The altar cannot create a container through another entrypoint");
        h.succeed();
    }
}
