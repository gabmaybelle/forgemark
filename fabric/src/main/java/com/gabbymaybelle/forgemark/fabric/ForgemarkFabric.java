package com.gabbymaybelle.forgemark.fabric;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.ForgemarkCommands;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.network.SettingsPayload;
import com.gabbymaybelle.forgemark.network.StrikePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The Fabric entrypoint: everything happens in {@code common}; this wires its pieces into
 * Fabric's events. The client-only half is {@code fabric.client.ForgemarkFabricClient}.
 */
public class ForgemarkFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Forgemark.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ForgemarkCommands.register(dispatcher));
		PayloadTypeRegistry.playS2C().register(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(StrikePayload.TYPE, StrikePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(StrikePayload.TYPE, (payload, context) -> ForgingAnvilBlockEntity.handleStrike(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(SettingsPayload.current()));
	}
}
