package com.gabbymaybelle.forgemark.quality;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * The quality tiers, with each one's bonuses before the quench and the bonus-scale setting: the
 * "Quality tiers" table of the design doc. Durability, mining speed and attack damage are
 * percentages of the item's own stat; toughness is flat, because iron, gold and chain armour
 * have none to scale. The order matters: a higher ordinal is a better tier.
 */
public enum Tier implements StringRepresentable {
	ROUGH(-5, 0, 0, 0.0, ChatFormatting.GRAY),
	STANDARD(0, 0, 0, 0.0, ChatFormatting.WHITE),
	FINE(5, 3, 3, 0.1, ChatFormatting.GREEN),
	SUPERIOR(10, 6, 6, 0.2, ChatFormatting.AQUA),
	MASTERWORK(15, 8, 8, 0.4, ChatFormatting.GOLD);

	public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);
	public static final StreamCodec<ByteBuf, Tier> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Tier::ordinal);

	public final int durabilityPercent;
	public final int miningSpeedPercent;
	public final int attackDamagePercent;
	public final double toughness;
	public final ChatFormatting colour;

	Tier(int durabilityPercent, int miningSpeedPercent, int attackDamagePercent, double toughness, ChatFormatting colour) {
		this.durabilityPercent = durabilityPercent;
		this.miningSpeedPercent = miningSpeedPercent;
		this.attackDamagePercent = attackDamagePercent;
		this.toughness = toughness;
		this.colour = colour;
	}

	/** The tier called {@code name} in the settings file or a command, or null. */
	public static @Nullable Tier byName(String name) {
		for (Tier tier : values()) {
			if (tier.getSerializedName().equals(name.trim().toLowerCase(Locale.ROOT))) {
				return tier;
			}
		}
		return null;
	}

	public Tier min(Tier other) {
		return this.ordinal() <= other.ordinal() ? this : other;
	}

	public boolean isBetterThan(Tier other) {
		return this.ordinal() > other.ordinal();
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** "Fine", "Masterwork"... from the lang file, in the tier's colour. */
	public MutableComponent displayName() {
		return Component.translatable("forgemark.tier." + getSerializedName()).withStyle(colour);
	}
}
