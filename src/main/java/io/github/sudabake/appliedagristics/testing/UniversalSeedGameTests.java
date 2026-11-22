package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class UniversalSeedGameTests {
    private static final ResourceLocation COW = new ResourceLocation("minecraft:cow"), PIG = new ResourceLocation("minecraft:pig");
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void aeTransientRecipeTransferKeepsBiologicalIdentity(GameTestHelper h) {
        h.setBlock(0, 1, 1, AEBlocks.CREATIVE_ENERGY_CELL.block()); h.setBlock(1, 1, 1, AEBlocks.DRIVE.block());
        DriveBlockEntity drive = (DriveBlockEntity) h.getBlockEntity(new BlockPos(1, 1, 1));
        drive.getInternalInventory().setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
        // Local test player, no login handshake or unrelated datapack-sync packets.
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "transfer-test");
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        var host = appeng.api.parts.PartHelper.getOrPlacePartHost(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), false, player);
        host.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT), null, player);
        var terminal = host.addPart(appeng.core.definitions.AEParts.CRAFTING_TERMINAL.asItem(), Direction.NORTH, player);
        h.runAtTickTime(100, () -> {
            var menu = new appeng.menu.me.items.CraftingTermMenu(0, player.getInventory(), terminal); player.containerMenu = menu;
            menu.broadcastChanges();
            h.assertTrue(menu.isPowered(), "Test terminal connected to powered network");
            var storage = terminal.getInventory(); var source = IActionSource.ofMachine(terminal);
            var cow = AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW));
            var pig = AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), PIG));
            h.assertTrue(storage.insert(pig, 64, Actionable.MODULATE, source) == 64, "Test network stores wrong species");
            player.getInventory().setItem(0, pig.toStack(8));
            var templates = NonNullList.withSize(9, ItemStack.EMPTY); for (int slot = 0; slot < 3; slot++) templates.set(slot, cow.toStack(1));
            // Round-trip the actual AE packet, preserving its biological component.
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            buffer.writeBoolean(false);
            buffer.writeInt(templates.size());
            for (var template : templates) buffer.writeItem(template);
            buffer.writeBoolean(false);
            var decoded = new appeng.core.sync.packets.FillCraftingGridFromRecipePacket(buffer);
            buffer.release();
            decoded.serverPacketData(player);
            for (int slot = 0; slot < 3; slot++) h.assertTrue(menu.getCraftingMatrix().getStackInSlot(slot).isEmpty(), "No pig essence pulled from network or player for cow recipe");
            h.assertTrue(storage.getAvailableStacks().get(pig) == 64 && player.getInventory().getItem(0).getCount() == 8, "Wrong species stock untouched");
            h.assertTrue(storage.insert(cow, 9, Actionable.MODULATE, source) == 9, "Test network also stores correct species");
            terminal.getGridNode().getGrid().getStorageService().invalidateCache();
            menu.getCraftingMatrix().setItemDirect(0, pig.toStack(1));
            decoded.serverPacketData(player);
            for (int slot = 0; slot < 3; slot++) h.assertTrue(COW.equals(BioData.identity(menu.getCraftingMatrix().getStackInSlot(slot))), "Every filled slot has cow identity: slot=" + slot + " stack=" + menu.getCraftingMatrix().getStackInSlot(slot));
            h.assertTrue(storage.getAvailableStacks().get(cow) == 6 && storage.getAvailableStacks().get(pig) == 65, "Extract exactly three cow essences and return wrong grid item");
            var input = ForgeTestInputs.crafting(3, 1, List.of(menu.getCraftingMatrix().getStackInSlot(0), menu.getCraftingMatrix().getStackInSlot(1), menu.getCraftingMatrix().getStackInSlot(2)));
            var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
            h.assertTrue(recipe.assemble(input, h.getLevel().registryAccess()).is(Items.BEEF), "AE-filled real crafting grid produces selected cow loot");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void lootPatternsAndAECompatibility(GameTestHelper h) {
        var drops = BioLootCatalog.drops(h.getLevel(), COW);
        h.assertTrue(drops.contains(Items.BEEF) && drops.contains(Items.LEATHER) && drops.contains(Items.COOKED_BEEF), "Enumerate raw and conditional cooked loot types");
        h.assertTrue(BioLootCatalog.drops(h.getLevel(), new ResourceLocation("minecraft:wither")).contains(Items.NETHER_STAR), "Include code-generated wither star");
        h.assertTrue(BioLootCatalog.drops(h.getLevel(), new ResourceLocation("minecraft:ender_dragon")).contains(Items.DRAGON_EGG), "Include special dragon egg");
        h.assertTrue(BioLootCatalog.drops(h.getLevel(), new ResourceLocation("minecraft:drowned")).contains(Items.TRIDENT), "Include natural equipment drops outside loot table");
        h.assertTrue(BioLootCatalog.drops(h.getLevel(), new ResourceLocation("minecraft:creeper")).contains(Items.CREEPER_HEAD), "Include charged-creeper head drop");
        var sheep = BioLootCatalog.drops(h.getLevel(), new ResourceLocation("minecraft:sheep"));
        h.assertTrue(sheep.size() > 11 && sheep.contains(Items.BLACK_WOOL) && sheep.contains(Items.WHITE_WOOL), "More than eleven drops and variant tables supported");
        var patterns = new HashSet<Integer>(); for (int i = 0; i < BioPatterns.capacity(); i++) h.assertTrue(patterns.add(BioPatterns.canonical(BioPatterns.at(i))), "No mirrored/translated pattern collisions");
        for (int index = 0; index < sheep.size(); index++) {
            var stacks = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY)); int mask = BioPatterns.at(index);
            for (int i = 0; i < 9; i++) if ((mask & 1 << i) != 0) stacks.set(i, BioData.withIdentity(Agristics.BIO_ESSENCE.get(), new ResourceLocation("minecraft:sheep")));
            var input = ForgeTestInputs.crafting(3, 3, stacks); var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
            var output = recipe.assemble(input, h.getLevel().registryAccess()); h.assertTrue(output.is(sheep.get(index)), "Each shape produces its indexed drop, including overflow");
            var encoded = PatternDetailsHelper.encodeCraftingPattern(recipe, stacks.toArray(ItemStack[]::new), output, false, false);
            var decoded = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
            h.assertTrue(decoded != null && decoded.getPrimaryOutput().what().equals(AEItemKey.of(output)), "AE encoded crafting pattern resolves biological recipe");
        }
        var mixed = new ArrayList<ItemStack>(List.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW), BioData.withIdentity(Agristics.BIO_ESSENCE.get(), PIG), BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW)));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, ForgeTestInputs.crafting(3, 1, mixed), h.getLevel()).isEmpty(), "Mixing species cannot craft loot");
        h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void biologicalNetworkStockIsolation(GameTestHelper h) {
        MultiblockScenarios.biologicalStockIsolation(h);
    }
}
