package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.mojang.logging.LogUtils;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Two client processes exercise the real chunk save and subsequent AE storage mount. */
final class StoragePersistenceChecks {
    private static final BlockPos DRIVE = new BlockPos(5, 100, 32);
    private static final BlockPos PROVIDER = DRIVE.east();
    private static int ticks;
    private static volatile boolean finished;

    static boolean tick(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var playerId = mc.player.getUUID();
        boolean prepare = System.getProperty("appliedagristics.storageReload").equals("prepare");
        mc.options.pauseOnLostFocus = false;
        if (++ticks == 1) server.execute(() -> {
            var level = server.overworld();
            var player = server.getPlayerList().getPlayer(playerId);
            player.teleportTo(level, DRIVE.getX(), DRIVE.getY() + 1, DRIVE.getZ(), java.util.Set.of(), 0, 0);
            level.getChunkAt(DRIVE);
            if (prepare) prepare(level);
        });
        if (ticks == 80) server.execute(() -> {
            try {
                verify(server.overworld(), prepare);
                LogUtils.getLogger().info("CLIENT_SMOKE_PASSED storageReload={} cowAndPigDistinct=true exactNbtPattern=true",
                    prepare ? "prepared" : "verified");
            } catch (RuntimeException exception) {
                LogUtils.getLogger().error("CLIENT_SMOKE_BIOLOGY_FAILED storage reload", exception);
            } finally {
                finished = true;
            }
        });
        return finished;
    }

    private static void prepare(ServerLevel level) {
        level.setBlockAndUpdate(DRIVE, AEBlocks.DRIVE.block().defaultBlockState());
        level.setBlockAndUpdate(DRIVE.west(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(PROVIDER, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        var drive = (DriveBlockEntity) level.getBlockEntity(DRIVE);
        drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_1K.stack());
        var cow = new ResourceLocation("minecraft:cow");
        var seed = BioData.withIdentity(Agristics.BIO_SEED.get(), cow);
        ItemStack pattern = ForgeTestInputs.processingPattern(
            List.of(GenericStack.fromItemStack(BioData.withIdentity(Agristics.SAMPLER.get(), cow)),
                GenericStack.fromItemStack(new ItemStack(Agristics.NUTRIENT.get())),
                GenericStack.fromItemStack(new ItemStack(Agristics.EMPTY_BIO_SEED.get()))),
            List.of(GenericStack.fromItemStack(seed)));
        var provider = (PatternProviderBlockEntity) level.getBlockEntity(PROVIDER);
        provider.getLogic().getPatternInv().setItemDirect(0, pattern);
    }

    private static void verify(ServerLevel level, boolean prepare) {
        var drive = (DriveBlockEntity) level.getBlockEntity(DRIVE);
        var provider = (PatternProviderBlockEntity) level.getBlockEntity(PROVIDER);
        var cow = new ResourceLocation("minecraft:cow");
        var pig = new ResourceLocation("minecraft:pig");
        var cowSeed = AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(), cow));
        var pigSeed = AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(), pig));
        var grid = drive.getMainNode().getGrid();
        if (grid == null || !drive.getMainNode().isActive()) throw new IllegalStateException("Saved AE drive is offline");
        var inventory = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(drive);
        if (prepare && (inventory.insert(cowSeed, 7, Actionable.MODULATE, source) != 7
                        || inventory.insert(pigSeed, 11, Actionable.MODULATE, source) != 11))
            throw new IllegalStateException("Could not store the two biological seed identities");
        if (inventory.extract(cowSeed, 64, Actionable.SIMULATE, source) != 7
            || inventory.extract(pigSeed, 64, Actionable.SIMULATE, source) != 11)
            throw new IllegalStateException("AE merged or lost biological seed identities");
        var pattern = PatternDetailsHelper.decodePattern(provider.getLogic().getPatternInv().getStackInSlot(0), level);
        if (pattern == null || !cowSeed.equals(pattern.getPrimaryOutput().what()))
            throw new IllegalStateException("Encoded pattern lost its biological output");
        var sampler = AEItemKey.of(BioData.withIdentity(Agristics.SAMPLER.get(), cow));
        boolean samplerFound = false;
        for (var input : pattern.getInputs()) for (var choice : input.getPossibleInputs())
            if (sampler.equals(choice.what())) samplerFound = true;
        if (!samplerFound) throw new IllegalStateException("Encoded pattern lost its sampled input identity");
        if (!prepare && (inventory.extract(cowSeed, 7, Actionable.MODULATE, source) != 7
            || inventory.extract(pigSeed, 11, Actionable.MODULATE, source) != 11))
            throw new IllegalStateException("Saved biological items were not separately retrievable");
    }

    private StoragePersistenceChecks() {}
}
