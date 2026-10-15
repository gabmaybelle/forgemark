package com.gabbymaybelle.forgemark.neoforge;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.ForgemarkCommands;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.network.SettingsPayload;
import com.gabbymaybelle.forgemark.network.StrikePayload;
import com.gabbymaybelle.forgemark.platform.Services;
import com.gabbymaybelle.forgemark.quality.Temper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.tooltip.TooltipAppender;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The NeoForge entrypoint: everything happens in {@code common}; this wires its pieces into
 * NeoForge's events. The client-only half is {@code neoforge.client.ForgemarkNeoForgeClient}.
 */
@Mod(Forgemark.MOD_ID)
public class ForgemarkNeoForge {
	public ForgemarkNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = (NeoForgePlatform) Services.PLATFORM;
		// init() queues the registrations; the registry events hand them to NeoForge.
		Forgemark.init();
		modBus.addListener(RegisterEvent.class, platform::onRegister);
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, platform::onCreativeTab);
		modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
				.playToClient(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC, (payload, context) -> Config.applyFromServer(payload))
				.playToServer(StrikePayload.TYPE, StrikePayload.STREAM_CODEC, (payload, context) -> {
					if (context.player() instanceof ServerPlayer player) {
						ForgingAnvilBlockEntity.handleStrike(player, payload);
					}
				}));
		// The temper's own tooltip lines, just above the enchantments.
		modBus.addListener(RegisterTooltipAppendersEvent.class, event -> event.registerComponentAppenderBefore(
				Temper.type(), DataComponents.ENCHANTMENTS, TooltipAppender.createComponentAppender(Temper.type())));
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> ForgemarkCommands.register(event.getDispatcher()));
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				PacketDistributor.sendToPlayer(player, SettingsPayload.current());
			}
		});
	}
}
