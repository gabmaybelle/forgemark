package com.gabbymaybelle.forgemark.network;

import com.gabbymaybelle.forgemark.Forgemark;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The press: the client says which anvil it struck and where it saw the marker. The server
 * checks the player, the anvil and the timing before anything happens
 * ({@code ForgingAnvilBlockEntity.strike}).
 */
public record StrikePayload(BlockPos pos, float markerPosition) implements CustomPacketPayload {
	public static final Type<StrikePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Forgemark.MOD_ID, "strike"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StrikePayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, StrikePayload::pos,
			ByteBufCodecs.FLOAT, StrikePayload::markerPosition,
			StrikePayload::new);

	@Override
	public Type<StrikePayload> type() {
		return TYPE;
	}
}
