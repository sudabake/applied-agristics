package io.github.sudabake.appliedagristics.compat.jei;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.client.CultureScreen;
import mezz.jei.api.*;
import mezz.jei.api.constants.*;
import mezz.jei.api.ingredients.subtypes.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import java.util.*;

/** Loaded by JEI only. The server catalog, including modded mobs, owns the variants. */
@JeiPlugin
public final class AgristicsJei implements IModPlugin {
    public static final mezz.jei.api.recipe.RecipeType<String> STRUCTURE = mezz.jei.api.recipe.RecipeType.create(Agristics.ID,"array_structure",String.class);
    public static void showStructure() { var r=runtime(); if(r!=null) r.getRecipesGui().showTypes(List.of(STRUCTURE)); }
    public record Process(ResourceLocation entity, int generation) {}
    public static final mezz.jei.api.recipe.RecipeType<Process> SAMPLING = mezz.jei.api.recipe.RecipeType.create(Agristics.ID, "sampling", Process.class);
    public static final mezz.jei.api.recipe.RecipeType<Process> CULTURE = mezz.jei.api.recipe.RecipeType.create(Agristics.ID, "culture", Process.class);
    public static final mezz.jei.api.recipe.RecipeType<Process> EXTRACTION = mezz.jei.api.recipe.RecipeType.create(Agristics.ID, "extraction", Process.class);
    public static final mezz.jei.api.recipe.RecipeType<Process> PLANTING = mezz.jei.api.recipe.RecipeType.create(Agristics.ID, "bio_planting", Process.class);
    private static AgristicsJei instance;
    private IJeiRuntime runtime;
    private Map<ResourceLocation, List<Item>> loaded = Map.of();
    private List<ItemStack> variants = List.of();
    private List<RecipeHolder<CraftingRecipe>> crafting = List.of();
    private List<Process> processes = List.of();
    private int generation;

    public AgristicsJei() {
        instance = this;
        NeoForge.EVENT_BUS.addListener(this::catalogUpdated);
    }
    public static IJeiRuntime runtime() { return instance == null ? null : instance.runtime; }
    @Override public ResourceLocation getPluginUid() { return id("biology"); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Agristics.ID, path); }
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        var interpreter = new ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) { return BioData.identity(stack); }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                var entity = BioData.identity(stack); return entity == null ? "" : entity.toString();
            }
        };
        for (var item : List.of(Agristics.BIO_SEED, Agristics.BIO_ESSENCE))
            registration.registerSubtypeInterpreter(item.get(), interpreter);
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ArrayStructureCategory(gui),new BioProcessCategory(gui, SAMPLING), new BioProcessCategory(gui, CULTURE),
            new BioProcessCategory(gui, EXTRACTION), new BioProcessCategory(gui, PLANTING));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for(var block:List.of(Agristics.NETWORK,Agristics.NETWORK_ACCESS,Agristics.INPUT_PORT,Agristics.OUTPUT_PORT,Agristics.SEEDS,Agristics.ESSENCE_VAULT,Agristics.PLANTER_FRAME,Agristics.CULTIVATION_CHAMBER,Agristics.SPEED_MODULE,Agristics.PARALLEL_MODULE,Agristics.CONVERSION_MODULE))
            registration.addRecipeCatalyst(new ItemStack(block.get()),STRUCTURE);
        registration.addRecipeCatalyst(new ItemStack(Agristics.CULTIVATOR.get()), CULTURE, EXTRACTION);
        registration.addRecipeCatalyst(new ItemStack(Agristics.EARLY.get()), PLANTING);
        registration.addRecipeCatalyst(new ItemStack(Agristics.NETWORK.get()), PLANTING);
        // The ME altar uses MA's ordinary external pedestals and infusion recipes.
        registration.addRecipeCatalyst(new ItemStack(Agristics.ME_INFUSION.get()),
            com.blakebr0.mysticalagriculture.compat.jei.category.InfusionCategory.RECIPE_TYPE);
    }
    @Override public void registerRecipes(IRecipeRegistration registration) { registration.addRecipes(STRUCTURE,List.of("agricultural_array")); }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CultureScreen.class, 43, 89, 122, 6, CULTURE, EXTRACTION);
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        refresh();
    }
    @Override public void onRuntimeUnavailable() {
        runtime = null; loaded = Map.of(); variants = List.of(); crafting = List.of(); processes = List.of();
    }
    private void catalogUpdated(BioLootCatalog.ClientCatalogUpdated event) { refresh(); }
    private void refresh() {
        if (runtime == null) return;
        var catalog = BioLootCatalog.clientCatalog(); if (catalog.equals(loaded)) return;
        var manager = runtime.getRecipeManager();
        manager.hideRecipes(RecipeTypes.CRAFTING, crafting);
        for (var type : List.of(SAMPLING, CULTURE, EXTRACTION, PLANTING)) manager.hideRecipes(type, processes);
        if (!variants.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, variants);
        var newVariants = new ArrayList<ItemStack>();
        var newCrafting = new ArrayList<RecipeHolder<CraftingRecipe>>();
        var newProcesses = new ArrayList<Process>();
        generation++;
        catalog.keySet().stream().sorted().forEach(entity -> {
            newProcesses.add(new Process(entity, generation));
            for (var item : List.of(Agristics.BIO_SEED, Agristics.BIO_ESSENCE))
                newVariants.add(BioData.withIdentity(item.get(), entity));
            var seed = BioData.withIdentity(Agristics.BIO_SEED.get(), entity);
            var extract = BioData.withIdentity(Agristics.EXTRACT.get(), entity);
            var ingredients = NonNullList.of(Ingredient.EMPTY, DataComponentIngredient.of(true, extract),
                Ingredient.of(com.blakebr0.mysticalagriculture.init.ModItems.PROSPERITY_SEED_BASE.get()));
            newCrafting.add(new RecipeHolder<>(recipeId(entity, "seed"), new ShapelessRecipe("", CraftingBookCategory.MISC, seed, ingredients)));
            var essence = BioData.withIdentity(Agristics.BIO_ESSENCE.get(), entity);
            var drops = catalog.get(entity);
            for (int i = 0; i < Math.min(drops.size(), BioPatterns.capacity()); i++) {
                if(drops.get(i)==Items.AIR) continue;
                int mask = BioPatterns.at(i); var rows = new String[3];
                for (int y = 0; y < 3; y++) {
                    var row = new StringBuilder(); for (int x = 0; x < 3; x++) row.append((mask & 1 << (y * 3 + x)) != 0 ? 'E' : ' '); rows[y] = row.toString();
                }
                var pattern = ShapedRecipePattern.of(Map.of('E', DataComponentIngredient.of(true, essence)), rows);
                newCrafting.add(new RecipeHolder<>(recipeId(entity, "loot/" + i),
                    new ShapedRecipe("", CraftingBookCategory.MISC, pattern, new ItemStack(drops.get(i)))));
            }
        });
        variants = List.copyOf(newVariants); crafting = List.copyOf(newCrafting); processes = List.copyOf(newProcesses); loaded = catalog;
        runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, variants);
        manager.addRecipes(RecipeTypes.CRAFTING, crafting);
        for (var type : List.of(SAMPLING, CULTURE, EXTRACTION, PLANTING)) manager.addRecipes(type, processes);
        org.slf4j.LoggerFactory.getLogger("AppliedAgristics").info("JEI biological catalog: {} species, {} crafting recipes", processes.size(), crafting.size());
    }
    private ResourceLocation recipeId(ResourceLocation entity, String suffix) {
        return id("jei/" + generation + "/" + entity.getNamespace() + "/" + entity.getPath() + "/" + suffix);
    }
}
