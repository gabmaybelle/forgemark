package com.gabbymaybelle.forgemark.compat;

import com.gabbymaybelle.forgemark.forge.Metal;
import com.gabbymaybelle.forgemark.quality.Quality;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the recipe viewers show: for every ingot or gem the hearth takes, the items it tempers.
 * Worked out from the items themselves (anything temperable that the ingot repairs), so other
 * mods' gear is listed without per-mod code. The viewers' plugins ({@code JeiPlugin},
 * {@code ReiPlugin}) build their categories from this; nothing here imports a viewer.
 */
public final class TemperingEntries {
	/** One ingot and the items it tempers, in registry order. */
	public record Entry(ItemStack ingot, List<ItemStack> items) {
	}

	private TemperingEntries() {
	}

	/** Every heatable ingot that tempers at least one item, in registry order. */
	public static List<Entry> all() {
		List<ItemStack> ingots = new ArrayList<>();
		List<ItemStack> temperable = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = new ItemStack(item);
			if (Metal.isHeatable(stack)) {
				ingots.add(stack);
			}
			if (Quality.isTemperable(stack)) {
				temperable.add(stack);
			}
		}
		Map<Item, Entry> entries = new LinkedHashMap<>();
		for (ItemStack ingot : ingots) {
			List<ItemStack> items = new ArrayList<>();
			for (ItemStack stack : temperable) {
				if (Metal.repairs(stack, ingot)) {
					items.add(stack);
				}
			}
			if (!items.isEmpty()) {
				entries.put(ingot.getItem(), new Entry(ingot, List.copyOf(items)));
			}
		}
		return List.copyOf(entries.values());
	}
}
