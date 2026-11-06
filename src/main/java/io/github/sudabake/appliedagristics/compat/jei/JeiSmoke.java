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
    private static appeng.parts.encoding.PatternEncodingTerminalPart terminal;
    private static volatile boolean encoded;
    private static boolean opened;
    private static boolean openRequested;
    private static int encodingTicks;
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
            if (ticks == 15) runtime.getRecipesGui().showTypes(List.of(AgristicsJei.CULTURE));
            if (ticks == 27) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-culture.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if (ticks == 30) runtime.getRecipesGui().show(focus.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, BioData.withIdentity(Agristics.BIO_ESSENCE.get(), COW)));
            if (ticks == 42) Screenshot.grab(Minecraft.getInstance().gameDirectory, "agr-jei-loot.png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
            if(ticks==45) {
                var wheat=focus.createFocus(RecipeIngredientRole.INPUT,VanillaTypes.ITEM_STACK,new ItemStack(Items.WHEAT_SEEDS));
                var manager=runtime.getRecipeManager();var recipes=manager.createRecipeLookup(AgristicsJei.PLANTING).limitFocus(List.of(wheat)).get().toList();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(AgristicsJei.PLANTING),recipes,List.of(wheat));
            }
            if(ticks==57) Screenshot.grab(Minecraft.getInstance().gameDirectory,"agr-jei-planting-products.png",Minecraft.getInstance().getMainRenderTarget(),message->{});
            if (ticks == 59) runtime.getIngredientFilter().setFilterText(net.minecraft.network.chat.Component.translatable("item.appliedagristics.bio_seed").getString());
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
            if(ticks==116) {
                Minecraft.getInstance().setScreen(null);
                Minecraft.getInstance().getSingleplayerServer().execute(()-> {
                var mc=Minecraft.getInstance();var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var pos=new net.minecraft.core.BlockPos(35,100,10);level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.west(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                appeng.api.parts.PartHelper.setPart(level,pos,null,player,appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
                terminal=appeng.api.parts.PartHelper.setPart(level,pos,net.minecraft.core.Direction.NORTH,player,appeng.core.definitions.AEParts.PATTERN_ENCODING_TERMINAL.asItem());
                require(terminal!=null,"Real pattern encoding terminal can be placed");
                terminal.getLogic().getBlankPatternInv().setItemDirect(0,appeng.core.definitions.AEItems.BLANK_PATTERN.stack());
                player.teleportTo(level,35.5,100,7,java.util.Set.of(),0,0);
            });
            }
            if(ticks>=135 && !openRequested && appeng.api.parts.PartHelper.getPart(Minecraft.getInstance().level,new net.minecraft.core.BlockPos(35,100,10),net.minecraft.core.Direction.NORTH) instanceof appeng.parts.encoding.PatternEncodingTerminalPart) {
                openRequested=true;
                Minecraft.getInstance().getSingleplayerServer().execute(()-> {
                    var mc=Minecraft.getInstance();var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    terminal.onUseWithoutItem(player,net.minecraft.world.phys.Vec3.ZERO);
                });
            }
            if(Minecraft.getInstance().player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu) {opened=true;encodingTicks++;}
            if(encodingTicks==1) {
                var mc=Minecraft.getInstance();require(mc.player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu,"Real AE pattern terminal opens");
                var menu=(appeng.menu.me.items.PatternEncodingTermMenu)mc.player.containerMenu;
                var category=runtime.getRecipeManager().getRecipeCategory(AgristicsJei.CULTURE);
                var handler=runtime.getRecipeTransferManager().getRecipeTransferHandler(menu,category).orElseThrow();
                require(handler instanceof BioPatternTransfer,"JEI resolves our component-preserving pattern transfer handler");
                var error=handler.transferRecipe(menu,new AgristicsJei.Process(COW,0),null,mc.player,false,true);
                require(error==null,"JEI transfer succeeds through native AE encoder packets");
            }
            if(encodingTicks==10) {
                var mc=Minecraft.getInstance();((appeng.menu.me.items.PatternEncodingTermMenu)mc.player.containerMenu).encode();
            }
            if(encodingTicks==20) Minecraft.getInstance().getSingleplayerServer().execute(()-> {
                var mc=Minecraft.getInstance();var stack=terminal.getLogic().getEncodedPatternInv().getStackInSlot(0);
                var pattern=appeng.api.crafting.PatternDetailsHelper.decodePattern(stack,mc.getSingleplayerServer().overworld());
                require(pattern!=null && pattern.getInputs().length==3 && pattern.getPrimaryOutput().what() instanceof appeng.api.stacks.AEItemKey key
                    && key.matches(BioData.withIdentity(Agristics.BIO_SEED.get(),COW)),"Encoded native processing pattern retains the cow seed output");
                boolean sample=false;
                for(var input:pattern.getInputs())for(var option:input.getPossibleInputs())
                    if(option.what() instanceof appeng.api.stacks.AEItemKey key && key.matches(BioData.withIdentity(Agristics.SAMPLER.get(),COW)))sample=true;
                require(sample,"Encoded sample retains its exact cow component");encoded=true;
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("JEI_NATIVE_PATTERN_ENCODE_PASSED inputs=3 species=minecraft:cow standardPattern=true");
            });
            if(encodingTicks==26) Screenshot.grab(Minecraft.getInstance().gameDirectory,"agr-jei-encoded-bio-pattern.png",Minecraft.getInstance().getMainRenderTarget(),message->{});
            if (encodingTicks >= 30 && encoded) {
                if(!io.github.sudabake.appliedagristics.testing.EaepClientChecks.tick(Minecraft.getInstance())) return false;
                org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("JEI_SMOKE_PASSED automaticVariants=true searchList=true speciesFocus=true actualCrafting=true catalogReload=true structureScene=true guidePage=true");
                return true;
            }
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
        int equipment=0;
        var derived=Set.of(Agristics.BIO_SEED.get(),Agristics.BIO_ESSENCE.get());
        for(var entry:Agristics.ITEMS.getEntries()) {
            var item=entry.get(); if(derived.contains(item)) continue;
            var output=focus.createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,new ItemStack(item));
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var recipes=manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(output)).get().toList();
            require(recipes.stream().anyMatch(r->r.id().equals(id)),"Missing equipment recipe in JEI: "+id);
            for(var recipe:recipes) require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(RecipeTypes.CRAFTING),recipe,focus.getEmptyFocusGroup()).isPresent(),"Invalid equipment recipe layout: "+id);
            equipment++;
        }
        require(equipment==17,"JEI equipment recipe coverage");
        org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("JEI_EQUIPMENT_RECIPES_PASSED equipment=17");
        for (var entity : catalog.keySet()) for (var item : List.of(Agristics.BIO_SEED, Agristics.BIO_ESSENCE))
            require(ingredientManager.getAllItemStacks().stream().anyMatch(stack -> stack.is(item.get()) && entity.equals(BioData.identity(stack))), "Missing automatic variant: " + entity + "/" + item.getId());
        for (var item : List.of(Agristics.SAMPLER)) {
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
        require(manager.createRecipeLookup(AgristicsJei.CULTURE).limitFocus(List.of(seedFocus)).get().count() == 1, "Seed recipe lookup");
        var wheatFocus=focus.createFocus(RecipeIngredientRole.INPUT,VanillaTypes.ITEM_STACK,new ItemStack(Items.WHEAT_SEEDS));
        var wheatRecipes=manager.createRecipeLookup(AgristicsJei.PLANTING).limitFocus(List.of(wheatFocus)).get().toList();
        require(wheatRecipes.stream().anyMatch(recipe->recipe.outputs().stream().anyMatch(stack->stack.is(Items.WHEAT))),"Wheat seed informational page lists wheat");
        for(var recipe:manager.createRecipeLookup(AgristicsJei.PLANTING).get().toList()) {
            require(recipe.outputs().stream().allMatch(stack->stack.getCount()==1),"Planting information displays item types at one-count icons");
            require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(AgristicsJei.PLANTING),recipe,focus.getEmptyFocusGroup()).isPresent(),"Planting product page layout");
        }
        var emptyFocus = focus.getEmptyFocusGroup();
        for (var type : List.of(AgristicsJei.SAMPLING,AgristicsJei.CULTURE))
            for (var recipe : manager.createRecipeLookup(type).get().toList()) require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(type), recipe, emptyFocus).isPresent(), "Invalid process layout");
        var registries = Minecraft.getInstance().level.registryAccess();
        for (var holder : manager.createRecipeLookup(RecipeTypes.CRAFTING).get().filter(recipe -> recipe.id().getNamespace().equals(Agristics.ID) && recipe.id().getPath().startsWith("jei/")).toList()) {
            require(manager.createRecipeLayoutDrawable(manager.getRecipeCategory(RecipeTypes.CRAFTING), holder, emptyFocus).isPresent(), "Invalid crafting layout");
            var stacks = holder.value().getIngredients().stream().map(ingredient -> ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0]).toList();
            if (holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                var input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), stacks);
                var actual = new BioLootRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
                require(actual.matches(input, Minecraft.getInstance().level) && ItemStack.isSameItemSameComponents(actual.assemble(input, registries), shaped.getResultItem(registries)), "JEI pattern differs from real loot recipe");
            }
        }
        var altered = new LinkedHashMap<>(catalog); altered.put(COW, List.of(Items.AIR,Items.DIAMOND));
        BioLootCatalog.loadClient(encode(altered));
        var updated = manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(cowEssence)).get().toList();
        require(updated.size() == 1 && updated.getFirst().value().getResultItem(registries).is(Items.DIAMOND), "Stale catalog recipes after reload");
        require(BioLootCatalog.clientDrops(COW).getFirst()==Items.AIR,"Client must retain disabled pattern slots");
        require(updated.getFirst().value() instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped && shaped.getWidth()==1 && shaped.getHeight()==3,"JEI must keep diamond in the second, vertical pattern slot");
        require(manager.createRecipeLookup(AgristicsJei.CULTURE).limitFocus(List.of(seedFocus)).get().count() == 1, "Duplicate process recipes after reload");
        BioLootCatalog.loadClient(encode(catalog));
        require(manager.createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(cowEssence)).get().count() == catalog.get(COW).stream().filter(i->i!=Items.AIR).count(), "Catalog restoration");
    }
    private static CompoundTag encode(Map<ResourceLocation,List<Item>> catalog) {
        var root = new CompoundTag(); var entities = new ListTag();
        catalog.forEach((entity, items) -> {
            var entry = new CompoundTag(); entry.putString("entity", entity.toString()); var list = new ListTag();
            for (var item : items) list.add(StringTag.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
            entry.put("items", list); entities.add(entry);
        }); root.put("entities", entities);root.put("planting",io.github.sudabake.appliedagristics.logic.PlantingCatalog.clientTag());return root;
    }
}
