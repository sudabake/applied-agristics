package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.sudabake.appliedagristics.block.BiologicalAltar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.*;

/** Render-only cached projection: never added to a level and never runs entity AI. */
public final class BiologicalAltarRenderer implements BlockEntityRenderer<BiologicalAltar> {
    private final Map<ResourceLocation,Entity> previews=new LinkedHashMap<>() { @Override protected boolean removeEldestEntry(Map.Entry<ResourceLocation,Entity> entry) { return size()>16; } };
    private final Set<ResourceLocation> unsupported=new HashSet<>();
    private net.minecraft.world.level.Level previewLevel;
    public BiologicalAltarRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(BiologicalAltar altar,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        var mc=Minecraft.getInstance();
        var stack=altar.visualItem();
        if(!stack.isEmpty()) {
            pose.pushPose();pose.translate(.5,1.05,.5);pose.mulPose(Axis.YP.rotationDegrees((altar.getLevel().getGameTime()+partial)*2));pose.scale(.6f,.6f,.6f);
            mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,pose,buffer,altar.getLevel(),0);pose.popPose();
        }
        var id=altar.ritualSpecies(); float progress=altar.ritualProgress(partial);
        if(id==null || progress<.75 || unsupported.contains(id)) return;
        if(previewLevel!=altar.getLevel()) { previews.clear();unsupported.clear();previewLevel=altar.getLevel(); }
        try {
            var entity=previews.computeIfAbsent(id,key->BuiltInRegistries.ENTITY_TYPE.get(key).create(altar.getLevel()));
            if(entity==null) return;
            var renderer=mc.getEntityRenderDispatcher().getRenderer(entity);
            var translucent=buffer.getBuffer(RenderType.entityTranslucent(renderer.getTextureLocation(entity)));
            MultiBufferSource ghost=type->new GhostVertex(translucent);
            float scale=(float)Math.min(1,1.6/Math.max(.25,Math.max(entity.getBbHeight(),entity.getBbWidth())));
            pose.pushPose();
            try {
                pose.translate(.5,1,.5);pose.scale(scale,scale,scale);pose.mulPose(Axis.YP.rotationDegrees((altar.getLevel().getGameTime()+partial)*2));
                renderer.render(entity,0,0,pose,ghost,LightTexture.FULL_BRIGHT);
            } finally { pose.popPose(); }
        } catch(RuntimeException e) {
            unsupported.add(id);
            org.slf4j.LoggerFactory.getLogger("AppliedAgristics").warn("Biological hologram unavailable for {}",id,e);
        }
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(BiologicalAltar altar) { return new net.minecraft.world.phys.AABB(altar.getBlockPos()).inflate(4); }
    private record GhostVertex(VertexConsumer delegate) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x,float y,float z) { delegate.addVertex(x,y,z);return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { delegate.setColor(r*3/4,g,b*3/4,a/3);return this; }
        @Override public VertexConsumer setUv(float u,float v) { delegate.setUv(u,v);return this; }
        @Override public VertexConsumer setUv1(int u,int v) { delegate.setUv1(u,v);return this; }
        @Override public VertexConsumer setUv2(int u,int v) { delegate.setUv2(u,v);return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) { delegate.setNormal(x,y,z);return this; }
    }
}
