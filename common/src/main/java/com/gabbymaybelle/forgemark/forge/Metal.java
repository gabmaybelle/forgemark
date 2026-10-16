package com.gabbymaybelle.forgemark.forge;

import net.minecraft.world.item.Item;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.gabbymaybelle.forgemark.ForgemarkTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * How each metal behaves at the forge, read from tags so packs and other mods can tune or add
 * metals with plain data files: how long an ingot takes to reach forging heat, and how many
 * strikes it takes before it dims and needs reheating. Anything heatable that's in none of the
 * tags gets the defaults.
 */
public final class Metal {
	public static final int DEFAULT_HEAT_TICKS = 200;
	public static final int QUICK_HEAT_TICKS = 120;
	public static final int SLOW_HEAT_TICKS = 400;
	public static final int DEFAULT_STRIKES_PER_HEAT = 3;
	public static final int SOFT_STRIKES_PER_HEAT = 2;
	public static final int HARD_STRIKES_PER_HEAT = 4;

	private Metal() {
	}

	/**
	 * Whether the hearth takes this item: anything in the heatable tag (vanilla ingots and gems,
	 * the shared c:ingots and c:gems tags), or anything that looks like a metal by name, so a
	 * mod that skips the shared tags still joins in. Leather, planks, cobblestone and membrane
	 * repair things too, and none of them pass; the tag is there for odd names.
	 */
	public static boolean isHeatable(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(ForgemarkTags.HEATABLE) || looksLikeMetal(stack));
	}

	/** An item id ending in "ingot" or "gem", the way nearly every mod names its metals. */
	public static boolean looksLikeMetal(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return path.endsWith("ingot") || path.endsWith("_gem");
	}

	/** Ticks from cold to forging heat. */
	public static int heatTicks(ItemStack ingot) {
		if (ingot.is(ForgemarkTags.HEATS_QUICKLY)) {
			return QUICK_HEAT_TICKS;
		}
		if (ingot.is(ForgemarkTags.HEATS_SLOWLY)) {
			return SLOW_HEAT_TICKS;
		}
		return DEFAULT_HEAT_TICKS;
	}

	/** Strikes one heat is good for. */
	public static int strikesPerHeat(ItemStack ingot) {
		if (ingot.is(ForgemarkTags.SOFT_METAL)) {
			return SOFT_STRIKES_PER_HEAT;
		}
		if (ingot.is(ForgemarkTags.HARD_METAL)) {
			return HARD_STRIKES_PER_HEAT;
		}
		return DEFAULT_STRIKES_PER_HEAT;
	}

	/**
	 * Whether this ingot (or gem) repairs this item at an anvil, so it's the one that tempers it.
	 * On 1.21.1 that is the item's own rule: a tool's tier or armour's material says which
	 * ingredient mends it, and modded gear answers the same question by overriding it.
	 */
	public static boolean repairs(ItemStack item, ItemStack ingot) {
		return !item.isEmpty() && !ingot.isEmpty() && item.getItem().isValidRepairItem(item, ingot);
	}

	private static final Map<Item, Boolean> HEATABLE_REPAIR = new ConcurrentHashMap<>();

	/**
	 * Whether anything heatable repairs this item, so the forge can temper it. 1.21.1 has no list
	 * of repair items on the item, so every heatable item is asked once per kind of item (the
	 * answer is kept: it can't change while the game runs).
	 */
	public static boolean hasHeatableRepairMaterial(ItemStack item) {
		return HEATABLE_REPAIR.computeIfAbsent(item.getItem(), kind -> {
			for (Item candidate : BuiltInRegistries.ITEM) {
				ItemStack ingot = new ItemStack(candidate);
				if (isHeatable(ingot) && repairs(item, ingot)) {
					return true;
				}
			}
			return false;
		});
	}
}
