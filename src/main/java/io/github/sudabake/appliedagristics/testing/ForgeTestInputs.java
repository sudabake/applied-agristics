package io.github.sudabake.appliedagristics.testing;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Adapts the fixtures to the 1.20.1 crafting grid and mock-player APIs. */
public final class ForgeTestInputs {
    private ForgeTestInputs() {}

    public static TransientCraftingContainer crafting(int width, int height, List<ItemStack> items) {
        var contents = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) contents.set(slot, items.get(slot));
        return new TransientCraftingContainer(null, width, height, contents);
    }

    static Player mockPlayer(GameTestHelper helper, GameType type) {
        var level = helper.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "agr-test-player");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.gameMode.changeGameModeForPlayer(type);
        return player;
    }

    static ItemStack processingPattern(List<GenericStack> inputs, List<GenericStack> outputs) {
        return PatternDetailsHelper.encodeProcessingPattern(inputs.toArray(GenericStack[]::new),
            outputs.toArray(GenericStack[]::new));
    }
}
