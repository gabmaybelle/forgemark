package com.gabbymaybelle.forgemark.fabric;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.ForgemarkCommands;
import com.gabbymaybelle.forgemark.network.SettingsPayload;
import com.gabbymaybelle.forgemark.quality.Temper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;

/**
 * The Fabric entrypoint: everything happens in {@code common}; this wires its pieces into
 * Fabric's events. The client-only half is {@code fabric.client.ForgemarkFabricClient}.
 */
public class ForgemarkFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Forgemark.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ForgemarkCommands.register(dispatcher));
		PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(SettingsPayload.current()));
		// The temper's own tooltip lines, just above the enchantments.
		ItemComponentTooltipProviderRegistry.addBefore(DataComponents.ENCHANTMENTS, Temper.type());
	}
}
