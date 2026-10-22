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
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(type == AgristicsJei.SAMPLING ? Agristics.SAMPLER.get() : type == AgristicsJei.PLANTING ? Agristics.BIO_SEED.get() : Agristics.CULTIVATOR.get().asItem()));
        arrow = gui.createAnimatedRecipeArrow(80);
    }
    @Override public RecipeType<AgristicsJei.Process> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.appliedagristics." + type.getUid().getPath()); }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 94; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, AgristicsJei.Process recipe, IFocusGroup focuses) {
        var id = recipe.entity();
        if (type == AgristicsJei.SAMPLING) {
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
            builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 25).setStandardSlotBackground().addItemStack(new ItemStack(Agristics.CAPSULE.get()));
        } else {
            builder.addSlot(RecipeIngredientRole.CATALYST, 32, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.BIO_SEED.get(), id));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 25).setStandardSlotBackground().addItemStack(BioData.withIdentity(Agristics.BIO_ESSENCE.get(), id));
        }
    }
    @Override public void draw(AgristicsJei.Process recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(BuiltInRegistries.ENTITY_TYPE.get(recipe.entity()).getDescription().getString(), 164), 1, 3, 0x404040, false);
        arrow.draw(graphics, 94, 25);
        if (type == AgristicsJei.CULTURE || type == AgristicsJei.EXTRACTION) {
            int ticks = type == AgristicsJei.CULTURE ? Balance.CULTURE_TICKS.get() : Balance.EXTRACT_TICKS.get();
            int power = type == AgristicsJei.CULTURE ? Balance.CULTURE_POWER.get() : Balance.EXTRACT_POWER.get();
            graphics.drawString(font, Component.translatable("jei.appliedagristics.process_cost", ticks / 20.0, power), 1, 52, 0x404040, false);
            graphics.drawString(font, Component.translatable("jei.appliedagristics.single_use"), 1, 68, 0x404040, false);
        } else {
            graphics.drawString(font, Component.translatable("jei.appliedagristics." + (type == AgristicsJei.SAMPLING ? "sampling_help" : "planting_help")), 1, 52, 0x404040, false);
        }
    }
}
