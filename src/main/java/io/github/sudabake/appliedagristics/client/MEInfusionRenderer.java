package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.sudabake.appliedagristics.block.MEInfusionAltar;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

/** Invoke the actual MA renderer: same floating center item and eight pedestal placement ghosts. */
public final class MEInfusionRenderer implements BlockEntityRenderer<MEInfusionAltar> {
    private final com.blakebr0.mysticalagriculture.client.tesr.InfusionAltarRenderer nativeRenderer;
    public MEInfusionRenderer(BlockEntityRendererProvider.Context context) { nativeRenderer=new com.blakebr0.mysticalagriculture.client.tesr.InfusionAltarRenderer(context); }
    @Override public void render(MEInfusionAltar altar,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay) { nativeRenderer.render(altar.visualView(),partial,pose,buffer,light,overlay); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(MEInfusionAltar altar) { return new net.minecraft.world.phys.AABB(altar.getBlockPos()).inflate(4); }
}
