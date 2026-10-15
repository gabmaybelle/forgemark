package com.gabbymaybelle.forgemark.network;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.Forgemark;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The settings a client needs to show and work out the same numbers as the server: sent to
 * every player when they join, so the server's values win. The loaders register the type and
 * send it; {@link Config#applyFromServer} takes it in on the client.
 */
public record SettingsPayload(int bonusScale, String maxTier, boolean makersMark) implements CustomPacketPayload {
	public static final Type<SettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SettingsPayload::bonusScale,
			ByteBufCodecs.STRING_UTF8, SettingsPayload::maxTier,
			ByteBufCodecs.BOOL, SettingsPayload::makersMark,
			SettingsPayload::new);

	/** This side's current settings, ready to send. */
	public static SettingsPayload current() {
		return new SettingsPayload(Config.bonusScale, Config.maxTier, Config.makersMark);
	}

	@Override
	public Type<SettingsPayload> type() {
		return TYPE;
	}
}
