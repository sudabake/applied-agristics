package io.github.sudabake.appliedagristics.multiblock;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.sudabake.appliedagristics.multiblock.PlanterLayout.*;

class PlanterLayoutTest {
    private Role[] valid() {
        var cells=new Role[WIDTH*DEPTH*HEIGHT]; Arrays.fill(cells,Role.FRAME);
        cells[index(1,1,0)]=Role.CONTROLLER;
        cells[index(1,1,1)]=Role.CHAMBER; cells[index(1,2,1)]=Role.CHAMBER;
        cells[index(0,1,0)]=Role.INPUT; cells[index(2,1,0)]=Role.ACCESS;
        cells[index(1,0,0)]=Role.OUTPUT;
        return cells;
    }
    @Test void electricBlastFurnaceProportionsAndRequiredParts() {
        assertEquals(36,WIDTH*DEPTH*HEIGHT); assertEquals(4,HEIGHT);
        var cells=valid(); assertTrue(validate(i->cells[i]).formed());
        cells[index(0,1,0)]=Role.FRAME; assertEquals("missing_input_port",validate(i->cells[i]).status());
        cells[index(0,1,0)]=Role.INPUT; cells[index(2,1,0)]=Role.FRAME;
        assertEquals("missing_network_access",validate(i->cells[i]).status());
    }
    @Test void rejectsExtraPortsWrongInteriorAndExtraController() {
        var cells=valid(); cells[index(0,0,0)]=Role.ACCESS;
        assertEquals("multiple_network_access",validate(i->cells[i]).status());
        cells[index(0,0,0)]=Role.FRAME; cells[index(1,2,1)]=Role.FRAME;
        assertEquals("missing_chamber",validate(i->cells[i]).status());
        cells[index(1,2,1)]=Role.CHAMBER; cells[index(0,0,0)]=Role.CONTROLLER;
        assertEquals("invalid_shell",validate(i->cells[i]).status());
    }
    @Test void countsPhysicalModulesAndAllowsAdditionalSeedBays() {
        var cells=valid(); cells[index(0,0,0)]=Role.SPEED; cells[index(0,1,2)]=Role.SPEED;
        cells[index(2,0,0)]=Role.PARALLEL; cells[index(0,0,1)]=Role.CONVERSION; cells[index(2,0,1)]=Role.SEEDS;
        var result=validate(i->cells[i]); assertTrue(result.formed());
        assertEquals(2,result.speed()); assertEquals(1,result.parallel()); assertEquals(1,result.conversion());
    }
    @Test void bulkStoragesAreOptionalLateGameParts() {
        var cells=valid(); assertTrue(validate(i->cells[i]).formed());
        cells[index(0,0,0)]=Role.SEEDS; cells[index(2,0,0)]=Role.VAULT;
        assertTrue(validate(i->cells[i]).formed());
        cells[index(1,0,0)]=Role.FRAME;
        assertTrue(validate(i->cells[i]).formed(),"Essence vault replaces the finite output port");
        cells[index(0,1,0)]=Role.FRAME;
        assertTrue(validate(i->cells[i]).formed(),"Seed bay replaces the finite input port");
        cells[index(2,0,0)]=Role.FRAME;
        assertEquals("missing_output_port",validate(i->cells[i]).status());
    }
    @Test void everyShellCellIsRequired() {
        var cells=valid();
        for (int i=0;i<cells.length;i++) {
            var original=cells[i]; cells[i]=Role.OTHER;
            assertFalse(validate(index->cells[index]).formed(),"Missing cell "+i);
            cells[i]=original;
        }
    }
    @Test void upperShellIsFixedAndUpgradeCountsAreLimited() {
        var upper=valid(); upper[index(0,2,0)]=Role.SPEED;
        assertEquals("fixed_upper_shell",validate(i->upper[i]).status());
        var check=valid();
        int placed=0;
        for(int y=0;y<2;y++) for(int z=0;z<3;z++) for(int x=0;x<3;x++)
            if(check[index(x,y,z)]==Role.FRAME && placed++<5) check[index(x,y,z)]=Role.SPEED;
        assertEquals("too_many_speed_modules",validate(i->check[i]).status());
        for(int i=0;i<check.length;i++) if(check[i]==Role.SPEED) check[i]=Role.PARALLEL;
        assertEquals("too_many_parallel_modules",validate(i->check[i]).status());
    }
}

