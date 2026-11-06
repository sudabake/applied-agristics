package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioCropEntity;
import io.github.sudabake.appliedagristics.block.PlanterOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Disposable-world-only visual and real baked-quad checks; excluded from the JAR. */
public final class NextPhaseClientChecks {
    private static int ticks;
    private static boolean reloaded;
    private static int clearFrames;
    public static boolean passed;
    public static boolean tick(Minecraft mc) {
        ticks++;
        if(ticks==1) mc.getSingleplayerServer().execute(()-> {
            var level=mc.getSingleplayerServer().overworld();
            for(int x=-2;x<=25;x++) for(int z=8;z<=19;z++) level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
            for(int age=0;age<8;age++) {
                var p=new BlockPos(age,100,10);level.setBlockAndUpdate(p.below(),Blocks.FARMLAND.defaultBlockState());
                level.setBlockAndUpdate(p,Agristics.BIO_CROP.get().defaultBlockState().setValue(BlockStateProperties.AGE_7,age));
                ((BioCropEntity)level.getBlockEntity(p)).setIdentity(ResourceLocation.parse("minecraft:cow"));
            }
            for(int x=12;x<17;x++) for(int y=100;y<103;y++) level.setBlockAndUpdate(new BlockPos(x,y,10),x==13 && y==102?Blocks.AIR.defaultBlockState():Agristics.PLANTER_FRAME.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(12,101,10),Agristics.SPEED_MODULE.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(13,101,10),Agristics.PARALLEL_MODULE.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(14,101,10),PlanterOrientation.withFacing(Agristics.NETWORK_ACCESS.get().defaultBlockState(),Direction.SOUTH));
            level.setBlockAndUpdate(new BlockPos(15,101,10),Agristics.CONVERSION_MODULE.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(16,101,10),PlanterOrientation.withFacing(Agristics.NETWORK.get().defaultBlockState(),Direction.SOUTH));
            for(var side:Direction.values()) level.setBlockAndUpdate(new BlockPos(12+side.ordinal(),100,13),PlanterOrientation.withFacing(Agristics.NETWORK_ACCESS.get().defaultBlockState(),side));
            mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level,3.5,100,14.5,java.util.Set.of(),180,16);
        });
        if(ticks==60) Screenshot.grab(mc.gameDirectory,"agr-next-plants-and-jade.png",mc.getMainRenderTarget(),message->{});
        if(ticks==70) mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(mc.getSingleplayerServer().overworld(),18,102,17,java.util.Set.of(),150,14));
        if(ticks==120) {
            try {
                int quads=0;var random=RandomSource.create(19);
                for(int x=12;x<17;x++) for(int y=100;y<103;y++) {
                    var p=new BlockPos(x,y,10);var state=mc.level.getBlockState(p);if(state.isAir()) continue;
                    var model=mc.getBlockRenderer().getBlockModel(state);
                    if(model==mc.getModelManager().getMissingModel()) throw new IllegalStateException("Missing ordinary block model");
                    var data=model.getModelData(mc.level,p,state,ModelData.EMPTY);
                    for(var side:Direction.values()) for(var quad:model.getQuads(state,side,random,data,RenderType.cutout())) {
                        if(quad.getSprite().contents().name().getPath().contains("missing")) throw new IllegalStateException("Missing casing sprite");
                        int[] vertices=quad.getVertices();int stride=vertices.length/4;
                        for(int corner=0;corner<4;corner++) for(int axis=0;axis<3;axis++) {
                            float value=Float.intBitsToFloat(vertices[corner*stride+axis]);
                            if(!Float.isFinite(value) || value<-.001 || value>1.001) throw new IllegalStateException("Invalid baked casing vertex");
                        }
                        quads++;
                    }
                }
                for(var side:Direction.values()) {
                    var state=mc.level.getBlockState(new BlockPos(12+side.ordinal(),100,13));
                    if(PlanterOrientation.front(state)!=side) throw new IllegalStateException("Six-way client state did not synchronize");
                }
                passed=quads>0 && moduleItemModels(mc) && ordinaryFunctionalFaces(mc);com.mojang.logging.LogUtils.getLogger().info("NEXT_PHASE_CLIENT_PASSED sixDirections=true ordinaryQuads={} finiteVertices=true",quads);
            } catch(RuntimeException exception) { passed=false;com.mojang.logging.LogUtils.getLogger().error("NEXT_PHASE_CLIENT_FAILED",exception); }
            Screenshot.grab(mc.gameDirectory,"agr-next-connected-shell-ports.png",mc.getMainRenderTarget(),message->{});
        }
        if(ticks==125) mc.getSingleplayerServer().execute(()-> {
            var level=mc.getSingleplayerServer().overworld();
            level.setBlockAndUpdate(new BlockPos(15,101,10),Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(16,101,11),Agristics.PLANTER_FRAME.get().defaultBlockState());
        });
        if(ticks==145) Screenshot.grab(mc.gameDirectory,"agr-next-connected-shell-refresh.png",mc.getMainRenderTarget(),message->{});
        if(ticks==150) mc.reloadResourcePacks().whenComplete((ignored,error)-> {
            reloaded=true;
            passed &= error==null;
            com.mojang.logging.LogUtils.getLogger().info("NEXT_PHASE_RESOURCE_RELOAD completed={} error={}",error==null,error);
        });
        if(ticks>=165 && reloaded && mc.getOverlay()==null && ++clearFrames>=5) {
            passed &= moduleItemModels(mc);
            passed &= ordinaryFunctionalFaces(mc);
            passed &= mc.getBlockRenderer().getBlockModel(mc.level.getBlockState(new BlockPos(14,101,10)))!=mc.getModelManager().getMissingModel();
            Screenshot.grab(mc.gameDirectory,"agr-next-connected-shell-reloaded.png",mc.getMainRenderTarget(),message->{});
            return true;
        }
        return false;
    }
    private static boolean ordinaryFunctionalFaces(Minecraft mc) {
        var modules=java.util.Set.of("planter_speed_module","planter_parallel_module","planter_conversion_module");
        var names=java.util.List.of("network_planter","seed_storage","me_network_access",
                "planter_input_port","planter_output_port","essence_vault",
                "planter_speed_module","planter_parallel_module","planter_conversion_module");
        int checked=0;
        for(var name:names) {
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Agristics.ID,name));
            for(boolean formed:new boolean[]{false,true}) for(int cell=0;cell<(formed?36:1);cell++) {
                for(var facing:PlanterOrientation.property(block.defaultBlockState()).getPossibleValues()) {
                    var state=PlanterOrientation.withFacing(block.defaultBlockState(),facing)
                            .setValue(io.github.sudabake.appliedagristics.block.PlanterPartBlock.FORMED,formed)
                            .setValue(io.github.sudabake.appliedagristics.block.PlanterPartBlock.CELL,cell);
                    var model=mc.getBlockRenderer().getBlockModel(state);
                    for(var side:Direction.values()) {
                        var quads=model.getQuads(state,side,RandomSource.create(19),ModelData.EMPTY,null);
                        boolean front=modules.contains(name) || side==PlanterOrientation.front(state);
                        String expected=front?"block/"+name+(modules.contains(name)?"":"_front"):"block/planter_frame";
                        if(quads.size()!=1 || !quads.getFirst().getSprite().contents().name().getPath().equals(expected))
                            throw new IllegalStateException("Full-face material mismatch: "+name+" "+side+" formed="+formed+" cell="+cell+" facing="+facing);
                        var quad=quads.getFirst();var sprite=quad.getSprite();int[] vertices=quad.getVertices();int stride=vertices.length/4;
                        float minU=Float.POSITIVE_INFINITY,maxU=Float.NEGATIVE_INFINITY;
                        float minV=Float.POSITIVE_INFINITY,maxV=Float.NEGATIVE_INFINITY;
                        for(int vertex=0;vertex<4;vertex++) {
                            float u=Float.intBitsToFloat(vertices[vertex*stride+4]);
                            float v=Float.intBitsToFloat(vertices[vertex*stride+5]);
                            minU=Math.min(minU,u);maxU=Math.max(maxU,u);minV=Math.min(minV,v);maxV=Math.max(maxV,v);
                        }
                        if(maxU-minU<(sprite.getU1()-sprite.getU0())*.95f || maxV-minV<(sprite.getV1()-sprite.getV0())*.95f)
                            throw new IllegalStateException("Full-face UV cropped: "+name);
                        checked++;
                    }
                }
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("ORDINARY_FUNCTIONAL_FACES_PASSED faces={} formedAndUnformed=true full16px=true allCells=true",checked);
        return checked>0;
    }
    private static boolean moduleItemModels(Minecraft mc) {
        for (var block : java.util.List.of(Agristics.SPEED_MODULE.get(), Agristics.PARALLEL_MODULE.get(), Agristics.CONVERSION_MODULE.get())) {
            var stack = new net.minecraft.world.item.ItemStack(block);
            var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
            var sprites = new java.util.HashSet<String>();
            var random = RandomSource.create(19);
            for (var quad : model.getQuads(null, null, random)) sprites.add(quad.getSprite().contents().name().toString());
            for (var side : Direction.values())
                for (var quad : model.getQuads(null, side, random)) sprites.add(quad.getSprite().contents().name().toString());
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
            if (sprites.size()!=1 || !sprites.contains("appliedagristics:block/" + name)) return false;
        }
        com.mojang.logging.LogUtils.getLogger().info("MODULE_ITEM_MODELS_PASSED singleTexture=true sixFaces=true");
        return true;
    }

    private NextPhaseClientChecks() {}
}
