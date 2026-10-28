package io.github.sudabake.appliedagristics.compat.jei;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;

/** Native JEI slots, fluid renderer and arrow; no duplicate crafting interface. */
final class BioProcessCategory implements IRecipeCategory<AgristicsJei.Process> {
    private final RecipeType<AgristicsJei.Process> type;
    private final IDrawable icon, arrow;
    BioProcessCategory(IGuiHelper gui, RecipeType<AgristicsJei.Process> type) {
        this.type = type;
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(type == AgristicsJei.RITUAL ? Agristics.BIO_ALTAR.get().asItem() : type == AgristicsJei.EXTRACTION ? Agristics.CULTURE.get() : type == AgristicsJei.SAMPLING ? Agristics.SAMPLER.get() : type == AgristicsJei.PLANTING ? Agristics.BIO_SEED.get() : Agristics.CULTIVATOR.get().asItem()));
        arrow = gui.createAnimatedRecipeArrow(80);
    }
    @Override public RecipeType<AgristicsJei.Process> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.appliedagristics." + type.getUid().getPath()); }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 94; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, AgristicsJei.Process recipe, IFocusGroup focuses) {
        var id = recipe.entity();
        if(type==AgristicsJei.RITUAL) {
            var level=Minecraft.getInstance().level;
            var rituals=level.getRecipeManager().getAllRecipesFor(Agristics.BIO_INFUSION_TYPE.get());
            if(rituals.isEmpty()) return;
            var ritual=rituals.getFirst().value();
            builder.addSlot(RecipeIngredientRole.INPUT,10,35).setStandardSlotBackground().addIngredients(ritual.center());
            builder.addSlot(RecipeIngredientRole.INPUT,35,35).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.EXTRACT.get(),id));
            builder.addSlot(RecipeIngredientRole.INPUT,60,35).setStandardSlotBackground().addIngredients(ritual.essence());
            builder.addSlot(RecipeIngredientRole.OUTPUT,140,35).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.BIO_SEED.get(),id));
        } else if (type == AgristicsJei.SAMPLING) {
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 25).setStandardSlotBackground().addItemStack(new ItemStack(Agristics.SAMPLER.get()));
            var egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));
            if (egg != null) builder.addSlot(RecipeIngredientRole.CATALYST, 40, 25).setStandardSlotBackground().addItemStack(new ItemStack(egg))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.appliedagristics.mob_reference")));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.SAMPLER.get(), id));
        } else if (type == AgristicsJei.CULTURE) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.SAMPLER.get(), id));
            builder.addSlot(RecipeIngredientRole.INPUT, 25, 25).setStandardSlotBackground().addItemStack(new ItemStack(Agristics.NUTRIENT.get()));
            builder.addSlot(RecipeIngredientRole.INPUT, 46, 25).setStandardSlotBackground().addItemStack(new ItemStack(Agristics.CAPSULE.get()));
            builder.addSlot(RecipeIngredientRole.INPUT, 67, 25).setStandardSlotBackground().addFluidStack(Fluids.WATER, Balance.CULTURE_WATER.get()).setFluidRenderer(Balance.CULTURE_WATER.get(), false, 16, 16);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.CULTURE.get(), id));
        } else if (type == AgristicsJei.EXTRACTION) {
            builder.addSlot(RecipeIngredientRole.INPUT, 32, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.CULTURE.get(), id));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.EXTRACT.get(), id));

        } else {
            builder.addSlot(RecipeIngredientRole.CATALYST, 32, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.BIO_SEED.get(), id));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), id));
        }
    }
    @Override public void draw(AgristicsJei.Process recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(BuiltInRegistries.ENTITY_TYPE.get(recipe.entity()).getDescription().getString(), 164), 1, 3, 0x404040, false);
        if(type==AgristicsJei.RITUAL) {
            arrow.draw(graphics,100,35);
            graphics.drawString(font,Component.translatable("jei.appliedagristics.ritual_help"),1,70,0x404040,false);return;
        }
        arrow.draw(graphics, 94, 25);
        if(type==AgristicsJei.EXTRACTION) {
            graphics.drawString(font,Component.translatable("jei.appliedagristics.throw_help"),1,52,0x404040,false);
            graphics.drawString(font,Component.translatable("jei.appliedagristics.nourishment_help"),1,68,0x404040,false);return;
        }
        if (type == AgristicsJei.CULTURE) {
            int ticks = Balance.CULTURE_TICKS.get();
            graphics.drawString(font, Component.translatable("jei.appliedagristics.culture_duration", String.format(java.util.Locale.ROOT,"%.1f",ticks / 20.0)), 1, 52, 0x404040, false);
            graphics.drawString(font, Component.translatable("jei.appliedagristics.single_use"), 1, 68, 0x404040, false);
        } else {
            graphics.drawString(font, Component.translatable("jei.appliedagristics." + (type == AgristicsJei.SAMPLING ? "sampling_help" : "planting_help")), 1, 52, 0x404040, false);
        }
    }
}
