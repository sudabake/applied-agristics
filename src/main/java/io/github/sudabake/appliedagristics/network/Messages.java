package io.github.sudabake.appliedagristics.network;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Messages {
    private Messages() {}
    public record Control(BlockPos pos, int action, String id, long value, ItemStack seed) implements CustomPacketPayload {
        // Keep persisted variant IDs intact and use the standard ResourceLocation UTF bound.
        public static final int MAX_ID_LENGTH = FriendlyByteBuf.MAX_STRING_LENGTH;
        public static final Type<Control> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID, "control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = StreamCodec.of(
            (buffer, p) -> { buffer.writeBlockPos(p.pos); buffer.writeVarInt(p.action); buffer.writeUtf(p.id, MAX_ID_LENGTH); buffer.writeVarLong(p.value); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, p.seed); },
            buffer -> new Control(buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf(MAX_ID_LENGTH), buffer.readVarLong(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));
        @Override public Type<Control> type() { return TYPE; }
    }
    public record Snapshot(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID, "snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
            (buffer, p) -> { buffer.writeBlockPos(p.pos); buffer.writeNbt(p.data); },
            buffer -> new Snapshot(buffer.readBlockPos(), buffer.readNbt()));
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    public record BiologicalCatalog(CompoundTag data) implements CustomPacketPayload {
        public static final Type<BiologicalCatalog> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID, "biological_catalog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BiologicalCatalog> CODEC = StreamCodec.of((buffer, p) -> buffer.writeNbt(p.data), buffer -> new BiologicalCatalog(buffer.readNbt()));
        @Override public Type<BiologicalCatalog> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("5");
        registrar.playToClient(BiologicalCatalog.TYPE, BiologicalCatalog.CODEC, (p, context) -> context.enqueueWork(() -> { if (p.data != null) io.github.sudabake.appliedagristics.biology.BioLootCatalog.loadClient(p.data); }));
        registrar.playToServer(Control.TYPE, Control.CODEC, (p, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof MachineMenu menu)
                || !menu.machine.getBlockPos().equals(p.pos) || !menu.stillValid(player)) return;
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
                case 7 -> { if (machine instanceof IOControls io && p.value >= 0 && p.value < 6) io.configureSide((int)p.value,(io.sideMode((int)p.value)+1)%4); }
                case 8 -> { if (machine instanceof ItemPort port) port.toggleAutoOutput(); else if (machine instanceof IOControls io) io.toggleAutoOutput(); }
                case 9 -> { if (machine instanceof CellCultivator cultivator) cultivator.toggleAutoExtract(); }
                case 11 -> { /* Retired controller IO action: ignore without reusing its ID. */ }
                case 12 -> { if (machine instanceof NetworkPlanter) { menu.search="hide_ordinary".equals(menu.search)?"":"hide_ordinary";menu.page=0; player.getPersistentData().putBoolean("appliedagristics.hideOrdinary","hide_ordinary".equals(menu.search)); } }
                case 13 -> { if (machine instanceof EssenceVault vault && p.value>=Integer.MIN_VALUE && p.value<=Integer.MAX_VALUE) vault.setPriority((int)p.value); }
                default -> { return; }
            }
            menu.sync(player);
        }));
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (p, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof MachineMenu menu && menu.machine.getBlockPos().equals(p.pos) && p.data != null) menu.state = p.data;
        }));
    }
}
