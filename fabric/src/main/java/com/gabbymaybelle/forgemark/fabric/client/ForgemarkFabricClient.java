package com.gabbymaybelle.forgemark.fabric.client;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.client.ForgeRenderers;
import com.gabbymaybelle.forgemark.client.ForgingClient;
import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlock;
import com.gabbymaybelle.forgemark.network.SettingsPayload;
import com.gabbymaybelle.forgemark.quality.Quality;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * The client half on Fabric: the server's settings on join and the file's back on leaving,
 * the strike bar, the press, and what sits on the hearth and anvil.
 */
public class ForgemarkFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> Config.applyFromServer(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Config.load());
		HudRenderCallback.EVENT.register(ForgingClient::render);
		// The temper's lines under the item's name (1.21.1 draws no tooltip for a mod's own component).
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> Quality.addTooltip(stack, lines));
		ForgingAnvilBlock.clientStrike = ForgingClient::sendStrike;
		BlockEntityRenderers.register(ForgemarkBlocks.HEARTH_ENTITY.get(), ForgeRenderers.Hearth::new);
		BlockEntityRenderers.register(ForgemarkBlocks.FORGING_ANVIL_ENTITY.get(), ForgeRenderers.Anvil::new);
	}
}
