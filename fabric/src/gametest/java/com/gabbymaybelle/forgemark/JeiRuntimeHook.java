package com.gabbymaybelle.forgemark;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * JEI hands its runtime to a plugin, so the test mod has one (its {@code jei_mod_plugin}
 * entrypoint). Only JEI loads this class; {@link ViewerScreens} reads the runtime from here.
 */
public final class JeiRuntimeHook implements IModPlugin {
	static @Nullable IJeiRuntime runtime;

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath("forgemark_gametest", "viewer_screens");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		JeiRuntimeHook.runtime = runtime;
	}
}
