package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=Agristics.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ArrayKeys {
    public static final KeyMapping STRUCTURE=new KeyMapping("key.appliedagristics.structure",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_U,"key.categories.appliedagristics");
    public static final KeyMapping GUIDE=new KeyMapping("key.appliedagristics.guide",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.appliedagristics");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(STRUCTURE);event.register(GUIDE);ArrayGuide.init(); }
    public static boolean part(String name) { return java.util.Set.of("network_planter","seed_storage","me_network_access","planter_input_port","planter_output_port","essence_vault","planter_frame","cultivation_chamber","planter_speed_module","planter_parallel_module","planter_conversion_module").contains(name); }
    @EventBusSubscriber(modid=Agristics.ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var mc=Minecraft.getInstance(); if(mc.screen!=null || mc.level==null) return;
            boolean structure=STRUCTURE.consumeClick(),guide=GUIDE.consumeClick();
            if(!(mc.hitResult instanceof BlockHitResult hit)) return;
            var id=BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(hit.getBlockPos()).getBlock());
            if(!id.getNamespace().equals(Agristics.ID)||!part(id.getPath())) return;
            if(guide) ArrayGuide.open();
            else if(structure && net.neoforged.fml.ModList.get().isLoaded("jei")) io.github.sudabake.appliedagristics.compat.jei.AgristicsJei.showStructure();
            else if(structure) ArrayGuide.open();
        }
    }
}
