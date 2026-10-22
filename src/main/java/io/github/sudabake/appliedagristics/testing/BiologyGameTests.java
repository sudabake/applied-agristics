package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.CellCultivator;
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
    private static void charge(CellCultivator be) { for (int i = 0; i < 32; i++) be.energy.receiveEnergy(2000, false); }
    private static void inputs(CellCultivator be) {
        be.items.setStackInSlot(0, BioData.withIdentity(Agristics.SAMPLER.get(), COW));
        be.items.setStackInSlot(1, new ItemStack(Agristics.NUTRIENT.get())); be.items.setStackInSlot(2, new ItemStack(Agristics.CAPSULE.get()));
        be.water.fill(new FluidStack(Fluids.WATER, Balance.CULTURE_WATER.get()), FluidAction.EXECUTE);
    }
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
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void singleUseCultureAndExtraction(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, Agristics.CULTIVATOR.get()); CellCultivator be = h.getBlockEntity(pos);
        inputs(be); be.step(); h.assertTrue(be.items.getStackInSlot(0).getCount() == 1 && be.water.getFluidAmount() == Balance.CULTURE_WATER.get(), "No power preserves inputs");
        charge(be); be.step();
        h.assertTrue(be.items.getStackInSlot(0).isEmpty() && be.items.getStackInSlot(1).isEmpty() && be.items.getStackInSlot(2).isEmpty(), "One-use inputs enter active batch");
        for (int i = 1; i < Balance.CULTURE_TICKS.get(); i++) be.step();
        h.assertTrue(be.items.getStackInSlot(3).getCount() == 1 && COW.equals(BioData.identity(be.items.getStackInSlot(3))), "Exactly one mature culture retains identity");
        h.assertTrue(be.energy.getEnergyStored() == CellCultivator.CAPACITY - Balance.CULTURE_TICKS.get() * Balance.CULTURE_POWER.get(), "Culture charges the agreed FE per tick");
        be.step(); h.assertTrue(be.items.getStackInSlot(3).getCount() == 1 && be.snapshot(0, "").getString("status").equals("missing_inputs"), "No retained mother and no infinite expansion");
        be.toggleAutoExtract(); for (int i = 0; i < Balance.EXTRACT_TICKS.get(); i++) be.step();
        h.assertTrue(be.items.getStackInSlot(3).isEmpty() && be.items.getStackInSlot(4).getCount() == 1 && COW.equals(BioData.identity(be.items.getStackInSlot(4))), "Extraction destroys culture and transfers identity");
        h.assertTrue(be.items.getStackInSlot(5).getCount() == 1, "Extraction returns one empty shell");
        h.assertTrue(be.energy.getEnergyStored() == CellCultivator.CAPACITY - Balance.CULTURE_TICKS.get() * Balance.CULTURE_POWER.get() - Balance.EXTRACT_TICKS.get() * Balance.EXTRACT_POWER.get(), "Extraction FE counted exactly");
        int energy = be.energy.getEnergyStored(); be.step(); h.assertTrue(be.energy.getEnergyStored() == energy, "Idle consumes no FE"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void cultureBlockingPersistenceAndAutomation(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, Agristics.CULTIVATOR.get()); CellCultivator be = h.getBlockEntity(pos);
        inputs(be); charge(be); for (int i = 0; i < 27; i++) be.step();
        int progress = be.snapshot(0, "").getInt("progress");
        be.energy.deserializeNBT(h.getLevel().registryAccess(), net.minecraft.nbt.IntTag.valueOf(0)); be.step();
        h.assertTrue(be.snapshot(0, "").getInt("progress") == progress, "Power loss pauses active batch");
        charge(be); be.items.setStackInSlot(3, BioData.withIdentity(Agristics.CULTURE.get(), PIG)); be.step();
        h.assertTrue(be.snapshot(0, "").getInt("progress") == progress && be.energy.getEnergyStored() == CellCultivator.CAPACITY, "Different identity blocks output without power consumption");
        var drop = new ItemStack(Agristics.CULTIVATOR.get()); be.saveToItem(drop, h.getLevel().registryAccess());
        var restored = new CellCultivator(be.getBlockPos(), be.getBlockState()); restored.setLevel(h.getLevel());
        restored.loadWithComponents(drop.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(), h.getLevel().registryAccess());
        restored.items.setStackInSlot(3, ItemStack.EMPTY);
        for (int i = progress; i < Balance.CULTURE_TICKS.get(); i++) restored.step();
        h.assertTrue(restored.items.getStackInSlot(3).getCount() == 1 && COW.equals(BioData.identity(restored.items.getStackInSlot(3))), "Pickup/reload preserves paid progress and sample without duplication");
        var down = restored.automation(Direction.DOWN); var up = restored.automation(Direction.UP);
        h.assertTrue(up.extractItem(3, 1, true).isEmpty() && down.extractItem(3, 1, true).getCount() == 1, "Culture output is available from below while extraction is off");
        restored.toggleAutoExtract(); h.assertTrue(down.extractItem(3, 1, true).isEmpty(), "Cached output handler respects automatic extraction setting");
        restored.items.setStackInSlot(5, new ItemStack(Agristics.CAPSULE.get(), 64)); int energy = restored.energy.getEnergyStored(); restored.step();
        h.assertTrue(restored.energy.getEnergyStored() == energy && restored.items.getStackInSlot(4).isEmpty(), "Full shell return blocks extraction atomically");
        var saved = restored.saveWithFullMetadata(h.getLevel().registryAccess()); var again = new CellCultivator(restored.getBlockPos(), restored.getBlockState()); again.setLevel(h.getLevel()); again.loadWithComponents(saved, h.getLevel().registryAccess());
        again.items.setStackInSlot(5, ItemStack.EMPTY); for (int i = 0; i < Balance.EXTRACT_TICKS.get(); i++) again.step();
        h.assertTrue(again.items.getStackInSlot(4).getCount() == 1 && again.items.getStackInSlot(5).getCount() == 1 && again.autoExtract(), "Blocked extraction survives restart with one output and one returned shell");
        h.assertTrue(again.energy.extractEnergy(100, false) == 0, "FE port cannot extract stored energy");
        h.assertTrue(again.water.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE) == 0, "Only water is accepted");
        h.assertTrue(!again.items.isItemValid(0, new ItemStack(Agristics.SAMPLER.get())), "Blank sampler cannot be cultured");
        h.succeed();
    }
}
