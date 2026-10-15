package com.gabbymaybelle.forgemark.compat;

import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * REI's "Tempering" tab: one display per ingot or gem, with the items it tempers, from
 * {@link TemperingEntries}. Found through the {@code rei_client} entrypoint on Fabric; on NeoForge
 * {@code neoforge.compat.ReiNeoForgePlugin} extends this with REI's annotation. Only loaded
 * when REI is installed; its API is compile-only.
 */
public class ReiPlugin implements REIClientPlugin {
	public static final CategoryIdentifier<Display> TEMPERING = CategoryIdentifier.of(ForgemarkBlocks.id("tempering"));

	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.add(new Category());
		registry.addWorkstations(TEMPERING, EntryIngredients.of(ForgemarkBlocks.HEARTH_ITEM.get()),
				EntryIngredients.of(ForgemarkBlocks.FORGING_ANVIL_ITEM.get()), EntryIngredients.of(ForgemarkBlocks.SMITHING_HAMMER.get()));
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		for (TemperingEntries.Entry entry : TemperingEntries.all()) {
			registry.add(new Display(entry));
		}
	}

	/** An ingot in, the items it tempers out. The display never leaves the client, so it has no serializer. */
	public static class Display extends BasicDisplay {
		public Display(TemperingEntries.Entry entry) {
			super(List.of(EntryIngredients.of(entry.ingot())), List.of(EntryIngredients.ofItemStacks(entry.items())));
		}

		@Override
		public CategoryIdentifier<?> getCategoryIdentifier() {
			return TEMPERING;
		}

		@Override
		public @Nullable DisplaySerializer<? extends me.shedaniel.rei.api.common.display.Display> getSerializer() {
			return null;
		}
	}

	public static class Category implements DisplayCategory<Display> {
		@Override
		public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
			return TEMPERING;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("forgemark.category.tempering");
		}

		@Override
		public Renderer getIcon() {
			return EntryStacks.of(ForgemarkBlocks.FORGING_ANVIL_ITEM.get());
		}

		@Override
		public int getDisplayHeight() {
			return 44;
		}

		@Override
		public List<Widget> setupDisplay(Display display, Rectangle bounds) {
			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			int x = bounds.getCenterX() - 41;
			int y = bounds.getCenterY() - 9;
			widgets.add(Widgets.createSlot(new Point(x, y)).entries(display.getInputEntries().getFirst()).markInput());
			widgets.add(Widgets.createSlot(new Point(x + 22, y)).entry(EntryStacks.of(ForgemarkBlocks.SMITHING_HAMMER.get())));
			widgets.add(Widgets.createArrow(new Point(x + 44, y)));
			widgets.add(Widgets.createResultSlotBackground(new Point(x + 74, y)));
			widgets.add(Widgets.createSlot(new Point(x + 74, y)).entries(display.getOutputEntries().getFirst()).disableBackground().markOutput());
			return widgets;
		}
	}
}
