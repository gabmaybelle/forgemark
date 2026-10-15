package com.gabbymaybelle.forgemark.client;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.forge.Forging;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.network.StrikePayload;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The strike bar: drawn over the hotbar while the player looks at a forging anvil that holds
 * an item and an ingot. The marker's position comes from the anvil's sweep start and the
 * client's game time, the zones from the anvil, so the bar needs no packet; the press sends
 * the marker position the player saw, and the server decides. Colour-blind safe: blue wide
 * zone, orange narrow zone, white marker.
 */
public final class ForgingClient {
	public static final Identifier BAR_ID = Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "strike_bar");
	private static final int WIDTH = 120;
	private static final int HEIGHT = 10;
	private static final int BACKGROUND = 0xCC202020;
	private static final int WIDE = 0xFF5B8DEF;
	private static final int NARROW = 0xFFF5A623;
	private static final int MARKER = 0xFFFFFFFF;
	private static final int HEAT_HOT = 0xFFFF8C1A;
	private static final int HEAT_DIM = 0xFF803000;
	private static final int HEAT_TRACK = 0xFF303030;
	private static final int MISS = 0xFF666666;
	private static float lastPartialTick;

	private ForgingClient() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		lastPartialTick = delta.getGameTimeDeltaPartialTick(false);
		ForgingAnvilBlockEntity anvil = targetAnvil(minecraft);
		if (anvil == null || minecraft.level == null) {
			return;
		}
		long now = minecraft.level.getGameTime();
		int x = (graphics.guiWidth() - WIDTH) / 2;
		int y = graphics.guiHeight() - 104; // above the action bar messages and the held item's name
		graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 10, BACKGROUND);
		float center = anvil.getZoneCenter();
		graphics.fill(x + Math.round((center - Forging.WIDE_ZONE) * WIDTH), y, x + Math.round((center + Forging.WIDE_ZONE) * WIDTH), y + HEIGHT, WIDE);
		graphics.fill(x + Math.round((center - Forging.NARROW_ZONE) * WIDTH), y, x + Math.round((center + Forging.NARROW_ZONE) * WIDTH), y + HEIGHT, NARROW);
		int marker = x + Math.round(Forging.markerPosition(now - anvil.getSweepStart() + lastPartialTick) * WIDTH);
		graphics.fill(marker - 1, y - 2, marker + 1, y + HEIGHT + 2, MARKER);

		float heat = anvil.ingotHeat(now);
		boolean hot = anvil.isHotEnough(now);
		graphics.fill(x, y + HEIGHT + 4, x + WIDTH, y + HEIGHT + 7, HEAT_TRACK);
		graphics.fill(x, y + HEIGHT + 4, x + Math.round(heat * WIDTH), y + HEIGHT + 7, hot ? HEAT_HOT : HEAT_DIM);

		List<Integer> strikes = anvil.getStrikes();
		for (int i = 0; i < Forging.STRIKES; i++) {
			int dotX = x + WIDTH + 6 + i * 6;
			int colour = i < strikes.size() ? switch (strikes.get(i)) {
				case Forging.PERFECT -> NARROW;
				case Forging.GOOD -> WIDE;
				default -> MISS;
			} : HEAT_TRACK;
			graphics.fill(dotX, y + 3, dotX + 4, y + 7, colour);
		}

		Component text = hot
				? Component.translatable("forgemark.bar.strike", strikes.size() + 1, Forging.STRIKES)
				: Component.translatable("forgemark.bar.reheat");
		graphics.text(minecraft.font, text, (graphics.guiWidth() - minecraft.font.width(text)) / 2, y - 14, MARKER);
	}

	/** The press: tells the server where the marker was when the player clicked. */
	public static void sendStrike(BlockPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientPacketListener connection = minecraft.getConnection();
		if (minecraft.level == null || connection == null || !(minecraft.level.getBlockEntity(pos) instanceof ForgingAnvilBlockEntity anvil)) {
			return;
		}
		float position = Forging.markerPosition(minecraft.level.getGameTime() - anvil.getSweepStart() + lastPartialTick);
		connection.send(new ServerboundCustomPayloadPacket(new StrikePayload(pos, position)));
	}

	private static @Nullable ForgingAnvilBlockEntity targetAnvil(Minecraft minecraft) {
		if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)) {
			return null;
		}
		return minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof ForgingAnvilBlockEntity anvil && anvil.canStrike() ? anvil : null;
	}
}
