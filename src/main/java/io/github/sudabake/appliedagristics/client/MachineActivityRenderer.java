package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.vertex.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.AABB;
import com.mojang.math.Axis;

/** Controller screen and local culture bubbles; no array-wide effects or extra entities. */
public final class MachineActivityRenderer<T extends MachineEntity> implements BlockEntityRenderer<T> {
    public MachineActivityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(T machine,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        if (machine.getLevel()==null) return;
        if (!(machine instanceof NetworkPlanter) && !machine.workingVisual()) return;
        VertexConsumer buffer;
        float time=(machine.getLevel().getGameTime()%10800)+partial;
        poses.pushPose(); poses.translate(.5,0,.5);
        poses.mulPose(Axis.YP.rotationDegrees(switch(machine.getBlockState().getValue(PlanterPartBlock.FACING)) { case EAST->270; case SOUTH->180; case WEST->90; default->0; }));
        poses.translate(-.5,0,-.5);
        if (machine instanceof NetworkPlanter planter) {
            boolean formed=machine.getBlockState().getValue(PlanterPartBlock.FORMED);
            boolean active=formed && planter.poweredVisual();
            int frame=machine.workingVisual() && active ? (int)(time/4)%24 : 0;
            String state=!active?"off":machine.workingVisual()?"working":"idle";
            var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("appliedagristics","textures/block/network_planter_"+state+".png");
            var image=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            float height=state.equals("working")?768:32;
            textured(image,poses,7f/32,7f/32,25f/32,25f/32,-.002f,7f/32,(frame*32+7)/height,25f/32,(frame*32+25)/height,light);
        } else if (machine instanceof CellCultivator) {
            buffer=buffers.getBuffer(RenderType.lightning());
            poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(180));poses.translate(-.5,0,-.5);
            for (int i=0;i<3;i++) {
                float phase=((time+i*31)%96)/96;
                float x=.40f+i*.11f, y=.43f+phase*.55f, r=.012f;
                int alpha=(int)(130*Math.sin(phase*Math.PI));
                rect(buffer,poses,x-r,y-r,x+r,y-r+.006f,.61f,150,220,235,alpha);
                rect(buffer,poses,x-r,y+r-.006f,x+r,y+r,.61f,150,220,235,alpha);
                rect(buffer,poses,x-r,y-r,x-r+.006f,y+r,.61f,150,220,235,alpha);
                rect(buffer,poses,x+r-.006f,y-r,x+r,y+r,.61f,150,220,235,alpha);
            }
        }
        poses.popPose();
    }
    private static void textured(VertexConsumer out,PoseStack pose,float x0,float y0,float x1,float y1,float z,float u0,float v0,float u1,float v1,int light) {
        var m=pose.last().pose();
        out.addVertex(m,x0,y0,z).setColor(-1).setUv(u0,v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x0,y1,z).setColor(-1).setUv(u0,v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x1,y1,z).setColor(-1).setUv(u1,v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
        out.addVertex(m,x1,y0,z).setColor(-1).setUv(u1,v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,-1);
    }
    private static void rect(VertexConsumer out,PoseStack pose,float x0,float y0,float x1,float y1,float z,int r,int g,int b,int a) {
        var m=pose.last().pose();
        out.addVertex(m,x0,y0,z).setColor(r,g,b,a); out.addVertex(m,x0,y1,z).setColor(r,g,b,a);
        out.addVertex(m,x1,y1,z).setColor(r,g,b,a); out.addVertex(m,x1,y0,z).setColor(r,g,b,a);
    }
    @Override public AABB getRenderBoundingBox(T machine) { return new AABB(machine.getBlockPos()).inflate(machine instanceof CellCultivator ? .4 : 0); }
}

