package io.github.sudabake.appliedagristics.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.crafting.CraftingHelper;
import java.util.List;
import java.util.Optional;

/** Pack-controlled cultivation data. Outputs describe the net harvest. */
public record PlantingRecipe(ResourceLocation id, Ingredient seed, Ingredient soil, List<ItemStack> outputs,
        BlockState display, boolean useLoot, int ticks, boolean reproduces,
        Optional<ResourceLocation> lootTable) implements Recipe<Container> {
    public PlantingRecipe(Ingredient seed, Ingredient soil, List<ItemStack> outputs, BlockState display,
                         boolean useLoot, int ticks, boolean reproduces, Optional<ResourceLocation> lootTable) {
        this(new ResourceLocation(Agristics.ID, "inline_planting"), seed, soil, outputs, display,
                useLoot, ticks, reproduces, lootTable);
    }

    public PlantingRecipe(Ingredient seed, Ingredient soil, List<ItemStack> outputs, BlockState display,
                         boolean useLoot, int ticks, boolean reproduces) {
        this(seed, soil, outputs, display, useLoot, ticks, reproduces, Optional.empty());
    }

    @Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(Container input, Level level) { return seed.test(input.getItem(0)); }
    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return getResultItem(registries).copy(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return Agristics.PLANTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return Agristics.PLANTING_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<PlantingRecipe> {
        @Override public PlantingRecipe fromJson(ResourceLocation id, JsonObject json) {
            var seed = Ingredient.fromJson(json.get("seed"), false);
            var soil = json.has("soil") ? Ingredient.fromJson(json.get("soil")) : Ingredient.EMPTY;
            var outputs = new java.util.ArrayList<ItemStack>();
            for (var entry : GsonHelper.getAsJsonArray(json, "outputs")) {
                var stack = entry.getAsJsonObject().deepCopy();
                if (!stack.has("item") && stack.has("id")) stack.add("item", stack.remove("id"));
                outputs.add(CraftingHelper.getItemStack(stack, true, false));
            }
            var display = json.has("display") ? BlockState.CODEC.parse(JsonOps.INSTANCE, json.get("display"))
                .getOrThrow(false, error -> { throw new JsonSyntaxException(error); }) : Blocks.WHEAT.defaultBlockState();
            int ticks = GsonHelper.getAsInt(json, "ticks", 200);
            if (ticks < 20 || ticks > 1200000) throw new JsonSyntaxException("Planting ticks outside 20–1200000");
            var table = json.has("loot_table") ? Optional.of(new ResourceLocation(GsonHelper.getAsString(json, "loot_table"))) : Optional.<ResourceLocation>empty();
            return new PlantingRecipe(id, seed, soil, List.copyOf(outputs), display,
                GsonHelper.getAsBoolean(json, "use_loot", false), ticks,
                GsonHelper.getAsBoolean(json, "reproduces", false), table);
        }

        @Override public PlantingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            var seed = Ingredient.fromNetwork(buf);
            var soil = Ingredient.fromNetwork(buf);
            var outputs = buf.readList(FriendlyByteBuf::readItem);
            var display = Block.stateById(buf.readVarInt());
            return new PlantingRecipe(id, seed, soil, outputs, display, buf.readBoolean(), buf.readVarInt(),
                buf.readBoolean(), buf.readOptional(FriendlyByteBuf::readResourceLocation));
        }

        @Override public void toNetwork(FriendlyByteBuf buf, PlantingRecipe recipe) {
            recipe.seed.toNetwork(buf);
            recipe.soil.toNetwork(buf);
            buf.writeCollection(recipe.outputs, FriendlyByteBuf::writeItem);
            buf.writeVarInt(Block.getId(recipe.display));
            buf.writeBoolean(recipe.useLoot);
            buf.writeVarInt(recipe.ticks);
            buf.writeBoolean(recipe.reproduces);
            buf.writeOptional(recipe.lootTable, FriendlyByteBuf::writeResourceLocation);
        }
    }
}
