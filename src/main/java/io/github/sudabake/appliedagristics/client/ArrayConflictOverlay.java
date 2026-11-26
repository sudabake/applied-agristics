package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid=Agristics.ID,value=Dist.CLIENT)
public final class ArrayConflictOverlay {
    private static final RenderType CONFLICT_LINES = RenderType.create("agr_array_conflicts",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,256,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
    private static Messages.BuildConflicts conflicts;
    private static net.minecraft.client.multiplayer.ClientLevel markedLevel;
    private static long start;

    private ArrayConflictOverlay() {}

    public static void show(Messages.BuildConflicts message) {
        var level=Minecraft.getInstance().level;
        if (level==null || !level.dimension().location().equals(message.dimension())) return;
        conflicts=message;
        markedLevel=level;
        start=System.nanoTime();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || conflicts==null) return;
        var minecraft=Minecraft.getInstance();
        long elapsed=(System.nanoTime()-start)/1_000_000;
        if (minecraft.level!=markedLevel || elapsed>=8000) {
            conflicts=null;
            markedLevel=null;
            return;
        }
        if (elapsed%600>=330) return;
        var pose=event.getPoseStack();
        var camera=event.getCamera().getPosition();
        var buffers=minecraft.renderBuffers().bufferSource();
        var lines=buffers.getBuffer(CONFLICT_LINES);
        pose.pushPose();
        pose.translate(-camera.x,-camera.y,-camera.z);
        for (var pos:conflicts.positions())
            LevelRenderer.renderLineBox(pose,lines,new AABB(pos).inflate(0.005),1,0.1f,0.1f,1);
        pose.popPose();
        buffers.endBatch(CONFLICT_LINES);
    }
}
