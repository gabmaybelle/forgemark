package com.gabbymaybelle.forgemark.compat;

import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI's "Tempering" tab: one entry per ingot or gem, with the items it tempers, from
 * {@link TemperingEntries}. Found through the {@code jei_mod_plugin} entrypoint on Fabric and
 * the annotation on NeoForge, and only loaded when JEI is installed; its API is compile-only.
 */
@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
	public static final RecipeType<TemperingEntries.Entry> TEMPERING = RecipeType.create("forgemark", "tempering", TemperingEntries.Entry.class);

	@Override
	public ResourceLocation getPluginUid() {
		return ForgemarkBlocks.id("jei");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(TEMPERING, TemperingEntries.all());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalysts(TEMPERING, ForgemarkBlocks.HEARTH_ITEM.get(), ForgemarkBlocks.FORGING_ANVIL_ITEM.get(), ForgemarkBlocks.SMITHING_HAMMER.get());
	}

	public static class Category extends AbstractRecipeCategory<TemperingEntries.Entry> {
		private final IDrawable arrow;

		Category(IGuiHelper gui) {
			super(TEMPERING, Component.translatable("forgemark.category.tempering"), gui.createDrawableItemLike(ForgemarkBlocks.FORGING_ANVIL_ITEM.get()), 100, 18);
			arrow = gui.getRecipeArrow();
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, TemperingEntries.Entry entry, IFocusGroup focuses) {
			builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(entry.ingot());
			builder.addSlot(RecipeIngredientRole.CATALYST, 23, 1).setStandardSlotBackground().addItemLike(ForgemarkBlocks.SMITHING_HAMMER.get());
			builder.addOutputSlot(78, 1).setOutputSlotBackground().addItemStacks(entry.items());
		}

		@Override
		public void draw(TemperingEntries.Entry entry, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
			arrow.draw(graphics, 46, 1);
		}
	}
}
