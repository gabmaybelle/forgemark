package com.gabbymaybelle.forgemark.fabric.client;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.network.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The client half on Fabric: take the server's settings when joining, the file's back when leaving. */
public class ForgemarkFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> Config.applyFromServer(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Config.load());
	}
}
