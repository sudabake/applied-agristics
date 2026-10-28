"""Compatibility entry point and collision export for the approved art installer."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/appliedagristics'

def install():
    from install_alpha14_art import install as current_install
    current_install()


def collision_shapes(names):
    statements=[]; methods=[]
    for index,name in enumerate(names):
        data=json.loads((ASSETS/f'models/block/{name}.json').read_text(encoding='utf-8'))
        elements=data.get('elements',[{'from':[0,0,0],'to':[16,16,16]}])
        elements=[e for e in elements if min(b-a for a,b in zip(e['from'],e['to']))>=(.03 if name=='cell_cultivator' else .2)]
        if name == 'cell_cultivator':
            # Only the external base, tank and pipe need collision/selection geometry.
            # Internal cells and thin glass details create a huge voxel coordinate grid.
            elements = [
                {'from': [0, 0, 0], 'to': [16, 6, 16.55]},
                {'from': [1.09, 5.9, 1.09], 'to': [14.91, 20.65, 14.91]},
                {'from': [14.1, 5.7, 11.5], 'to': [16.6, 8.4, 12.9]},
            ]
        boxes=','.join('{'+','.join(str(v) for v in e['from']+e['to'])+'}' for e in elements)
        statements.append(f'        shape{index}();')
        methods.append(f'    private static void shape{index}() {{ add("'+name+'",new double[][]{'+boxes+'}); }')
    source='''package io.github.sudabake.appliedagristics.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Generated from the installed model geometry by tools/install_art_resources.py. */
public final class MachineShapes {
    private static final Map<String,VoxelShape[]> SHAPES=new HashMap<>();
    static {
'''+ '\n'.join(statements)+'''
    }
'''+ '\n'.join(methods)+'''
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
'''
    (ROOT/'src/main/java/io/github/sudabake/appliedagristics/block/MachineShapes.java').write_text(source,encoding='utf-8',newline='\n')


if __name__=='__main__': install()
