package com.gabbymaybelle.forgemark.neoforge.client;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.client.ForgeRenderers;
import com.gabbymaybelle.forgemark.client.ForgingClient;
import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half on NeoForge: the settings packet handler (registered in the main entrypoint)
 * takes the server's settings when joining and this puts the file's back when leaving; plus
 * the strike bar, the press, and what sits on the hearth and anvil. Only loaded on the client,
 * so a dedicated server never sees these classes.
 */
@Mod(value = Forgemark.MOD_ID, dist = Dist.CLIENT)
public class ForgemarkNeoForgeClient {
	public ForgemarkNeoForgeClient(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> Config.load());
		ForgingAnvilBlock.clientStrike = ForgingClient::sendStrike;
		modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAboveAll(ForgingClient.BAR_ID, ForgingClient::render));
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			event.registerBlockEntityRenderer(ForgemarkBlocks.HEARTH_ENTITY.get(), ForgeRenderers.Hearth::new);
			event.registerBlockEntityRenderer(ForgemarkBlocks.FORGING_ANVIL_ENTITY.get(), ForgeRenderers.Anvil::new);
		});
	}
}
