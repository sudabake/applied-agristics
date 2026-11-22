package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;

@EventBusSubscriber(modid = Agristics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Agristics.EARLY_BE.get(),EarlyPlanterRenderer::new); event.registerBlockEntityRenderer(Agristics.INFUSION_BE.get(),MEInfusionRenderer::new); event.registerBlockEntityRenderer(Agristics.NETWORK_BE.get(),MachineActivityRenderer::new); }
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) { event.register(EarlyPlanterRenderer.BIO_MODEL); }
    @SubscribeEvent public static void screens(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(Agristics.MENU.get(), ClientSetup::createScreen)); }
    private static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<io.github.sudabake.appliedagristics.menu.MachineMenu> createScreen(io.github.sudabake.appliedagristics.menu.MachineMenu menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.SeedStorage) return new SeedStorageScreen(menu,inventory,title);
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.MEBioCultivator) return new BioCultivatorScreen(menu,inventory,title);
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.ItemPort) return new PortScreen(menu,inventory,title);
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.EssenceVault) return new EssenceVaultScreen(menu,inventory,title);
        return menu.machine instanceof io.github.sudabake.appliedagristics.block.AltarControls ? new AltarScreen(menu, inventory, title) : new MachineScreen(menu, inventory, title);
    }
}
