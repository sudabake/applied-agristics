package io.github.sudabake.appliedagristics.network;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Messages {
    private static final String PROTOCOL = "forge-1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Agristics.ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Messages() {}

    public record Control(BlockPos pos, int containerId, long sessionToken, int action, String id, long value, ItemStack seed) {
        public static final int MAX_ID_LENGTH = 32767;

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(containerId);
            buf.writeLong(sessionToken);
            buf.writeVarInt(action);
            buf.writeUtf(id, MAX_ID_LENGTH);
            buf.writeVarLong(value);
            buf.writeItem(seed);
        }

        public static Control read(FriendlyByteBuf buf) {
            return new Control(buf.readBlockPos(), buf.readVarInt(), buf.readLong(),
                buf.readVarInt(), buf.readUtf(MAX_ID_LENGTH), buf.readVarLong(), buf.readItem());
        }
    }

    public record Snapshot(BlockPos pos, int containerId, long sessionToken, CompoundTag data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(containerId);
            buf.writeLong(sessionToken);
            buf.writeNbt(data);
        }

        public static Snapshot read(FriendlyByteBuf buf) {
            return new Snapshot(buf.readBlockPos(), buf.readVarInt(), buf.readLong(), buf.readNbt());
        }
    }
    public record BiologicalCatalog(CompoundTag data) {}

    public static void register() {
        CHANNEL.messageBuilder(Control.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Control::write).decoder(Control::read)
            .consumerMainThread((p, context) -> control(p, context.get().getSender())).add();
        CHANNEL.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Snapshot::write).decoder(Snapshot::read)
            .consumerMainThread((p, ctx) -> {
                var player = Clientbound.player();
                if (player != null && player.containerMenu instanceof MachineMenu menu
                    && menu.matches(p.pos, p.containerId, p.sessionToken) && p.data != null) menu.state = p.data;
            }).add();
        CHANNEL.messageBuilder(BiologicalCatalog.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p, buf) -> buf.writeNbt(p.data)).decoder(buf -> new BiologicalCatalog(buf.readNbt()))
            .consumerMainThread((p, ctx) -> {
                if (p.data != null) io.github.sudabake.appliedagristics.biology.BioLootCatalog.loadClient(p.data);
            }).add();
    }

    public static void sendToServer(Control message) { CHANNEL.sendToServer(message); }
    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static final class Clientbound {
        private static net.minecraft.world.entity.player.Player player() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }

    private static void control(Control p, ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof MachineMenu menu)
            || !menu.matches(p.pos, p.containerId, p.sessionToken) || !menu.stillValid(player)) return;
        var machine = menu.machine;
        switch (p.action) {
            case 0 -> { if (machine instanceof SeedStorage drive) drive.collect(player); }
            case 1, 2 -> { if (machine instanceof NetworkPlanter planter) { var id = ResourceLocation.tryParse(p.id); if (id != null) planter.configure(id, p.action == 1, p.value); } }
            case 3 -> {
                if (machine instanceof SeedStorage drive && !p.seed.isEmpty()) {
                    var key = appeng.api.stacks.AEItemKey.of(p.seed); if (drive.count(key) > 0) drive.withdraw(key, player);
                }
            }
            case 4 -> { menu.page = (int) Math.max(0, Math.min(100000, p.value)); }
            case 5, 6 -> { if (machine instanceof NetworkPlanter planter) planter.configureAll(p.action == 5); }
            case 7 -> { if (machine instanceof IOControls io && p.value >= 0 && p.value < 6) io.configureSide((int)p.value,(io.sideMode((int)p.value)&2)==0 ? 3 : 1); }
            case 8 -> { if (machine instanceof ItemPort port) port.toggleAutoOutput(); else if (machine instanceof IOControls io) io.toggleAutoOutput(); }
            case 12 -> { if (machine instanceof NetworkPlanter) { menu.search="hide_ordinary".equals(menu.search)?"":"hide_ordinary";menu.page=0; player.getPersistentData().putBoolean("appliedagristics.hideOrdinary","hide_ordinary".equals(menu.search)); } }
            case 13 -> { if (machine instanceof EssenceVault vault && p.value>=Integer.MIN_VALUE && p.value<=Integer.MAX_VALUE) vault.setPriority((int)p.value); }
            case 14 -> { if(machine instanceof NetworkPlanter planter) planter.toggleAutoEnableNewSeeds(); }
            case 15 -> { if(machine instanceof SeedStorage drive) drive.toggleAutoCollect(); }
            case 16 -> { if(machine instanceof SeedStorage drive && p.value>=0 && p.value<4) drive.setCollectFilter((int)p.value); }
            case 17 -> { if(machine instanceof SeedStorage drive) drive.ejectSeeds(); }
            default -> { return; }
        }
        menu.sync(player);
    }
}
