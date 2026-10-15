package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.platform.Services;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code forgemark:glowing} component: a freshly forged item is still hot until this game
 * time, and dipping it in a water cauldron before then quenches it. After that it has simply
 * air-cooled, and the component is ignored and dropped at the next opportunity.
 */
public final class Glowing {
	/** How long a finished item stays hot enough to quench: 15 seconds. */
	public static final int TICKS = 300;

	public static final Identifier ID = Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "glowing");
	private static final Supplier<DataComponentType<?>> REGISTERED = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, ID,
			() -> DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	private Glowing() {
	}

	public static void register() {
	}

	@SuppressWarnings("unchecked")
	public static DataComponentType<Long> type() {
		return (DataComponentType<Long>) REGISTERED.get();
	}

	public static void put(ItemStack stack, long gameTime) {
		stack.set(type(), gameTime + TICKS);
	}

	/** Whether the item is still hot enough to quench. */
	public static boolean isHot(ItemStack stack, long gameTime) {
		Long until = stack.get(type());
		return until != null && gameTime <= until;
	}

	public static void clear(ItemStack stack) {
		stack.remove(type());
	}
}
