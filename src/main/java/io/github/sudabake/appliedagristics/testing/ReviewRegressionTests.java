package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Balance;
import io.github.sudabake.appliedagristics.Cultivations;
import io.github.sudabake.appliedagristics.logic.CropSources;
import io.github.sudabake.appliedagristics.logic.PlantingRecipe;
import io.github.sudabake.appliedagristics.network.Messages;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Regressions from the 2026-10-26 review; only registered in developer test runs. */
@PrefixGameTestTemplate(false)
public final class ReviewRegressionTests {
    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void conversionPreservesCarrotHarvest(GameTestHelper h) {
        nativeEdibleSeed(h, Items.CARROT);
    }

    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void conversionPreservesPotatoHarvest(GameTestHelper h) {
        nativeEdibleSeed(h, Items.POTATO);
    }

    private static void nativeEdibleSeed(GameTestHelper h, Item item) {
        var fixture = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
        fixture.modules(0, 0, true);
        var key = AEItemKey.of(new ItemStack(item));
        fixture.seeds().add(key, 1);
        h.runAtTickTime(100, () -> {
            var controller = fixture.controller();
            h.assertTrue(controller.getMainNode().isActive() && controller.conversion(), "Powered array has conversion");
            var crop = Cultivations.seed(key.toStack(1), h.getLevel(), controller.getBlockPos());
            h.assertTrue(key.equals(AEItemKey.of(crop.essenceStack())), "Native product and seed share the exact key");
            controller.configure(crop.getId(), true, -1);
            controller.tasks.get(crop.getId()).progress = 32 * Balance.LATE_TICKS.get();
            controller.step();
            h.assertTrue(fixture.seeds().count(key) == 1, "Existing reserved seed is not consumed or converted");
            h.assertTrue(fixture.output().count(key) > 0, "Completed native harvest reaches output instead of disappearing");
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void conversionPreservesExactSameKeyRecipe(GameTestHelper h) {
        var fixture = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
        fixture.modules(0, 1, true);
        var key = AEItemKey.of(new ItemStack(Items.APPLE));
        fixture.seeds().add(key, 1);
        h.runAtTickTime(100, () -> withRecipe(h, Items.APPLE, List.of(new ItemStack(Items.APPLE, 3)), () -> {
            var controller = fixture.controller();
            var id = Cultivations.id(key.toStack(1), h.getLevel());
            h.assertTrue(controller.getMainNode().isActive() && controller.parallel() == 4, "Array has four parallel slots");
            controller.configure(id, true, -1);
            var task = controller.tasks.get(id);
            task.progress = 200;
            controller.step();
            h.assertTrue(fixture.seeds().count(key) == 4 && task.occupied == 4 && fixture.output().count(key) == 0,
                    "First three net products fill parallel capacity exactly");
            task.progress = 400;
            controller.step();
            h.assertTrue(fixture.seeds().count(key) == 4 && fixture.output().count(key) == 6,
                    "Two full batches output exactly six, with neither loss nor duplication");
            controller.configure(id, false, -1);
            h.assertTrue(fixture.seeds().used(key) == 0 && fixture.seeds().count(key) == 4,
                    "Stopping returns all four seeds and leaves the harvest intact");
            h.succeed();
        }));
    }

    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void conversionOnlyConsumesNewDifferentKeySeeds(GameTestHelper h) {
        var fixture = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
        fixture.modules(0, 1, true);
        var seed = AEItemKey.of(new ItemStack(Items.COOKIE));
        var product = AEItemKey.of(new ItemStack(Items.DIAMOND));
        fixture.seeds().add(seed, 1);
        h.runAtTickTime(100, () -> withRecipe(h, Items.COOKIE,
                List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.COOKIE, 3)), () -> {
            var controller = fixture.controller();
            var id = Cultivations.id(seed.toStack(1), h.getLevel());
            h.assertTrue(controller.getMainNode().isActive(), "Conversion array is powered");
            controller.configure(id, true, -1);
            var task = controller.tasks.get(id);
            task.progress = 200;
            controller.step();
            h.assertTrue(fixture.seeds().count(seed) == 4 && fixture.output().count(product) == 2,
                    "New seeds fill parallel capacity before any conversion");
            fixture.seeds().add(seed, 10);
            task.progress = 400;
            controller.step();
            h.assertTrue(fixture.seeds().count(seed) == 14 && fixture.output().count(product) == 12,
                    "Two batches add four products plus six converted seeds; all stored seeds remain");
            h.succeed();
        }));
    }

    private static void withRecipe(GameTestHelper h, Item seed, List<ItemStack> outputs, Runnable check) {
        var manager = h.getLevel().getRecipeManager();
        var originals = new ArrayList<>(manager.getRecipes());
        var recipes = new ArrayList<RecipeHolder<?>>(originals);
        var recipe = new PlantingRecipe(Ingredient.of(seed), Ingredient.of(Blocks.DIRT), outputs,
                Blocks.WHEAT.defaultBlockState(), false, 200, true);
        recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID, "test/review_conversion"), recipe));
        try {
            manager.replaceRecipes(recipes);
            CropSources.invalidate();
            check.run();
        } finally {
            manager.replaceRecipes(originals);
            CropSources.invalidate();
        }
    }

    @GameTest(template = "empty", templateNamespace = Agristics.ID, timeoutTicks = 240)
    public static void longVariantControlsRoundTripAndKeepSavedIds(GameTestHelper h) {
        var fixture = PlanterFixture.install(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), true);
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mysticalagriculture:quartz_enriched_iron_seeds"));
        h.assertTrue(item != Items.AIR, "Long-named MA seed is registered");
        var seed = new ItemStack(item);
        seed.set(DataComponents.CUSTOM_NAME, Component.literal("Review variety"));
        var id = Cultivations.id(seed, h.getLevel());
        h.assertTrue(id.toString().length() > 128, "Actual generated variant ID exceeds the former protocol limit");
        fixture.seeds().add(AEItemKey.of(seed), 1);
        h.runAtTickTime(100, () -> {
            var controller = fixture.controller();
            for (int action : new int[]{1, 2}) {
                var packet = new Messages.Control(controller.getBlockPos(), action, id.toString(), 123456, seed);
                var decoded = roundTrip(h, packet);
                h.assertTrue(decoded.pos().equals(packet.pos()) && decoded.action() == action
                                && decoded.id().equals(id.toString()) && decoded.value() == packet.value()
                                && ItemStack.isSameItemSameComponents(decoded.seed(), seed),
                        "Actual control codec preserves long identity, action, target and components");
                controller.configure(ResourceLocation.parse(decoded.id()), action == 1, decoded.value());
                var task = controller.tasks.get(id);
                h.assertTrue(task != null && task.enabled == (action == 1) && task.target == 123456,
                        "Decoded controls configure the existing variant task");
            }
            var tasks = controller.saveWithFullMetadata(h.getLevel().registryAccess()).getList("tasks", 10);
            h.assertTrue(tasks.size() == 1 && tasks.getCompound(0).getString("id").equals(id.toString()),
                    "Save identity remains unchanged rather than truncated or replaced");
            h.succeed();
        });
    }

    private static Messages.Control roundTrip(GameTestHelper h, Messages.Control packet) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            Messages.Control.CODEC.encode(buffer, packet);
            var decoded = Messages.Control.CODEC.decode(buffer);
            h.assertTrue(!buffer.isReadable(), "Control codec consumes the entire packet");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "empty", templateNamespace = Agristics.ID)
    public static void controlIdBoundsAreSymmetric(GameTestHelper h) {
        for (var id : List.of("", "minecraft:wheat", "test:" + "a".repeat(Messages.Control.MAX_ID_LENGTH - 5))) {
            var packet = new Messages.Control(BlockPos.ZERO, 4, id, -1, ItemStack.EMPTY);
            h.assertTrue(roundTrip(h, packet).id().equals(id), "Empty, ordinary and maximum-length IDs round-trip");
        }
        var oversized = "a".repeat(Messages.Control.MAX_ID_LENGTH + 1);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            boolean rejected = false;
            try {
                Messages.Control.CODEC.encode(buffer, new Messages.Control(BlockPos.ZERO, 1, oversized, -1, ItemStack.EMPTY));
            } catch (EncoderException expected) {
                rejected = true;
            }
            h.assertTrue(rejected, "Encoder rejects IDs exceeding the documented bound");
            buffer.clear();
            buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeVarInt(1);
            buffer.writeUtf(oversized, Messages.Control.MAX_ID_LENGTH + 1);
            rejected = false;
            try {
                Messages.Control.CODEC.decode(buffer);
            } catch (DecoderException expected) {
                rejected = true;
            }
            h.assertTrue(rejected, "Decoder enforces the same bound for malformed incoming packets");
            h.succeed();
        } finally {
            buffer.release();
        }
    }
}
