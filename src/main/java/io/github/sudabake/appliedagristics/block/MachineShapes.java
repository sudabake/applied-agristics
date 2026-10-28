package io.github.sudabake.appliedagristics.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Generated from the installed model geometry by tools/install_art_resources.py. */
public final class MachineShapes {
    private static final Map<String,VoxelShape[]> SHAPES=new HashMap<>();
    static {
        shape0();
        shape1();
        shape2();
        shape3();
        shape4();
        shape5();
        shape6();
        shape7();
        shape8();
        shape9();
        shape10();
        shape11();
        shape12();
        shape13();
        shape14();
    }
    private static void shape0() { add("network_planter",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape1() { add("seed_storage",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape2() { add("me_network_access",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape3() { add("planter_input_port",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape4() { add("planter_output_port",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape5() { add("essence_vault",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape6() { add("planter_frame",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape7() { add("cultivation_chamber",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape8() { add("planter_speed_module",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape9() { add("planter_parallel_module",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape10() { add("planter_conversion_module",new double[][]{{0,0,0,16,16,16}}); }
    private static void shape11() { add("early_planter",new double[][]{{0,0,0,16,1,16},{1,1,1,15,6,15},{2,1,15,14,9,16},{2,1,0,14,9,1},{0,1,2,1,9,14},{15,1,2,16,9,14},{0,1,0,2,10,2},{0,1,14,2,10,16},{14,1,0,16,10,2},{14,1,14,16,10,16}}); }
    private static void shape12() { add("cell_cultivator",new double[][]{{0,0,0,16,6,16.55},{1.09,5.9,1.09,14.91,20.65,14.91},{14.1,5.7,11.5,16.6,8.4,12.9}}); }
    private static void shape13() { add("me_infusion_altar",new double[][]{{0,0,0,16,8,16},{3,13,3,13,14,13},{6,10,6,10,11,10},{5,11,5,11,13,11},{2,13.5,4,3,14.5,12},{13,13.5,4,14,14.5,12},{4,13.5,2,12,14.5,3},{4,13.5,13,12,14.5,14},{0.5,8,4,4,10,12},{12,8,4,15.5,10,12},{2,8,12,14,10,15.5},{2,8,0.5,14,10,4},{14,8,2,15.5,10,4},{14,8,12,15.5,10,14},{0.5,8,12,2,10,14},{0.5,8,2,2,10,4},{0.25,8,14,2,9,15.75},{14,8,14,15.75,9,15.75},{14,8,0.25,15.75,9,2},{0.25,8,0.25,2,9,2},{5,8,5,11,10,11}}); }
    private static void shape14() { add("biological_altar",new double[][]{{0,0,0,16,8,16},{3,13,3,13,14,13},{6,10,6,10,11,10},{5,11,5,11,13,11},{2,13.5,4,3,14.5,12},{13,13.5,4,14,14.5,12},{4,13.5,2,12,14.5,3},{4,13.5,13,12,14.5,14},{0.5,8,4,4,10,12},{12,8,4,15.5,10,12},{2,8,12,14,10,15.5},{2,8,0.5,14,10,4},{14,8,2,15.5,10,4},{14,8,12,15.5,10,14},{0.5,8,12,2,10,14},{0.5,8,2,2,10,4},{0.25,8,14,2,9,15.75},{14,8,14,15.75,9,15.75},{14,8,0.25,15.75,9,2},{0.25,8,0.25,2,9,2},{5,8,5,11,10,11}}); }
    private static void add(String name,double[][] boxes) {
        var shapes=new VoxelShape[4];
        for (int turn=0;turn<4;turn++) {
            VoxelShape shape=Shapes.empty();
            for (var box:boxes) {
                double x=box[0],z=box[2],X=box[3],Z=box[5];
                for (int n=0;n<turn;n++) { double previous=x; x=16-Z; Z=X; X=16-z; z=previous; }
                shape=Shapes.or(shape,net.minecraft.world.level.block.Block.box(x,box[1],z,X,box[4],Z));
            }
            shapes[turn]=shape.optimize();
        }
        SHAPES.put(name,shapes);
    }
    public static VoxelShape get(String name,Direction facing,boolean formed) {
        if (formed && !name.equals("early_planter") && !name.equals("cell_cultivator") && !name.equals("me_infusion_altar")) return Shapes.block();
        var shapes=SHAPES.get(name); if (shapes==null) return Shapes.block();
        return shapes[switch(facing) { case SOUTH -> 0; case WEST -> 1; case NORTH -> 2; default -> 3; }];
    }
    private MachineShapes() {}
}
