package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.logic.CropSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=Agristics.ID,value=Dist.CLIENT)
public final class ClientCropReload {
    @SubscribeEvent public static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) { CropSources.invalidate(); }
}
