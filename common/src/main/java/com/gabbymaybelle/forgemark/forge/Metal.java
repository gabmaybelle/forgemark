package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.ForgemarkTags;
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

	/** Whether the hearth takes this item. */
	public static boolean isHeatable(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ForgemarkTags.HEATABLE);
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
}
