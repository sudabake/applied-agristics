package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class UIUnificationTests {
  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void nativeMachinesAreChannelFreeAndMigrateOutputFaces(GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    var pos = f.power().above();
    h.getLevel().setBlockAndUpdate(pos, Agristics.ME_BIO.get().defaultBlockState());
    var machine = (MEBioCultivator) h.getLevel().getBlockEntity(pos);
    h.runAtTickTime(100, () -> {
      h.assertTrue(!f.controller().getMainNode().getNode().hasFlag(GridFlags.REQUIRE_CHANNEL)
          && !machine.getMainNode().getNode().hasFlag(GridFlags.REQUIRE_CHANNEL),
          "Array access and biological processor do not consume channels");
      h.assertTrue(machine.getMainNode().getGrid() == f.controller().getMainNode().getGrid(),
          "Adjacent processor remains part of the same AE network");
      var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      saved.putIntArray("sides", new int[] {0, 1, 2, 3, 0, 2});
      machine.loadWithComponents(saved, h.getLevel().registryAccess());
      int[] expected = {1, 1, 3, 3, 1, 3};
      var swab = BioData.withIdentity(Agristics.SAMPLER.get(), ResourceLocation.parse("minecraft:cow"));
      for (var side : Direction.values()) {
        h.assertTrue(machine.sideMode(side.ordinal()) == expected[side.ordinal()],
            "Legacy output bits survive migration while every input face opens");
        h.assertTrue(machine.automation(side).insertItem(0, swab, true).isEmpty()
            && machine.items.getStackInSlot(0).isEmpty(),
            "Every face accepts valid input and simulation is side-effect free");
      }
      machine.items.setStackInSlot(3, new ItemStack(Items.WHEAT));
      h.assertTrue(machine.automation(Direction.DOWN).extractItem(3, 1, true).isEmpty()
          && machine.automation(Direction.NORTH).extractItem(3, 1, true).is(Items.WHEAT),
          "Output permission controls extraction independently of input");
      var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
          appeng.core.definitions.AEItems.CERTUS_QUARTZ_WRENCH.stack());
      var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.DOWN, pos, false);
      MachineWrench.wrench(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
          player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit));
      h.assertTrue(machine.sideMode(Direction.DOWN.ordinal()) == 3, "Wrench enables output without disabling input");
      MachineWrench.wrench(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
          player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit));
      h.assertTrue(machine.sideMode(Direction.DOWN.ordinal()) == 1, "Wrench switches back to input-only");
      h.assertTrue(Agristics.CULTIVATION_CHAMBER.get().defaultBlockState().getSoundType().getBreakSound()
          == net.minecraft.sounds.SoundEvents.GLASS_BREAK, "Cultivation chamber breaks like glass");
      h.succeed();
    });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
  public static void automaticCollectionGatesNetworkInsertionButKeepsExplicitRoutes(GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    h.runAtTickTime(100, () -> {
      f.drive().getInternalInventory().setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
      var bank = f.seeds();
      var key = AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
      var network = bank.getMainNode().getGrid().getStorageService().getInventory();
      var source = IActionSource.ofMachine(f.controller());
      h.assertTrue(network.insert(key, 8, Actionable.MODULATE, source) == 8 && bank.count(key) == 0,
          "Disabled collection sends network inserts to ordinary storage");
      bank.toggleAutoCollect();
      h.assertTrue(network.insert(key, 3, Actionable.MODULATE, source) == 3 && bank.count(key) == 3,
          "Enabled collection receives new network inserts preferentially");
      bank.toggleAutoCollect();
      h.assertTrue(network.insert(key, 4, Actionable.MODULATE, source) == 4 && bank.count(key) == 3,
          "Disabling again takes effect for subsequent inserts");
      bank.add(key, 2);
      bank.collect(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
      h.assertTrue(bank.count(key) == 17 && !bank.snapshot(0, "").getBoolean("autoCollect"),
          "Direct produced seeds and manual collection work while automatic collection stays off");
      h.succeed();
    });
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void vanillaFilterKeepsBiologicalAndMysticalCrops(GameTestHelper h) {
    var f = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
    f.seeds().add(AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)), 1);
    f.seeds().add(AEItemKey.of(new ItemStack(Crops.all().getFirst().getSeedsItem())), 1);
    f.seeds().add(AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),
        ResourceLocation.parse("minecraft:cow"))), 1);
    var rows = f.controller().snapshot(0, "hide_ordinary").getList("rows", 10);
    h.assertTrue(rows.size() == 2, "Vanilla filter keeps MA and component-bearing biological seeds");
    h.succeed();
  }
}
