package io.github.sudabake.appliedagristics.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Keeps the saved facing name while allowing only the three ports to use six directions. */
public final class PlanterOrientation {
    public static DirectionProperty property(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING)?BlockStateProperties.FACING:PlanterPartBlock.FACING;
    }
    public static Direction facing(BlockState state) { return state.getValue(property(state)); }
    public static BlockState withFacing(BlockState state,Direction direction) { return state.setValue(property(state),direction); }
    public static Direction next(BlockState state) {
        var old=facing(state);
        if(property(state)==PlanterPartBlock.FACING) return old.getClockWise();
        return switch(old) { case NORTH->Direction.EAST;case EAST->Direction.SOUTH;case SOUTH->Direction.WEST;case WEST->Direction.UP;case UP->Direction.DOWN;case DOWN->Direction.NORTH; };
    }
    public static Direction front(BlockState state) {
        Direction facing=facing(state);
        if(!state.getValue(PlanterPartBlock.FORMED) || facing.getAxis().isVertical()) return facing;
        int cell=state.getValue(PlanterPartBlock.CELL),x=cell%3,y=cell/9,z=cell/3%3;
        Direction local=z==0?Direction.SOUTH:z==2?Direction.NORTH:x==0?Direction.EAST:x==2?Direction.WEST:y==0?Direction.DOWN:y==3?Direction.UP:Direction.SOUTH;
        if(local.getAxis().isVertical()) return local;
        return switch(facing) { case NORTH->local.getOpposite();case EAST->local.getCounterClockWise();case WEST->local.getClockWise();default->local; };
    }
    private PlanterOrientation() {}
}
