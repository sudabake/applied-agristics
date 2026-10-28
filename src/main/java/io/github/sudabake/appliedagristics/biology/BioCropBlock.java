package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.*;

/** Vanilla crop growth with species-aware drops; each plant keeps its own identity. */
public final class BioCropBlock extends CropBlock implements EntityBlock {
    public BioCropBlock(Properties properties) { super(properties); }
    @Override protected ItemLike getBaseSeedId() { return Agristics.BIO_SEED.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new BioCropEntity(pos,state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (level.getBlockEntity(pos) instanceof BioCropEntity crop) crop.setIdentity(BioData.identity(stack));
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BioCropEntity crop) || crop.identity()==null) return List.of();
        var drops=new ArrayList<ItemStack>();
        drops.add(BioData.withIdentity(Agristics.BIO_SEED.get(),crop.identity()));
        if (isMaxAge(state)) {
            drops.add(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),crop.identity()));
            if (builder.getLevel().random.nextFloat()<.1f) drops.add(BioData.withIdentity(Agristics.BIO_SEED.get(),crop.identity()));
        }
        return drops;
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        return level.getBlockEntity(pos) instanceof BioCropEntity crop && crop.identity()!=null
            ? BioData.withIdentity(Agristics.BIO_SEED.get(),crop.identity()) : ItemStack.EMPTY;
    }
}
