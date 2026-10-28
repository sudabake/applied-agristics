package io.github.sudabake.appliedagristics.multiblock;

import java.util.function.IntFunction;

/** Server layout rules, independent of Minecraft rendering and world access. */
public final class PlanterLayout {
    public static final int WIDTH = 3, DEPTH = 3, HEIGHT = 4;
    public static final int MODULE_LIMIT = 4;
    public enum Role { CONTROLLER, SEEDS, ACCESS, INPUT, OUTPUT, VAULT, FRAME, CHAMBER, SPEED, PARALLEL, CONVERSION, OTHER }
    public record Result(boolean formed, String status, int speed, int parallel, int conversion) {}

    public static int index(int x, int y, int z) { return (y * DEPTH + z) * WIDTH + x; }
    public static boolean interior(int x, int y, int z) { return x == 1 && z == 1 && y > 0 && y < HEIGHT - 1; }
    /** A valid fully upgraded example shared by the JEI scene and the guide. */
    public static Role[] example() {
        var cells=new Role[WIDTH*DEPTH*HEIGHT]; java.util.Arrays.fill(cells,Role.FRAME);
        cells[index(1,1,0)]=Role.CONTROLLER; cells[index(2,1,0)]=Role.ACCESS;
        cells[index(0,1,0)]=Role.INPUT; cells[index(1,0,0)]=Role.OUTPUT;
        cells[index(0,0,0)]=Role.SEEDS; cells[index(2,0,0)]=Role.VAULT;
        cells[index(1,1,1)]=Role.CHAMBER; cells[index(1,2,1)]=Role.CHAMBER;
        int n=0;
        for(int y=0;y<2;y++) for(int z=0;z<3;z++) for(int x=0;x<3;x++)
            if(cells[index(x,y,z)]==Role.FRAME) {
                if(n<4) cells[index(x,y,z)]=Role.SPEED;
                else if(n<8) cells[index(x,y,z)]=Role.PARALLEL;
                else if(n==8) cells[index(x,y,z)]=Role.CONVERSION;
                n++;
            }
        return cells;
    }
    public static Result validate(IntFunction<Role> cells) {
        int input = 0, output = 0, seeds=0,vault=0, access = 0, speed = 0, parallel = 0, conversion = 0;
        for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) for (int x = 0; x < WIDTH; x++) {
            var role = cells.apply(index(x, y, z));
            if (x == 1 && y == 1 && z == 0) {
                if (role != Role.CONTROLLER) return invalid("invalid_controller");
            } else if (interior(x, y, z)) {
                if (role != Role.CHAMBER) return invalid("missing_chamber");
            } else {
                switch (role) {
                    case SEEDS -> seeds++;
                    case VAULT -> vault++;
                    case ACCESS -> access++;
                    case INPUT -> input++;
                    case OUTPUT -> output++;
                    case SPEED -> speed++;
                    case PARALLEL -> parallel++;
                    case CONVERSION -> conversion++;
                    case FRAME -> { }
                    default -> { return invalid("invalid_shell"); }
                }
            }
        }
        if (input+seeds == 0) return invalid("missing_input_port");
        if (output+vault == 0) return invalid("missing_output_port");
        if (access != 1) return invalid(access == 0 ? "missing_network_access" : "multiple_network_access");
        if (speed > MODULE_LIMIT) return invalid("too_many_speed_modules");
        if (parallel > MODULE_LIMIT) return invalid("too_many_parallel_modules");
        return new Result(true, "formed", speed, parallel, conversion);
    }
    private static Result invalid(String status) { return new Result(false, status, 0, 0, 0); }
    private PlanterLayout() {}
}
