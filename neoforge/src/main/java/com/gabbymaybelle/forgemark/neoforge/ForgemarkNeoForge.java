package com.gabbymaybelle.forgemark.neoforge;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entrypoint: everything happens in {@code common}. */
@Mod(Forgemark.MOD_ID)
public class ForgemarkNeoForge {
	public ForgemarkNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = (NeoForgePlatform) Services.PLATFORM;
		// init() queues the registrations; the registry events hand them to NeoForge.
		Forgemark.init();
		modBus.addListener(RegisterEvent.class, platform::onRegister);
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, platform::onCreativeTab);
	}
}
