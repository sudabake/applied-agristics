package io.github.sudabake.appliedagristics.logic;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Pack-controlled cultivation data, separate from machine crafting costs. Outputs are net harvest. */
public record PlantingRecipe(Ingredient seed,Ingredient soil,List<ItemStack> outputs,BlockState display,
        boolean useLoot,int ticks,boolean reproduces) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<PlantingRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("seed").forGetter(PlantingRecipe::seed),
        Ingredient.CODEC.optionalFieldOf("soil",Ingredient.EMPTY).forGetter(PlantingRecipe::soil),
        ItemStack.CODEC.listOf().fieldOf("outputs").forGetter(PlantingRecipe::outputs),
        BlockState.CODEC.optionalFieldOf("display",Blocks.WHEAT.defaultBlockState()).forGetter(PlantingRecipe::display),
        Codec.BOOL.optionalFieldOf("use_loot",false).forGetter(PlantingRecipe::useLoot),
        Codec.intRange(20,1200000).optionalFieldOf("ticks",200).forGetter(PlantingRecipe::ticks),
        Codec.BOOL.optionalFieldOf("reproduces",false).forGetter(PlantingRecipe::reproduces)
    ).apply(i,PlantingRecipe::new));
    @Override public boolean matches(SingleRecipeInput input,Level level) { return seed.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) { return getResultItem(registries).copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.PLANTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return Agristics.PLANTING_TYPE.get(); }
    public static final class Serializer implements RecipeSerializer<PlantingRecipe> {
        @Override public MapCodec<PlantingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,PlantingRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    }
}
