package io.github.sudabake.appliedagristics.compat.jei;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import mezz.jei.api.constants.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import java.util.*;

/** Runs only from the opt-in client smoke test, never during normal play. */
public final class JeiSmoke {
    private static int ticks;
    private static boolean verified;
    private static final ResourceLocation COW = ResourceLocation.parse("minecraft:cow");
    private static final ResourceLocation PIG = ResourceLocation.parse("minecraft:pig");
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public static boolean tick() {
        var runtime = AgristicsJei.runtime(); if (runtime == null || BioLootCatalog.clientCatalog().isEmpty()) return false;
        try {
            if (!verified) { verify(); verified = true; }
            ticks++;
            var focus = runtime.getJeiHelpers().getFocusFactory();
            if (ticks == 1) runtime.getRecipesGui().show(focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.BIO_SEED.get(), COW)));
            if (ticks == 12) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-seed.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if (ticks == 15) runtime.getRecipesGui().show(focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.CULTURE.get(), COW)));
            if (ticks == 27) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-culture.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if (ticks == 30) runtime.getRecipesGui().show(focus.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW)));
            if (ticks == 42) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-loot.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if (ticks == 45) runtime.getRecipesGui().show(focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.EXTRACT.get(), COW)));
            if (ticks == 57) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-extraction.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if (ticks == 59) runtime.getIngredientFilter().setFilterText("生物种子");
            if (ticks == 64) {
                var visible = runtime.getIngredientFilter().getFilteredItemStacks();
                for (var entity : BioLootCatalog.clientCatalog().keySet()) require(visible.stream().anyMatch(stack -> stack.is(Agristics.BIO_SEED.get()) && entity.equals(BioData.identity(stack))), "Variant missing in JEI search: " + entity);
                Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-automatic-seeds.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            }
            if(ticks==67) { runtime.getIngredientFilter().setFilterText("");AgristicsJei.showStructure(); }
            if(ticks==79) Screenshot.grab(Minecraft.getInstance().gameDirectory,"agr-jei-array-structure.png",Minecraft.getInstance().getMainRenderTarget(),message->{});
            if(ticks==80) {
                var category=runtime.getRecipeManager().getRecipeCategory(AgristicsJei.STRUCTURE);
                var key=com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(0);
                require(category.handleInput("agricultural_array",20,102,key),"Structure rotation input");
                require(category.handleInput("agricultural_array",90,102,key),"Structure layer input");
            }
            if(ticks==90) Screenshot.grab(Minecraft.getInstance().gameDirectory,"agr-jei-array-layer.png",Minecraft.getInstance().getMainRenderTarget(),message->{});
            if(ticks==92) {
                require(io.github.sudabake.appliedagristics.client.ArrayGuide.guide().pageExists(io.github.sudabake.appliedagristics.client.ArrayGuide.PAGE),"Guide page missing from AE's shared index");
                var guidePage=io.github.sudabake.appliedagristics.client.ArrayGuide.guide().getPage(io.github.sudabake.appliedagristics.client.ArrayGuide.PAGE);
                var scene=guidePage.document().getBlocks().stream().filter(guideme.scene.LytGuidebookScene.class::isInstance)
                    .map(guideme.scene.LytGuidebookScene.class::cast).findFirst().orElseThrow(()->new IllegalStateException("Guide scene missing"));
                require(scene.getScene().getLevel().getFilledBlocks().count()==36,"Guide scene must contain the complete array");
                io.github.sudabake.appliedagristics.client.ArrayGuide.open();
                require(Minecraft.getInstance().screen instanceof guideme.internal.screen.GuideScreen screen
                    && io.github.sudabake.appliedagristics.client.ArrayGuide.PAGE.equals(screen.getCurrentPageId()),"Guide screen did not open requested page");
            }
            if(ticks==110) Screenshot.grab(Minecraft.getInstance().gameDirectory,"agr-guideme-array.png",Minecraft.getInstance().getMainRenderTarget(),message->{});
            if (ticks >= 115) { org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("JEI_SMOKE_PASSED automaticVariants=true searchList=true speciesFocus=true actualCrafting=true catalogReload=true structureScene=true guidePage=true"); return true; }
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").error("JEI_SMOKE_FAILED", error);
            Minecraft.getInstance().stop();
        }
        return false;
    }
    private static void verify() {
        var runtime = AgristicsJei.runtime(); var manager = runtime.getRecipeManager(); var focus = runtime.getJeiHelpers().getFocusFactory();
        require(manager.createRecipeLookup(AgristicsJei.STRUCTURE).get().count()==1,"Array structure recipe missing");
        require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(AgristicsJei.STRUCTURE),"agricultural_array",focus.getEmptyFocusGroup()).isPresent(),"Invalid array structure layout");
        var catalog = BioLootCatalog.clientCatalog(); var ingredientManager = runtime.getIngredientManager();
        for(var retired:List.of(Agristics.LEGACY_SEEDS_FIRST.get().asItem(),Agristics.LEGACY_SEEDS_SECOND.get().asItem()))
            require(ingredientManager.getAllItemStacks().stream().noneMatch(s->s.is(retired)),"Retired storage tiers must not appear in JEI");
        int equipment=0;
        var derived=Set.of(Agristics.BIO_SEED.get(),Agristics.BIO_ESSENCE.get(),Agristics.CULTURE.get(),Agristics.EXTRACT.get(),Agristics.LEGACY_SEEDS_FIRST.get().asItem(),Agristics.LEGACY_SEEDS_SECOND.get().asItem());
        for(var entry:Agristics.ITEMS.getEntries()) {
            var item=entry.get(); if(derived.contains(item)) continue;
            var output=focus.createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,new ItemStack(item));
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var recipes=manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(output)).get().toList();
            require(recipes.stream().anyMatch(r->r.id().equals(id)),"Missing equipment recipe in JEI: "+id);
            for(var recipe:recipes) require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(RecipeTypes.CRAFTING),recipe,focus.getEmptyFocusGroup()).isPresent(),"Invalid equipment recipe layout: "+id);
            equipment++;
        }
        require(equipment==20,"JEI equipment recipe coverage");
        org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("JEI_EQUIPMENT_RECIPES_PASSED equipment=20");
        for (var entity : catalog.keySet()) for (var item : List.of(Agristics.BIO_SEED, Agristics.BIO_ESSENCE))
            require(ingredientManager.getAllItemStacks().stream().anyMatch(stack -> stack.is(item.get()) && entity.equals(BioData.identity(stack))), "Missing automatic variant: " + entity + "/" + item.getId());
        for (var item : List.of(Agristics.SAMPLER, Agristics.CULTURE, Agristics.EXTRACT)) {
            var entries = ingredientManager.getAllItemStacks().stream().filter(stack -> stack.is(item.get())).toList();
            require(entries.size() == 1 && BioData.identity(entries.getFirst()) == null, "Intermediate must have one generic JEI entry: " + item.getId());
        }
        var helper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        require(!helper.getUniqueId(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW), UidContext.Recipe).equals(helper.getUniqueId(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), PIG), UidContext.Recipe)), "Species subtype collision");
        var cowEssence = focus.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW));
        var cowLoot = manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(cowEssence)).get().toList();
        require(cowLoot.size() == catalog.get(COW).stream().filter(i->i!=Items.AIR).count(), "Wrong cow loot count: " + cowLoot.size());
        for (var recipe : cowLoot) require(catalog.get(COW).contains(recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()).getItem()), "Cross-species loot");
        var seedFocus = focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.BIO_SEED.get(), COW));
        require(manager.createRecipeLookup(AgristicsJei.RITUAL).limitFocus(List.of(seedFocus)).get().count() == 1, "Seed recipe lookup");
        var cultureFocus = focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.CULTURE.get(), COW));
        require(manager.createRecipeLookup(AgristicsJei.CULTURE).limitFocus(List.of(cultureFocus)).get().count() == catalog.size(), "Generic culture must expose all species recipes");
        var extractFocus = focus.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.EXTRACT.get(), COW));
        require(manager.createRecipeLookup(AgristicsJei.EXTRACTION).limitFocus(List.of(extractFocus)).get().count() == catalog.size(), "Generic extract must expose all species recipes");
        var emptyFocus = focus.getEmptyFocusGroup();
        for (var type : List.of(AgristicsJei.SAMPLING, AgristicsJei.CULTURE, AgristicsJei.EXTRACTION, AgristicsJei.PLANTING,AgristicsJei.RITUAL))
            for (var recipe : manager.createRecipeLookup(type).get().toList()) require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(type), recipe, emptyFocus).isPresent(), "Invalid process layout");
        var registries = Minecraft.getInstance().level.registryAccess();
        for (var holder : manager.createRecipeLookup(RecipeTypes.CRAFTING).get().filter(recipe -> recipe.id().getNamespace().equals(Agristics.ID) && recipe.id().getPath().startsWith("jei/")).toList()) {
            require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(RecipeTypes.CRAFTING), holder, emptyFocus).isPresent(), "Invalid crafting layout");
            var stacks = holder.value().getIngredients().stream().map(ingredient -> ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0]).toList();
            if (holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                var input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), stacks);
                var actual = new BioLootRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
                require(actual.matches(input, Minecraft.getInstance().level) && ItemStack.isSameItemSameComponents(actual.assemble(input, registries), shaped.getResultItem(registries)), "JEI pattern differs from real loot recipe");
            } else {
                var input = CraftingInput.of(2, 1, stacks); var actual = new BioSeedRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
                require(actual.matches(input, Minecraft.getInstance().level) && ItemStack.isSameItemSameComponents(actual.assemble(input, registries), holder.value().getResultItem(registries)), "JEI seed differs from real recipe");
            }
        }
        var altered = new LinkedHashMap<>(catalog); altered.put(COW, List.of(Items.AIR,Items.DIAMOND));
        BioLootCatalog.loadClient(encode(altered));
        var updated = manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(cowEssence)).get().toList();
        require(updated.size() == 1 && updated.getFirst().value().getResultItem(registries).is(Items.DIAMOND), "Stale catalog recipes after reload");
        require(BioLootCatalog.clientDrops(COW).getFirst()==Items.AIR,"Client must retain disabled pattern slots");
        require(updated.getFirst().value() instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped && shaped.getWidth()==1 && shaped.getHeight()==3,"JEI must keep diamond in the second, vertical pattern slot");
        require(manager.createRecipeLookup(AgristicsJei.CULTURE).limitFocus(List.of(cultureFocus)).get().count() == catalog.size(), "Duplicate process recipes after reload");
        BioLootCatalog.loadClient(encode(catalog));
        require(manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(cowEssence)).get().count() == catalog.get(COW).stream().filter(i->i!=Items.AIR).count(), "Catalog restoration");
    }
    private static CompoundTag encode(Map<ResourceLocation,List<Item>> catalog) {
        var root = new CompoundTag(); var entities = new ListTag();
        catalog.forEach((entity, items) -> {
            var entry = new CompoundTag(); entry.putString("entity", entity.toString()); var list = new ListTag();
            for (var item : items) list.add(StringTag.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
            entry.put("items", list); entities.add(entry);
        }); root.put("entities", entities); return root;
    }
}
