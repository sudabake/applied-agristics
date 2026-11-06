package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.PlanterOrientation;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Isolated preview pack only. This class is excluded from the distributed JAR. */
public final class CultivatorModelChecks {
    private static final BlockPos POSITION = new BlockPos(80,110,80);
    private static int ticks;
    private static int captures;
    private static boolean passed=true;
    private static boolean reloading;
    private static boolean reloaded;

    private static void checkLayers(Minecraft mc) {
        var state=mc.level.getBlockState(POSITION);
        if(!state.is(Agristics.ME_BIO.get())) throw new IllegalStateException("Preview machine did not load");
        var model=mc.getBlockRenderer().getBlockModel(state);
        var random=RandomSource.create(12);
        var types=model.getRenderTypes(state,random,ModelData.EMPTY);
        int opaque=model.getQuads(state,null,random,ModelData.EMPTY,RenderType.solid()).size();
        int transparent=model.getQuads(state,null,random,ModelData.EMPTY,RenderType.translucent()).size();
        if(!types.contains(RenderType.solid()) || !types.contains(RenderType.translucent())
                || opaque<=180 || transparent!=30)
            throw new IllegalStateException("Unexpected material passes: opaque="+opaque+" transparent="+transparent);
        var item=mc.getItemRenderer().getModel(new ItemStack(Agristics.ME_BIO.get()),mc.level,mc.player,0);
        if(item.getRenderPasses(new ItemStack(Agristics.ME_BIO.get()),false).size()!=3)
            throw new IllegalStateException("Inventory model did not preserve three rendering passes");
        com.mojang.logging.LogUtils.getLogger().info("CULTIVATOR_LAYERS_PASSED opaqueQuads={} transparentQuads={} itemPasses=3",opaque,transparent);
    }

    public static boolean tick(Minecraft mc) {
        ticks++;
        try {
            if(ticks==1) {
                mc.player.closeContainer();
                mc.options.hideGui=true;
                mc.options.pauseOnLostFocus=false;
                mc.getSingleplayerServer().execute(()-> {
                    var level=mc.getSingleplayerServer().overworld();
                    for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) {
                        level.setBlockAndUpdate(POSITION.offset(x,-2,z),Blocks.SMOOTH_STONE.defaultBlockState());
                        for(int y=-1;y<=4;y++) level.setBlockAndUpdate(POSITION.offset(x,y,z),Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(POSITION,PlanterOrientation.withFacing(Agristics.ME_BIO.get().defaultBlockState(),Direction.SOUTH));
                    level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                    player.teleportTo(level,80.5,109.7,83,java.util.Set.of(),180,14);
                });
            }
            if(ticks==50) checkLayers(mc);
            for(int view=0;view<8;view++) {
                if(ticks==60+view*20) {
                    double angle=view*Math.PI/4;
                    mc.getSingleplayerServer().execute(()-> {
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        player.teleportTo(player.serverLevel(),80.5+Math.sin(angle)*2.4,109.7,
                                80.5+Math.cos(angle)*2.4,java.util.Set.of(),(float)(180-Math.toDegrees(angle)),14);
                    });
                }
                if(ticks==72+view*20) {
                    String filename="cultivator-view-"+view+".png";
                    Screenshot.grab(mc.gameDirectory,filename,mc.getMainRenderTarget(),message-> {
                        int green=0;
                        try {
                            var picture=ImageIO.read(new java.io.File(mc.gameDirectory,"screenshots/"+filename));
                            for(int x=picture.getWidth()/5;x<picture.getWidth()*4/5;x++)
                                for(int y=picture.getHeight()/8;y<picture.getHeight()*7/8;y++) {
                                    int color=picture.getRGB(x,y),r=color>>16&255,g=color>>8&255,b=color&255;
                                    if(g>65 && g>r*1.1 && g>b*1.18) green++;
                                }
                        } catch(IOException exception) { green=-1; }
                        final int count=green;
                        mc.execute(()-> {
                            passed &= count>30;captures++;
                            com.mojang.logging.LogUtils.getLogger().info("CULTIVATOR_VIEW_CAPTURE {} greenPixels={}",filename,count);
                        });
                    });
                }
            }
            if(captures==8 && !reloading) {
                reloading=true;
                mc.reloadResourcePacks().whenComplete((ignored,error)->mc.execute(()-> {passed &= error==null;reloaded=true;}));
            }
            if(reloaded && mc.getOverlay()==null) {
                checkLayers(mc);
                if(passed) com.mojang.logging.LogUtils.getLogger().info("CLIENT_SMOKE_PASSED cultivatorClosedCells=true eightViews=true resourceReload=true");
                else com.mojang.logging.LogUtils.getLogger().error("CULTIVATOR_MODEL_FAILED captures={}",captures);
                return true;
            }
        } catch(RuntimeException exception) {
            passed=false;
            com.mojang.logging.LogUtils.getLogger().error("CULTIVATOR_MODEL_FAILED",exception);
            return true;
        }
        return false;
    }

    public static boolean passed() { return passed && captures==8 && reloaded; }

    private CultivatorModelChecks() {}
}
