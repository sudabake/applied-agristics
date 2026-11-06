package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.vertex.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.AABB;
import com.mojang.math.Axis;

/** Controller leaf display using the approved sprite frames. */
public final class MachineActivityRenderer implements BlockEntityRenderer<NetworkPlanter> {
    public MachineActivityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(NetworkPlanter machine,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        if (machine.getLevel()==null) return;
        float time=(machine.getLevel().getGameTime()%10800)+partial;
        poses.pushPose(); poses.translate(.5,0,.5);
        poses.mulPose(Axis.YP.rotationDegrees(switch(machine.getBlockState().getValue(PlanterPartBlock.FACING)) { case EAST->270; case SOUTH->180; case WEST->90; default->0; }));
        poses.translate(-.5,0,-.5);
        boolean formed=machine.getBlockState().getValue(PlanterPartBlock.FORMED);
        boolean active=formed && machine.poweredVisual();
        int frame=!active?0:machine.workingVisual()?2+(int)(time/4)%24:1;
        var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("appliedagristics","textures/block/network_planter_screen.png");
        var image=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        textured(image,poses,3f/16,3f/16,13f/16,13f/16,-.002f,0,frame/26f,1,(frame+1)/26f,light);
        poses.popPose();
    }
    private static void textured(VertexConsumer out,PoseStack pose,float x0,float y0,float x1,float y1,float z,float u0,float v0,float u1,float v1,int light) {
        var m=pose.last().pose();
        out.addVertex(m,x0,y0,z).setColor(-1).setUv(u0,v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x0,y1,z).setColor(-1).setUv(u0,v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x1,y1,z).setColor(-1).setUv(u1,v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x1,y0,z).setColor(-1).setUv(u1,v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
    }
    @Override public AABB getRenderBoundingBox(NetworkPlanter machine) { return new AABB(machine.getBlockPos()); }
}

