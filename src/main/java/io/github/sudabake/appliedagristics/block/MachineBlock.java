package io.github.sudabake.appliedagristics.block;

import com.mojang.serialization.MapCodec;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import java.util.List;

public final class MachineBlock extends BaseEntityBlock {
    public enum Kind {
        EARLY(0), SEEDS(1), NETWORK(2), CULTIVATOR(4), ACCESS(5), ME_INFUSION(6), INPUT_PORT(7), OUTPUT_PORT(8), ESSENCE_VAULT(9);
        public final int menuId; Kind(int menuId) { this.menuId=menuId; }
    }
    private final Kind kind;
    public MachineBlock(Kind kind, Properties properties) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(PlanterPartBlock.FORMED, false).setValue(PlanterPartBlock.FACING, Direction.NORTH).setValue(PlanterPartBlock.CELL,0));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) { builder.add(PlanterPartBlock.FORMED, PlanterPartBlock.FACING, PlanterPartBlock.CELL); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(PlanterPartBlock.FACING, context.getHorizontalDirection().getOpposite()); }
    public Kind kind() { return kind; }
    public boolean preservesContents() { return kind==Kind.SEEDS || kind==Kind.ESSENCE_VAULT; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new MachineBlock(kind, p)); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) { case EARLY -> new EarlyPlanter(pos, state); case SEEDS -> new SeedStorage(pos, state); case NETWORK -> new NetworkPlanter(pos, state); case CULTIVATOR -> new CellCultivator(pos, state); case ACCESS -> new NetworkAccess(pos,state); case ME_INFUSION -> new MEInfusionAltar(pos,state); case INPUT_PORT -> new ItemPort(pos,state,true); case OUTPUT_PORT -> new ItemPort(pos,state,false); case ESSENCE_VAULT -> new EssenceVault(pos,state); };
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world, pos, blockState, entity) -> {
            if (entity instanceof MachineEntity machine) {
                if (machine instanceof GridMachine grid) grid.ensureNode();
                int interval = machine instanceof CellCultivator || machine instanceof MEInfusionAltar ? 1 : machine instanceof NetworkPlanter ? 5 : 20;
                if ((world.getGameTime() + Math.floorMod(pos.asLong(), interval)) % interval == 0) { machine.step(); if (machine instanceof NetworkPlanter || machine instanceof CellCultivator) machine.updateWorkVisual(); }
            }
        };
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return MachineShapes.get(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath(),state.getValue(PlanterPartBlock.FACING),state.getValue(PlanterPartBlock.FORMED));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if ((kind == Kind.EARLY || kind == Kind.CULTIVATOR) && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MachineEntity machine) machine.open(server);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof GridMachine grid) grid.getMainNode().setOwningPlayer(player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (preservesContents() && params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MachineEntity be) be.saveToItem(stack, params.getLevel().registryAccess());
        return List.of(stack);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Creative breaking skips normal loot; keep the two portable storages intact there too.
        if (!level.isClientSide && player.isCreative() && preservesContents() && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            var stack=new ItemStack(this);
            machine.saveToItem(stack,level.registryAccess());
            Block.popResource(level,pos,stack);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && !preservesContents() && level.getBlockEntity(pos) instanceof MachineEntity machine)
            machine.dropContents();
        if (!state.is(replacement.getBlock())) io.github.sudabake.appliedagristics.multiblock.PlanterStructure.changed(level,pos);
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof GridMachine grid) grid.release();
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state,level,pos,previous,moving);
        if (!state.is(previous.getBlock())) io.github.sudabake.appliedagristics.multiblock.PlanterStructure.changed(level,pos);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos source, boolean moving) {
        io.github.sudabake.appliedagristics.multiblock.PlanterStructure.changed(level,source);
    }
}
