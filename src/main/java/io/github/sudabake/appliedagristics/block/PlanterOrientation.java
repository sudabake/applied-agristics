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
        return facing(state);
    }
    private PlanterOrientation() {}
}
