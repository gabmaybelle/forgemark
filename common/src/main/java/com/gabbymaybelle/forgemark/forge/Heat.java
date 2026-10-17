package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The {@code forgemark:heat} component on an ingot that has been in the hearth: how hot it was
 * (0 cold to 1 forging heat) and when, and how many strikes this heat has already given. Nothing
 * ticks it; the heat it has now is worked out from the game time, so an ingot cools in a pocket or
 * on the anvil just the same, and goes cold in {@link #COOLING_TICKS}. A heat gives the metal's
 * strikes per heat while the ingot still glows ({@link #GLOWING}); a trip to the hearth starts a
 * new count.
 */
public record Heat(float heat, long at, int strikes) {
	/** How long a fully heated ingot takes to go cold out of the hearth: 30 seconds. */
	public static final int COOLING_TICKS = 600;
	/** The heat an ingot glows at, and can still be struck at: 18 seconds after a full heat. */
	public static final float GLOWING = 0.4F;

	public static final Codec<Heat> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.FLOAT.fieldOf("heat").forGetter(Heat::heat),
			Codec.LONG.fieldOf("at").forGetter(Heat::at),
			// Ingots heated before 0.1.4 have no count: they start at none used.
			Codec.INT.optionalFieldOf("strikes", 0).forGetter(Heat::strikes)
	).apply(i, Heat::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, Heat> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Heat::heat,
			ByteBufCodecs.VAR_LONG, Heat::at,
			ByteBufCodecs.VAR_INT, Heat::strikes,
			Heat::new);

	public static final Identifier ID = Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "heat");
	private static final Supplier<DataComponentType<?>> REGISTERED = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, ID,
			() -> DataComponentType.<Heat>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

	public static void register() {
	}

	@SuppressWarnings("unchecked")
	public static DataComponentType<Heat> type() {
		return (DataComponentType<Heat>) REGISTERED.get();
	}

	/** The heat an item has now, 0 when it was never heated. */
	public static float now(ItemStack stack, long gameTime) {
		Heat heat = stack.get(type());
		return heat == null ? 0.0F : heat.at(gameTime);
	}

	/** Sets a fresh heat as of now, with no strikes used; cold ingots lose the component so they stack again. */
	public static void put(ItemStack stack, float heat, long gameTime) {
		if (heat <= 0.0F) {
			stack.remove(type());
		} else {
			stack.set(type(), new Heat(Math.min(1.0F, heat), gameTime, 0));
		}
	}

	/** How many strikes the ingot's current heat has given. */
	public static int strikesUsed(ItemStack stack) {
		Heat heat = stack.get(type());
		return heat == null ? 0 : heat.strikes();
	}

	/** One strike more from this heat. The ingot keeps cooling as it was; a strike takes none of its heat. */
	public static void struck(ItemStack stack) {
		Heat heat = stack.get(type());
		if (heat != null) {
			stack.set(type(), new Heat(heat.heat(), heat.at(), heat.strikes() + 1));
		}
	}

	public static @Nullable Heat on(ItemStack stack) {
		return stack.get(type());
	}

	/** The heat left at a later game time. */
	public float at(long gameTime) {
		long elapsed = Math.max(0, gameTime - at);
		return Math.max(0.0F, heat - (float) elapsed / COOLING_TICKS);
	}
}
