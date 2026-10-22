package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Opt-in developer smoke test; only operates on the disposable agr-smoke world. */
@EventBusSubscriber(modid = Agristics.ID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static boolean loading;
    private static boolean setup;
    private static int ticks;
    private static int stage;
    private static BlockPos origin;
    private static PlanterFixture planterFixture;
    private static BlockPos smokePos(int stage) {
        if (stage==5) return planterFixture.input().getBlockPos();
        if (stage==6) return planterFixture.output().getBlockPos();
        if (stage==7) return planterFixture.vault().getBlockPos();
        if (stage==3) return origin.offset(9,0,-8);
        return stage>=3 ? origin.offset(stage,0,-2) : origin.offset(stage,0,0);
    }
    private static int watchdog;
    private static boolean done;
    private static boolean guidePending;
    private static volatile boolean corePassed;
    private static int guideTicks;
    private static String expectedScrollId = "";
    private static boolean scrollPassed;
    private static boolean withdrawPassed;
    private static boolean dragPassed;
    private static boolean altarPassed;
    private static boolean nativeScreensPassed=true;
    private static int sceneTicks;
    private static boolean scenePending;
    private static ItemStack infusionExpected=ItemStack.EMPTY;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("appliedagristics.clientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (done) return;
        if (++watchdog > 2400 || setup && mc.level == null) { org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("CLIENT_SMOKE_TIMEOUT_OR_DISCONNECT"); mc.stop(); return; }
        if (!loading && mc.getOverlay() == null) {
            loading = true; mc.setScreen(new TitleScreen());
            mc.createWorldOpenFlows().openWorld("agr-smoke", () -> { org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("CLIENT_SMOKE_WORLD_OPEN_FAILED"); mc.stop(); });
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (scenePending) {
            sceneTicks++;
            if (sceneTicks==1) mc.getSingleplayerServer().execute(()->{
                var crop=Crops.all().getFirst(); var controller=planterFixture.controller();
                controller.configure(crop.getId(),true,-1); controller.step(); controller.updateWorkVisual();
                var culture=(CellCultivator)mc.getSingleplayerServer().overworld().getBlockEntity(smokePos(4));
                culture.items.setStackInSlot(0,io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.SAMPLER.get(),net.minecraft.resources.ResourceLocation.parse("minecraft:cow")));
            });
            if (sceneTicks==20) {
                Screenshot.grab(mc.gameDirectory,"agr-array-in-world.png",mc.getMainRenderTarget(),message -> {});
                boolean models=true;
                for (int y=-1;y<3;y++) for (int z=0;z<3;z++) for (int x=1;x<4;x++) {
                    var state=mc.level.getBlockState(origin.offset(x,y,z));
                    models &= state.hasProperty(PlanterPartBlock.CELL) && state.getValue(PlanterPartBlock.FORMED)
                        && mc.getBlockRenderer().getBlockModel(state)!=mc.getModelManager().getMissingModel();
                }
                corePassed &= models;
                var controller=(NetworkPlanter)mc.level.getBlockEntity(planterFixture.controller().getBlockPos());
                boolean activity=controller.workingVisual() && mc.getBlockEntityRenderDispatcher().getRenderer(controller) instanceof io.github.sudabake.appliedagristics.client.MachineActivityRenderer;
                var sprite=mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(net.minecraft.resources.ResourceLocation.parse("appliedagristics:item/mature_culture"));
                boolean animated=sprite.contents().getUniqueFrames().count()==24;
                corePassed &= activity && animated;
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_ACTIVITY_RESULT running={} cultureFrames={}",activity,animated);
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_FORMED_MODELS_RESULT {}",models);
                mc.getSingleplayerServer().execute(()->{
                    var level=mc.getSingleplayerServer().overworld(); ((EarlyPlanter)level.getBlockEntity(origin)).items.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS));
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level,0,101,-3,java.util.Set.of(),-8,28);
                });
            }
            if (sceneTicks==25) mc.getSingleplayerServer().execute(()->{ planterFixture.controller().configureAll(false); planterFixture.controller().step(); planterFixture.controller().updateWorkVisual(); });
            if (sceneTicks==40) {
                boolean stopped=!((NetworkPlanter)mc.level.getBlockEntity(planterFixture.controller().getBlockPos())).workingVisual();
                corePassed &= stopped; org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_ACTIVITY_STOP_RESULT {}",stopped);
            }
            if (sceneTicks==40 || sceneTicks==60 || sceneTicks==80 || sceneTicks==100) {
                var early=(EarlyPlanter)mc.level.getBlockEntity(origin); var seed=early.items.getStackInSlot(0);
                String identity=sceneTicks==60 ? "minecraft:cow" : "minecraft:pig";
                boolean passed=sceneTicks==40 ? seed.is(net.minecraft.world.item.Items.WHEAT_SEEDS) : sceneTicks==100 ? seed.isEmpty() : identity.equals(String.valueOf(io.github.sudabake.appliedagristics.biology.BioData.identity(seed)));
                passed &= mc.getBlockEntityRenderDispatcher().getRenderer(early) instanceof io.github.sudabake.appliedagristics.client.EarlyPlanterRenderer
                    && mc.getModelManager().getModel(io.github.sudabake.appliedagristics.client.EarlyPlanterRenderer.BIO_MODEL)!=mc.getModelManager().getMissingModel();
                corePassed &= passed;
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_PLANT_DISPLAY_RESULT stage={} passed={}",sceneTicks,passed);
                Screenshot.grab(mc.gameDirectory,"agr-plant-display-"+sceneTicks+".png",mc.getMainRenderTarget(),message->{});
                if (sceneTicks<100) {
                    int next=sceneTicks;
                    mc.getSingleplayerServer().execute(()->((EarlyPlanter)mc.getSingleplayerServer().overworld().getBlockEntity(origin)).items.setStackInSlot(0,next==80 ? ItemStack.EMPTY : io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_SEED.get(),net.minecraft.resources.ResourceLocation.parse(next==40?"minecraft:cow":"minecraft:pig"))));
                } else {
                    mc.getSingleplayerServer().execute(()->{
                        var level=mc.getSingleplayerServer().overworld(); ((EarlyPlanter)level.getBlockEntity(origin)).items.setStackInSlot(1,ItemStack.EMPTY);
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        net.minecraft.world.level.ItemLike[] icons={Agristics.SAMPLER.get(),Agristics.CAPSULE.get(),Agristics.CULTURE.get(),Agristics.EXTRACT.get(),Agristics.NUTRIENT.get()};
                        for (int i=0;i<icons.length;i++) player.getInventory().setItem(i,new ItemStack(icons[i]));
                        player.getInventory().selected=8; player.getInventory().setItem(8,ItemStack.EMPTY);
                        var pos=smokePos(4); mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level,pos.getX()+1.5,101.5,pos.getZ()-2.5,java.util.Set.of(),20,16);
                    });
                }
            }
            if (sceneTicks==115) {
                var culture=(CellCultivator)mc.level.getBlockEntity(smokePos(4));
                boolean passed=culture.workingVisual() && mc.getBlockEntityRenderDispatcher().getRenderer(culture) instanceof io.github.sudabake.appliedagristics.client.MachineActivityRenderer;
                corePassed &= passed; org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_CULTURE_BUBBLES_RESULT {}",passed);
                Screenshot.grab(mc.gameDirectory,"agr-culture-bubbles.png",mc.getMainRenderTarget(),message->{});
            }
            if (sceneTicks==120) mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld(); var pos=smokePos(3); var altar=(MEInfusionAltar)level.getBlockEntity(pos);
                level.setBlockAndUpdate(pos.west(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                for (var p:altar.pedestalPositions()) level.setBlockAndUpdate(p,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
                var recipe=level.getRecipeManager().getAllRecipesFor(com.blakebr0.mysticalagriculture.init.ModRecipeTypes.INFUSION.get()).stream().map(r->r.value()).filter(r->r.getIngredients().size()==8 && !r.getAltarIngredient().isEmpty()).findFirst().orElseThrow();
                for (int i=2;i<6;i++) altar.items.setStackInSlot(i,ItemStack.EMPTY);
                if (altar.snapshot(0,"").getBoolean("autoOutput")) altar.toggleAutoOutput();
                for (int i=0;i<8;i++) { var ingredient=recipe.getIngredients().get(i); ((com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity)level.getBlockEntity(altar.pedestalPositions().get(i))).getInventory().setStackInSlot(0,ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(1)); }
                altar.items.setStackInSlot(0,recipe.getAltarIngredient().getItems()[0].copyWithCount(1)); infusionExpected=recipe.getResultItem(level.registryAccess()).copy();
                mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level,pos.getX()+4,102,pos.getZ()-6,java.util.Set.of(),33,20);
            });
            if (sceneTicks==240) mc.getSingleplayerServer().execute(()->{
                var altar=(MEInfusionAltar)mc.getSingleplayerServer().overworld().getBlockEntity(smokePos(3));
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_NATIVE_SERVER status={} progress={} center={} output={} expected={}",altar.status,altar.snapshot(0,"").getInt("progress"),altar.items.getStackInSlot(0),altar.items.getStackInSlot(1),infusionExpected);
            });
            if (sceneTicks==150 || sceneTicks==300) {
                var altar=(MEInfusionAltar)mc.level.getBlockEntity(smokePos(3));
                boolean passed=mc.getBlockEntityRenderDispatcher().getRenderer(altar) instanceof io.github.sudabake.appliedagristics.client.MEInfusionRenderer;
                passed &= sceneTicks==150 ? !altar.items.getStackInSlot(0).isEmpty() : ItemStack.isSameItemSameComponents(altar.items.getStackInSlot(1),infusionExpected);
                corePassed &= passed; org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_NATIVE_INFUSION_RESULT stage={} passed={}",sceneTicks,passed);
                Screenshot.grab(mc.gameDirectory,"agr-native-infusion-"+sceneTicks+".png",mc.getMainRenderTarget(),message->{});
                if (sceneTicks==300) { scenePending=false; guidePending=true; }
            }
            return;
        }
        if (guidePending) {
            guideTicks++;
            var cowId = net.minecraft.resources.ResourceLocation.parse("minecraft:cow");
            if (guideTicks >= 20) {
                if (net.neoforged.fml.ModList.get().isLoaded("jei") && !io.github.sudabake.appliedagristics.compat.jei.JeiSmoke.tick()) return;
                var cowSeed = io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_SEED.get(), cowId);
                var pigSeed = io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_SEED.get(), net.minecraft.resources.ResourceLocation.parse("minecraft:pig"));
                var egg = net.minecraft.world.item.SpawnEggItem.byId(net.minecraft.world.entity.EntityType.COW);
                var essence=io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_ESSENCE.get(),cowId);
                var tooltip=new java.util.ArrayList<net.minecraft.network.chat.Component>(); essence.getItem().appendHoverText(essence,net.minecraft.world.item.Item.TooltipContext.of(mc.level),tooltip,net.minecraft.world.item.TooltipFlag.NORMAL);
                var before=mc.screen; var use=essence.getItem().use(mc.level,mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);
                corePassed &= tooltip.size()==1 && use.getResult()==net.minecraft.world.InteractionResult.PASS && mc.screen==before;
                if (corePassed && io.github.sudabake.appliedagristics.biology.BioLootCatalog.clientDrops(cowId).contains(net.minecraft.world.item.Items.BEEF)
                    && mc.getItemColors().getColor(cowSeed, 0) == (0xFF000000 | egg.getColor(0)) && mc.getItemColors().getColor(cowSeed, 0) != mc.getItemColors().getColor(pigSeed, 0))
                    org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_SMOKE_PASSED menus=8 formedModels=true plantDisplay=true jeiOnlyRecipes=true spawnEggTint=true");
                else org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("CLIENT_SMOKE_BIOLOGY_FAILED");
                done = true; mc.stop();
            }
            return;
        }
        if (!setup) {
            setup = true; var id = mc.player.getUUID(); origin = new BlockPos(0, 100, 0);
            mc.options.pauseOnLostFocus = false;
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id); var level = player.serverLevel();
                level.setDayTime(6000); level.setWeatherParameters(6000,0,false,false);
                level.setBlockAndUpdate(origin.offset(3,0,-2),Blocks.AIR.defaultBlockState());
                player.getInventory().clearContent();
                net.minecraft.world.level.ItemLike[] visibleParts={Agristics.EARLY.get(),Agristics.CULTIVATOR.get(),Agristics.NETWORK.get(),Agristics.SEEDS.get(),Agristics.NETWORK_ACCESS.get(),Agristics.INPUT_PORT.get(),Agristics.OUTPUT_PORT.get(),Agristics.ESSENCE_VAULT.get(),Agristics.PLANTER_FRAME.get(),Agristics.CULTIVATION_CHAMBER.get(),Agristics.SPEED_MODULE.get(),Agristics.PARALLEL_MODULE.get(),Agristics.CONVERSION_MODULE.get(),Agristics.ME_INFUSION.get()};
                for (int i=0;i<visibleParts.length;i++) player.getInventory().setItem(i+9,new ItemStack(visibleParts[i]));
                net.minecraft.world.level.ItemLike[] biologyItems={Agristics.SAMPLER.get(),Agristics.CAPSULE.get(),Agristics.CULTURE.get(),Agristics.EXTRACT.get(),Agristics.NUTRIENT.get()};
                for (int i=0;i<biologyItems.length;i++) player.getInventory().setItem(i,new ItemStack(biologyItems[i]));
                for (int x = -1; x <= 5; x++) for (int z = -1; z <= 2; z++) level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int x = 0; x <= 4; x++) for (int z = 0; z <= 1; z++) level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(origin, Agristics.EARLY.get().defaultBlockState());
                planterFixture=PlanterFixture.install(level,origin.offset(2,0,0),true);
                planterFixture.output().items.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.WHEAT,16));
                for (int i=0;i<5;i++) planterFixture.vault().storeProduced(appeng.api.stacks.AEItemKey.of(new ItemStack(Crops.all().get(i).getEssenceItem())),1000L*(i+1),false);
                // These two positions lie outside the old fixture clearing rectangle.
                // Replace their entities as well, so a repeated run never inherits IO toggles.
                level.setBlockAndUpdate(smokePos(3),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(smokePos(4),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(smokePos(3), Agristics.ME_INFUSION.get().defaultBlockState());
                level.setBlockAndUpdate(smokePos(4), Agristics.CULTIVATOR.get().defaultBlockState());
                var culture = (io.github.sudabake.appliedagristics.block.CellCultivator) level.getBlockEntity(smokePos(4));
                culture.items.setStackInSlot(0, io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.SAMPLER.get(), net.minecraft.resources.ResourceLocation.parse("minecraft:cow")));
                culture.items.setStackInSlot(1, new ItemStack(Agristics.NUTRIENT.get(), 8)); culture.items.setStackInSlot(2, new ItemStack(Agristics.CAPSULE.get(), 8));
                culture.water.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
                for (int i = 0; i < 32; i++) culture.energy.receiveEnergy(2000, false);
                var early = (EarlyPlanter) level.getBlockEntity(origin); var crop = Crops.all().getFirst();
                var altar = (MEInfusionAltar) level.getBlockEntity(smokePos(3));
                for (int i = 2; i < 6; i++) altar.items.setStackInSlot(i, appeng.core.definitions.AEItems.SPEED_CARD.stack());
                early.items.setStackInSlot(0, new ItemStack(crop.getSeedsItem())); early.items.setStackInSlot(1, new ItemStack(Blocks.FARMLAND)); early.water.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
                var drive = (SeedStorage) level.getBlockEntity(origin.offset(1, 0, 0)); drive.items.setStackInSlot(0, new ItemStack(crop.getSeedsItem(), 3)); drive.step();
                player.teleportTo(origin.getX(), origin.getY(), origin.getZ() - 1); early.open(player);
            });
            return;
        }
        if (!(mc.player.containerMenu instanceof MachineMenu menu) || mc.screen == null || menu.state.isEmpty()) return;
        ticks++;
        if (stage == 1 && ticks == 5) {
            mc.getSingleplayerServer().execute(() -> {
                var drive = (SeedStorage) mc.getSingleplayerServer().overworld().getBlockEntity(origin.offset(1, 0, 0));
                var key = appeng.api.stacks.AEItemKey.of(new ItemStack(Crops.all().getFirst().getSeedsItem()));
                drive.getMainNode().getGrid().getStorageService().getInventory().insert(key, 5, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.ofMachine(drive));
            });
        }
        if (stage == 1 && ticks == 10) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.sudabake.appliedagristics.network.Messages.Control(menu.machine.getBlockPos(), 0, "", 0, ItemStack.EMPTY));
        if (stage == 1 && ticks == 15) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.sudabake.appliedagristics.network.Messages.Control(menu.machine.getBlockPos(), 3, "", 0, new ItemStack(Crops.all().getFirst().getSeedsItem())));
        if (stage == 1 && ticks == 20) {
            mc.getSingleplayerServer().execute(() -> {
                var drive = (SeedStorage) mc.getSingleplayerServer().overworld().getBlockEntity(origin.offset(1, 0, 0));
                var crop = Crops.all().getFirst(); var key = appeng.api.stacks.AEItemKey.of(new ItemStack(crop.getSeedsItem()));
                withdrawPassed = drive.count(key) == 0;
                if (withdrawPassed) org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_SMOKE_WITHDRAW_PASSED");
                else org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("CLIENT_SMOKE_WITHDRAW_FAILED remaining={} used={}", drive.count(key), drive.used(key));
                drive.items.setStackInSlot(0, new ItemStack(crop.getSeedsItem(), 8)); drive.step();
                var planter = (NetworkPlanter) mc.getSingleplayerServer().overworld().getBlockEntity(origin.offset(2, 0, 0));
                planterFixture.modules(2,3,true);
                for (int i = 1; i < 6; i++) { drive.items.setStackInSlot(0, new ItemStack(Crops.all().get(i).getSeedsItem())); drive.step(); }
                drive.items.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS)); drive.step();
            });
        }
        if (stage == 2 && ticks == 5) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.sudabake.appliedagristics.network.Messages.Control(menu.machine.getBlockPos(), 1, Crops.all().getFirst().getId().toString(), 12, ItemStack.EMPTY));
        if (stage == 2 && ticks == 10) {
            var rows = menu.state.getList("rows", 10);
            if (rows.size() == 4) expectedScrollId = rows.getCompound(1).getString("id");
            mc.screen.mouseScrolled(mc.screen.width / 2.0, mc.screen.height / 2.0 - 40, 0, -1);
        }
        if (stage == 2 && ticks == 25) {
            var rows = menu.state.getList("rows", 10);
            scrollPassed = menu.page == 1 && !expectedScrollId.isEmpty() && !rows.isEmpty() && rows.getCompound(0).getString("id").equals(expectedScrollId);
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_SCROLL_RESULT {}", scrollPassed);
        }
        if (stage == 2 && ticks == 26) {
            int left = (mc.screen.width - 320) / 2, top = (mc.screen.height - 240) / 2;
            mc.screen.mouseClicked(left + 314, top + 68, 0);
            mc.screen.mouseDragged(left + 314, top + 144, 0, 0, 76);
            mc.screen.mouseReleased(left + 314, top + 144, 0);
            dragPassed = menu.page == Math.max(0, menu.state.getInt("pages") - 1);
        }
        if (stage == 3 && ticks == 27) mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            String[] ids = {"minecraft:cow", "minecraft:pig", "minecraft:wither", "minecraft:ender_dragon"};
            for (int i = 0; i < ids.length; i++) {
                var id = net.minecraft.resources.ResourceLocation.parse(ids[i]);
                player.getInventory().setItem(i, io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_SEED.get(), id));
                player.getInventory().setItem(i + 4, io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_ESSENCE.get(), id));
            }
        });
        if (stage == 3 && ticks == 11) org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
        if (stage == 3 && ticks == 12) Screenshot.grab(mc.gameDirectory, "agr-me-infusion-altar.png", mc.getMainRenderTarget(), message -> {});
        if (stage == 3 && ticks == 15 && mc.screen instanceof io.github.sudabake.appliedagristics.client.AltarScreen screen) mc.screen.mouseClicked(screen.getGuiLeft() - 14, screen.getGuiTop() + 12, 0);
        if (stage == 3 && ticks == 18 && mc.screen instanceof io.github.sudabake.appliedagristics.client.AltarIOScreen screen) {
            mc.screen.mouseClicked(screen.getGuiLeft() + 101, screen.getGuiTop() + 57, 0);
            mc.screen.mouseClicked(screen.getGuiLeft() + 151, screen.getGuiTop() + 113, 0);
        }
        if (stage == 3 && ticks == 25) {
            var sides = menu.state.getIntArray("sides");
            altarPassed = mc.screen instanceof io.github.sudabake.appliedagristics.client.AltarIOScreen && sides.length == 6 && sides[net.minecraft.core.Direction.EAST.ordinal()] == 2 && menu.state.getBoolean("autoOutput") && menu.state.getInt("acceleration") == 4;
        }
        if (stage == 4 && ticks == 5 && mc.screen instanceof io.github.sudabake.appliedagristics.client.CultureScreen) {
            mc.screen.mouseClicked((mc.screen.width - 176) / 2.0 + 150, (mc.screen.height - 193) / 2.0 + 32, 0);
        }
        if (stage == 4 && ticks == 26) {
            if (!(mc.screen instanceof io.github.sudabake.appliedagristics.client.CultureScreen) || !menu.state.getBoolean("autoExtract") || menu.state.getInt("duration") != Balance.CULTURE_TICKS.get() || menu.state.getInt("energy") <= 0)
                altarPassed = false;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
        }
        if ((stage==5 || stage==6) && ticks==12) nativeScreensPassed &= mc.screen instanceof io.github.sudabake.appliedagristics.client.PortScreen && menu.machine.items.getSlots()==16;
        if (stage==5 && ticks==15 && mc.screen instanceof io.github.sudabake.appliedagristics.client.PortScreen screen)
            mc.screen.mouseClicked(screen.getGuiLeft()-14,screen.getGuiTop()+12,0);
        if (stage==5 && ticks==25) nativeScreensPassed &= mc.screen instanceof io.github.sudabake.appliedagristics.client.AltarIOScreen;
        if (stage==7 && ticks==12) nativeScreensPassed &= mc.screen instanceof io.github.sudabake.appliedagristics.client.EssenceVaultScreen && menu.state.getInt("types")==5;
        if (ticks < 30) return;
        ticks = 0;
        String[] names = {"early-planter", "seed-storage", "network-planter", "me-infusion-altar-io", "cell-cultivator", "input-port-io", "output-port", "essence-vault"};
        Screenshot.grab(mc.gameDirectory, "agr-" + names[stage] + ".png", mc.getMainRenderTarget(), message -> org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_SMOKE_SCREENSHOT {}", message.getString()));
        if (++stage >= 8) {
            mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().overworld(); var crop = Crops.all().getFirst();
                var drive = (SeedStorage) level.getBlockEntity(origin.offset(1, 0, 0)); var planter = (NetworkPlanter) level.getBlockEntity(origin.offset(2, 0, 0));
                var task = planter.tasks.get(crop.getId());
                if (drive.count(appeng.api.stacks.AEItemKey.of(new ItemStack(crop.getSeedsItem()))) >= 8 && task != null && task.enabled && task.target == 12 && planter.parallel() == 64 && Math.abs(planter.speed()-io.github.sudabake.appliedagristics.logic.ProductionMath.moduleSpeed(2,Balance.SPEED_MAX.get()))<1e-8 && scrollPassed && withdrawPassed && dragPassed && altarPassed && nativeScreensPassed)
                    { corePassed = true; org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("CLIENT_SMOKE_MENUS_PASSED menus=8 collectAndTaskPackets=true"); }
                else org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("CLIENT_SMOKE_CONTROL_FAILED scroll={} withdraw={} drag={} altar={} nativeScreens={} parallel={} speed={}",scrollPassed,withdrawPassed,dragPassed,altarPassed,nativeScreensPassed,planter.parallel(),planter.speed());
                mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level,6,102,-6,java.util.Set.of(),25,20);
                mc.execute(() -> { mc.player.closeContainer(); scenePending = true; });
            }); return;
        }
        var id = mc.player.getUUID(); var pos = smokePos(stage);
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.teleportTo(pos.getX(), pos.getY(), pos.getZ() - 1); ((MachineEntity) player.level().getBlockEntity(pos)).open(player);
        });
    }
}

