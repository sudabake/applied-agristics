package io.github.sudabake.appliedagristics.biology;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A pack-reloadable ritual, shared by all species. Species must match exactly, including components. */
public record BioInfusionRecipe(Ingredient center, Ingredient essence, int ticks) implements Recipe<CraftingInput> {
    public static final MapCodec<BioInfusionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("center").forGetter(BioInfusionRecipe::center),
        Ingredient.CODEC_NONEMPTY.fieldOf("essence").forGetter(BioInfusionRecipe::essence),
        Codec.intRange(40,1200000).optionalFieldOf("ticks",120).forGetter(BioInfusionRecipe::ticks)
    ).apply(i,BioInfusionRecipe::new));
    public ResourceLocation species(CraftingInput input) {
        if(input.size()!=3 || !center.test(input.getItem(0)) || !essence.test(input.getItem(2))) return null;
        var extract=input.getItem(1);return extract.is(Agristics.EXTRACT.get()) && BioData.identified(extract)?BioData.identity(extract):null;
    }
    @Override public boolean matches(CraftingInput input,Level level) { return species(input)!=null; }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) { var id=species(input); return id==null?ItemStack.EMPTY:BioData.withIdentity(Agristics.BIO_SEED.get(),id); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return new ItemStack(Agristics.BIO_SEED.get()); }
    @Override public boolean canCraftInDimensions(int w,int h) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.BIO_INFUSION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return Agristics.BIO_INFUSION_TYPE.get(); }
    public static final class Serializer implements RecipeSerializer<BioInfusionRecipe> {
        @Override public MapCodec<BioInfusionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,BioInfusionRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    }
}
