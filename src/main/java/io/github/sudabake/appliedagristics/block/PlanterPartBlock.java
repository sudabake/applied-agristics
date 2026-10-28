package io.github.sudabake.appliedagristics.block;

import io.github.sudabake.appliedagristics.multiblock.PlanterLayout;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Stateless structure parts; no ticking block entity per frame or upgrade module. */
public final class PlanterPartBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty CELL = net.minecraft.world.level.block.state.properties.IntegerProperty.create("cell",0,35);
    private final PlanterLayout.Role role;
    public PlanterPartBlock(PlanterLayout.Role role, Properties properties) {
        super(properties); this.role = role;
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.NORTH).setValue(CELL,0));
    }
    public PlanterLayout.Role role() { return role; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return MachineShapes.get(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath(),state.getValue(FACING),state.getValue(FORMED));
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FORMED, FACING, CELL); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!state.is(previous.getBlock())) PlanterStructure.changed(level, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) PlanterStructure.changed(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos source, boolean moving) {
        PlanterStructure.changed(level, source);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Passive casing must also let the held item's placement action continue.
        return InteractionResult.PASS;
    }
}
