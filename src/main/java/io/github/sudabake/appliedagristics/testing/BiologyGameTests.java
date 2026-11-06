package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class BiologyGameTests {
    private static final ResourceLocation COW = ResourceLocation.parse("minecraft:cow");
    private static final ResourceLocation PIG = ResourceLocation.parse("minecraft:pig");

    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void sampleIdentityAndBosses(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL); var cow = EntityType.COW.create(h.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Agristics.SAMPLER.get(), 2));
        float health = cow.getHealth();
        var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow); NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled() && cow.getHealth() == health, "Sampling intercepts native interaction without damaging mob");
        h.assertTrue(player.getMainHandItem().getCount() == 1 && BioData.identity(player.getMainHandItem()) == null, "Only one swab from stack is labeled");
        ItemStack sample = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (COW.equals(BioData.identity(player.getInventory().getItem(i)))) sample = player.getInventory().getItem(i).copy();
        h.assertTrue(sample.getCount() == 1, "Exactly one cow sample is produced");
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)sample.save(h.getLevel().registryAccess()));
        h.assertTrue(COW.equals(BioData.identity(restored)), "Identity survives disk serialization");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(buffer, restored); var decoded = ItemStack.STREAM_CODEC.decode(buffer); buffer.release();
        h.assertTrue(ItemStack.isSameItemSameComponents(restored, decoded), "Identity survives inventory network synchronization");
        h.assertTrue(!ItemStack.isSameItemSameComponents(sample, BioData.withIdentity(Agristics.SAMPLER.get(), PIG)), "Different mobs cannot stack together");
        for(var described:java.util.List.of(sample,BioData.withIdentity(Agristics.BIO_SEED.get(),COW))) {
            var normal=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            var advanced=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            described.getItem().appendHoverText(described,Item.TooltipContext.of(h.getLevel()),normal,TooltipFlag.NORMAL);
            described.getItem().appendHoverText(described,Item.TooltipContext.of(h.getLevel()),advanced,TooltipFlag.ADVANCED);
            h.assertTrue(normal.stream().noneMatch(line->line.getString().contains(COW.toString()))
                && advanced.stream().anyMatch(line->line.getString().contains(COW.toString())),
                "Species IDs appear only in advanced tooltips without changing sampled item data");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, sample);
        BioData.sample(player, InteractionHand.MAIN_HAND, EntityType.PIG.create(h.getLevel()));
        h.assertTrue(COW.equals(BioData.identity(player.getMainHandItem())), "An existing sample cannot be relabeled");
        for (var type : java.util.List.of(EntityType.WITHER, EntityType.ENDER_DRAGON)) {
            var boss = type.create(h.getLevel()); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Agristics.SAMPLER.get()));
            BioData.sample(player, InteractionHand.MAIN_HAND, boss);
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(BioData.identity(player.getMainHandItem())), "Boss is directly sampleable: " + type);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Agristics.SAMPLER.get()));
        var dragon = EntityType.ENDER_DRAGON.create(h.getLevel()); BioData.sample(player, InteractionHand.MAIN_HAND, dragon.getSubEntities()[0]);
        h.assertTrue(ResourceLocation.parse("minecraft:ender_dragon").equals(BioData.identity(player.getMainHandItem())), "Dragon body part resolves parent identity");
        h.succeed();
    }


}
