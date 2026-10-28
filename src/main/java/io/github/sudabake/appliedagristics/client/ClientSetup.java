package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Agristics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(Agristics.THROWN_CULTURE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new); event.registerBlockEntityRenderer(Agristics.BIO_ALTAR_BE.get(),BiologicalAltarRenderer::new); event.registerBlockEntityRenderer(Agristics.EARLY_BE.get(),EarlyPlanterRenderer::new); event.registerBlockEntityRenderer(Agristics.INFUSION_BE.get(),MEInfusionRenderer::new); event.registerBlockEntityRenderer(Agristics.NETWORK_BE.get(),MachineActivityRenderer::new); event.registerBlockEntityRenderer(Agristics.CULTIVATOR_BE.get(),MachineActivityRenderer::new); }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) { event.register(EarlyPlanterRenderer.BIO_MODEL); }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(Agristics.MENU.get(), ClientSetup::createScreen); }
    private static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<io.github.sudabake.appliedagristics.menu.MachineMenu> createScreen(io.github.sudabake.appliedagristics.menu.MachineMenu menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.ItemPort) return new PortScreen(menu,inventory,title);
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.EssenceVault) return new EssenceVaultScreen(menu,inventory,title);
        return menu.machine instanceof io.github.sudabake.appliedagristics.block.CellCultivator ? new CultureScreen(menu, inventory, title) : menu.machine instanceof io.github.sudabake.appliedagristics.block.AltarControls ? new AltarScreen(menu, inventory, title) : new MachineScreen(menu, inventory, title);
    }
}
