package io.github.sudabake.appliedagristics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.EarlyPlanter;
import io.github.sudabake.appliedagristics.logic.Harvests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Mature display only. No random ticks, entities, growth animation or harvest simulation. */
public final class EarlyPlanterRenderer implements BlockEntityRenderer<EarlyPlanter> {
    public static final ModelResourceLocation BIO_MODEL=ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Agristics.ID,"block/bio_plant"));
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher renderer;
    public EarlyPlanterRenderer(BlockEntityRendererProvider.Context context) { renderer=context.getBlockRenderDispatcher(); }
    @Override public void render(EarlyPlanter machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var soil=machine.items.getStackInSlot(1);
        if (soil.getItem() instanceof net.minecraft.world.item.BlockItem item) {
            pose.pushPose(); pose.translate(1.0/16,6.0/16,1.0/16); pose.scale(14f/16,2f/16,14f/16);
            var ground=item.getBlock().defaultBlockState();
            if (ground.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)) pose.scale(1,16f/15,1);
            renderer.renderSingleBlock(ground,pose,buffers,light,overlay); pose.popPose();
        }
        var seed=machine.items.getStackInSlot(0); if (seed.isEmpty() || soil.isEmpty()) return;
        var state=io.github.sudabake.appliedagristics.logic.CropSources.display(machine.getLevel(),seed); boolean bio=BioData.seed(seed);
        if (state==null && !bio) return;
        pose.pushPose(); pose.translate(.14,8.0/16,.14); pose.scale(.72f,.72f,.72f);
        if (bio) {
            int color=BioColors.color(seed,0);
            var model=Minecraft.getInstance().getModelManager().getModel(BIO_MODEL);
            renderer.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),Blocks.WHEAT.defaultBlockState(),model,
                (color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,light,overlay);
        } else renderer.renderSingleBlock(state,pose,buffers,light,overlay);
        pose.popPose();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(EarlyPlanter machine) { return new net.minecraft.world.phys.AABB(machine.getBlockPos()).expandTowards(0,1,0); }
}
