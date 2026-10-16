package com.gabbymaybelle.forgemark.quality;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The one thing Forgemark saves on an item: the {@code forgemark:temper} data component, holding
 * the tier, whether the item was water-quenched, and who made it (Masterwork only). No stat
 * numbers are stored; {@link Quality} works them out from the tier and the item's own base stats
 * whenever they're needed, so a netherite upgrade or a changed setting is always right.
 *
 * <p>Its tooltip lines come from {@link Quality#addTooltip}, which the loaders call from their
 * tooltip events; it never touches the item's name or rarity.
 */
public record Temper(Tier tier, boolean quenched, Optional<String> maker) {
	public static final Codec<Temper> CODEC = RecordCodecBuilder.create(i -> i.group(
			Tier.CODEC.fieldOf("tier").forGetter(Temper::tier),
			Codec.BOOL.optionalFieldOf("quenched", false).forGetter(Temper::quenched),
			Codec.STRING.optionalFieldOf("maker").forGetter(Temper::maker)
	).apply(i, Temper::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, Temper> STREAM_CODEC = StreamCodec.composite(
			Tier.STREAM_CODEC, Temper::tier,
			ByteBufCodecs.BOOL, Temper::quenched,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Temper::maker,
			Temper::new);

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Forgemark.MOD_ID, "temper");
	private static final Supplier<DataComponentType<?>> REGISTERED = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, ID,
			() -> DataComponentType.<Temper>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

	/** Loads this class, which queues the component's registration. */
	public static void register() {
	}

	@SuppressWarnings("unchecked")
	public static DataComponentType<Temper> type() {
		return (DataComponentType<Temper>) REGISTERED.get();
	}

	public static Temper of(Tier tier, boolean quenched, @Nullable String maker) {
		return new Temper(tier, quenched, tier == Tier.MASTERWORK ? Optional.ofNullable(maker) : Optional.empty());
	}

	/** The temper on an item, or null when it was never tempered. */
	public static @Nullable Temper on(DataComponentHolder item) {
		return item.get(type());
	}

	public static void put(ItemStack stack, @Nullable Temper temper) {
		if (temper == null) {
			stack.remove(type());
		} else {
			stack.set(type(), temper);
		}
	}

	/**
	 * The better of two tempers, for the anvil: the higher tier wins, a quench breaks a tie, and
	 * otherwise {@code a} (the item being worked on) keeps its own, maker's name included.
	 */
	public static @Nullable Temper better(@Nullable Temper a, @Nullable Temper b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		if (b.tier.isBetterThan(a.tier) || (b.tier == a.tier && b.quenched && !a.quenched)) {
			return b;
		}
		return a;
	}

}
