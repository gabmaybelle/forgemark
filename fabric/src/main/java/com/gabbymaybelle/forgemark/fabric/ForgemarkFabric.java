package com.gabbymaybelle.forgemark.fabric;

import com.gabbymaybelle.forgemark.Forgemark;
import net.fabricmc.api.ModInitializer;

/** The Fabric entrypoint: everything happens in {@code common}. */
public class ForgemarkFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Forgemark.init();
	}
}
