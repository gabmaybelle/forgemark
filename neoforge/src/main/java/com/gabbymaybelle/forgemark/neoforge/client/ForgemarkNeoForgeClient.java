package com.gabbymaybelle.forgemark.neoforge.client;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.Forgemark;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half on NeoForge: the settings packet handler (registered in the main entrypoint)
 * takes the server's settings when joining; this puts the file's back when leaving. Only loaded
 * on the client, so a dedicated server never sees these classes.
 */
@Mod(value = Forgemark.MOD_ID, dist = Dist.CLIENT)
public class ForgemarkNeoForgeClient {
	public ForgemarkNeoForgeClient() {
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> Config.load());
	}
}
