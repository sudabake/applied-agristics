package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.compat.jdte.JdteBiologicalCrops;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.*;

@PrefixGameTestTemplate(false)
public final class JdteBatchTests {
  private static Object call(Object host, String name, Object... args) throws ReflectiveOperationException {
    for (var method : host.getClass().getDeclaredMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == args.length) {
        method.setAccessible(true);
        return method.invoke(host, args);
      }
    }
    throw new NoSuchMethodException(name);
  }

  private static Object field(Object host, String name) throws ReflectiveOperationException {
    var field = host.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(host);
  }

  private static Object host(GameTestHelper h, String name, int x) {
    var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("jdte:" + name));
    h.assertTrue(block != Blocks.AIR, "Real JDTE greenhouse is registered");
    var pos = new BlockPos(x, 1, 1);
    h.setBlock(pos, block);
    return h.getBlockEntity(pos);
  }

  private static Object definition(ItemStack seed) throws ReflectiveOperationException {
    return Class.forName("com.jdte.common.recipes.GreenhouseCropResolver")
        .getMethod("findGeneric", ItemStack.class).invoke(null, seed);
  }

  private static Object plan(Object host, GameTestHelper h, ItemStack seed, Object definition, int count)
      throws ReflectiveOperationException {
    Method planner = Arrays.stream(host.getClass().getDeclaredMethods())
        .filter(method -> method.getName().equals("planDrops")).findFirst().orElseThrow();
    var ledgerType = planner.getParameterTypes()[planner.getParameterCount() - 1];
    var capture = ledgerType.getDeclaredMethod("capture", IItemHandler.class);
    capture.setAccessible(true);
    var ledger = capture.invoke(null, field(host, "internalOutputHandler"));
    return planner.getParameterCount() == 5
        ? call(host, "planDrops", h.getLevel(), seed, definition, count, ledger)
        : call(host, "planDrops", h.getLevel(), seed, definition, count, 0, ledger);
  }

  @SuppressWarnings("unchecked")
  private static List<ItemStack> drops(Object plan) throws ReflectiveOperationException {
    return (List<ItemStack>) call(plan, "drops");
  }

  private static int count(List<ItemStack> drops, net.minecraft.world.item.Item item) {
    return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void biologicalCropsUseNativeBatchesInBothGreenhouses(GameTestHelper h) {
    if (!ModList.get().isLoaded("jdte")) { h.succeed(); return; }
    double chance = Balance.FALLBACK_SEED_CHANCE.get();
    try {
      int x = 1;
      for (var name : List.of("greenhouse", "large_greenhouse")) {
        var machine = host(h, name, x++);
        h.assertTrue(machine instanceof JdteBiologicalCrops.BatchHost, "Native sampler hook is applied");
        for (var species : List.of("minecraft:cow", "minecraft:chicken")) {
          var identity = ResourceLocation.parse(species);
          var seed = BioData.withIdentity(Agristics.BIO_SEED.get(), identity);
          var definition = definition(seed);
          h.assertTrue(definition instanceof JdteBiologicalCrops.Definition
              && call(definition, "harvestGenerator") == null,
              "Biological definitions use the batch path instead of the dynamic callback quota");
          for (double probability : new double[] {0, 1}) {
            Balance.FALLBACK_SEED_CHANCE.set(probability);
            var plan = plan(machine, h, seed, definition, 512);
            var drops = drops(plan);
            h.assertTrue((int) call(plan, "harvests") == 512
                && count(drops, Agristics.BIO_ESSENCE.get()) == 512
                && count(drops, Agristics.BIO_SEED.get()) == (probability == 0 ? 0 : 512)
                && drops.stream().allMatch(stack -> identity.equals(BioData.identity(stack))),
                "Native 512-harvest batch preserves species, probability boundaries and net seed accounting");
          }
          var type = definition.getClass();
          var components = type.getRecordComponents();
          var values = new Object[components.length];
          var types = new Class<?>[components.length];
          for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            values[i] = components[i].getAccessor().invoke(definition);
            if (components[i].getName().equals("outputs"))
              values[i] = List.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), identity).copyWithCount(7));
          }
          var explicit = type.getConstructor(types).newInstance(values);
          var plan = plan(machine, h, seed, explicit, 128);
          h.assertTrue(count(drops(plan), Agristics.BIO_ESSENCE.get()) == 896
              && count(drops(plan), Agristics.BIO_SEED.get()) == 0,
              "Unmarked explicit recipes retain their exact outputs without injected bonus seeds");
        }
      }
      h.succeed();
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("JDTE batch integration failed", exception);
    } finally { Balance.FALLBACK_SEED_CHANCE.set(chance); }
  }

  private static void settle(Object host, Object definition) throws ReflectiveOperationException {
    if (host.getClass().getSimpleName().equals("GreenhouseBE"))
      call(host, "settleSlot", 0, definition, 65536, 512, new int[] {0});
    else call(host, "settleSlot", 0, definition, 65536, 512, 9,
        call(host, "currentProductionSettings"), List.of(host), new int[] {0});
  }

  @GameTest(template = "empty", templateNamespace = Agristics.ID)
  public static void nativeSettlementPaysForRealOutputAndStopsWhenBlocked(GameTestHelper h) {
    if (!ModList.get().isLoaded("jdte")) { h.succeed(); return; }
    double chance = Balance.FALLBACK_SEED_CHANCE.get();
    try {
      Balance.FALLBACK_SEED_CHANCE.set(0.0);
      int x = 1;
      for (var name : List.of("greenhouse", "large_greenhouse")) {
        var machine = host(h, name, x++);
        var seed = BioData.withIdentity(Agristics.BIO_SEED.get(), ResourceLocation.parse("minecraft:cow"));
        var definition = definition(seed);
        var inventory = (IItemHandlerModifiable) field(machine, "itemHandler");
        inventory.setStackInSlot(0, seed.copyWithCount(64));
        var outputs = (IItemHandler) field(machine, "internalOutputHandler");
        var energy = (IEnergyStorage) call(machine, "getEnergyStorage");
        var tank = (IFluidHandler) call(machine, "getFluidTank");
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse("justdirethings:time_fluid_source"));
        energy.receiveEnergy(Integer.MAX_VALUE, false);
        tank.fill(new FluidStack(fluid, 64000), IFluidHandler.FluidAction.EXECUTE);
        int power = energy.getEnergyStored(), water = tank.getFluidInTank(0).getAmount();
        int powerCost = (int) call(machine, "getEffectiveEnergyPerHarvest");
        int fluidCost = (int) call(machine, "getEffectiveFluidPerHarvest", 0, definition);
        settle(machine, definition);
        int produced = 0;
        for (int i = 0; i < outputs.getSlots(); i++) produced += outputs.getStackInSlot(i).getCount();
        com.mojang.logging.LogUtils.getLogger().info("JDTE_SETTLEMENT host={} produced={} paidEnergy={} cost={} paidFluid={} cost={} input={}",
            name, produced, power-energy.getEnergyStored(), powerCost, water-tank.getFluidInTank(0).getAmount(), fluidCost, inventory.getStackInSlot(0).getCount());
        h.assertTrue(produced == 512 && power - energy.getEnergyStored() == 512 * powerCost
            && water - tank.getFluidInTank(0).getAmount() == (name.equals("greenhouse")
                ? 512 * fluidCost : (512 * fluidCost + 8) / 9)
            && inventory.getStackInSlot(0).getCount() == 64,
            "Real settlement exceeds a zero dynamic quota and pays native costs only for produced harvests");
        var entity = (net.minecraft.world.level.block.entity.BlockEntity) machine;
        var saved = entity.saveWithFullMetadata(h.getLevel().registryAccess());
        entity.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(inventory.getStackInSlot(0).getCount() == 64
            && BioData.identity(inventory.getStackInSlot(0)).equals(BioData.identity(seed))
            && energy.getEnergyStored() == power - 512 * powerCost,
            "Save/reload retains input identity and already-paid energy");
        int firstOutput = name.equals("greenhouse") ? 4 : 12;
        for (int i = 0; i < outputs.getSlots(); i++)
          inventory.setStackInSlot(firstOutput + i, new ItemStack(Items.COBBLESTONE, 64));
        power = energy.getEnergyStored(); water = tank.getFluidInTank(0).getAmount();
        settle(machine, definition);
        h.assertTrue(power == energy.getEnergyStored() && water == tank.getFluidInTank(0).getAmount(),
            "Full outputs stop native energy and fluid payment");
        for (int i = 0; i < outputs.getSlots(); i++) inventory.setStackInSlot(firstOutput + i, ItemStack.EMPTY);
        tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        settle(machine, definition);
        h.assertTrue(energy.getEnergyStored() == power && outputs.getStackInSlot(0).isEmpty(),
            "Missing required fluid produces nothing and consumes no power");
        tank.fill(new FluidStack(fluid, 64000), IFluidHandler.FluidAction.EXECUTE);
        energy.extractEnergy(Integer.MAX_VALUE, false);
        int availableFluid = tank.getFluidInTank(0).getAmount();
        settle(machine, definition);
        h.assertTrue(energy.getEnergyStored() == 0 && tank.getFluidInTank(0).getAmount() == availableFluid
            && outputs.getStackInSlot(0).isEmpty(), "Power loss consumes neither fluid nor inputs");
      }
      h.succeed();
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("JDTE native settlement failed", exception);
    } finally { Balance.FALLBACK_SEED_CHANCE.set(chance); }
  }
}
