package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.logic.CropSources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid=Agristics.ID,value=Dist.CLIENT)
public final class ClientCropReload {
    @SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) { CropSources.invalidate(); }
}
