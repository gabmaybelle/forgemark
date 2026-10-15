package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.platform.Services;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod in a real game. Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land
 * in fabric/build/run/clientGameTest/screenshots. Each feature adds its own checks here as it
 * lands, and each check is proven by running it once with the feature switched off.
 */
public class ForgemarkClientTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		settingsFile();
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			boolean loaded = world.getServer().computeOnServer(server -> FabricLoader.getInstance().isModLoaded(Forgemark.MOD_ID));
			check(loaded, "the mod is loaded in a new world");
			check(Services.PLATFORM.name().equals("Fabric"), "the Fabric platform is the one in use");
			ctx.takeScreenshot("forgemark_in_world");
		}
		if (!failures.isEmpty()) {
			throw new AssertionError(failures.size() + " check(s) failed:\n - " + String.join("\n - ", failures));
		}
	}

	/** The settings file is written with the defaults, survives a typo, and keeps unknown keys. */
	private void settingsFile() {
		Path file = Config.path();
		check(Files.exists(file), "the settings file is written on first start");
		check(Config.bonusScale == 100 && Config.maxTier.equals("masterwork") && Config.noTimingCost == 2,
				"the settings default to bonus scale 100, maximum tier masterwork, no-timing cost 2");
		try {
			String original = Files.readString(file, StandardCharsets.UTF_8);
			Files.writeString(file, original.replace("bonusScale=100", "bonusScale=lots").replace("maxTier=masterwork", "maxTier=legendary")
					+ "someFutureSetting=yes\n", StandardCharsets.UTF_8);
			Config.load();
			check(Config.bonusScale == 100, "a number with a typo falls back to its default");
			check(Config.maxTier.equals("masterwork"), "an unknown tier name falls back to masterwork");
			String rewritten = Files.readString(file, StandardCharsets.UTF_8);
			check(rewritten.contains("bonusScale=lots"), "a typo is left in the file for the player to fix (not overwritten)");
			check(rewritten.contains("someFutureSetting=yes"), "a setting this version doesn't know is kept");
			Files.writeString(file, original, StandardCharsets.UTF_8);
			Config.load();
		} catch (IOException e) {
			failures.add("couldn't edit the settings file: " + e);
		}
	}

	private void check(boolean ok, String what) {
		if (ok) {
			Forgemark.LOGGER.info("[test] ok: {}", what);
		} else {
			Forgemark.LOGGER.error("[test] FAILED: {}", what);
			failures.add(what);
		}
	}
}
