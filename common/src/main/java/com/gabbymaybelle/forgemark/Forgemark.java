package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forgemark: temper the tools, weapons and armour you already have, from any mod, at a hearth
 * and forging anvil, for a small, capped quality bonus. The loader entrypoints ({@code fabric}
 * and {@code neoforge}) call {@link #init()} once; everything that doesn't care which loader it
 * runs on lives here in {@code common}.
 */
public final class Forgemark {
	public static final String MOD_ID = "forgemark";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Forgemark() {
	}

	public static void init() {
		Config.load();
		LOGGER.info("Forgemark: ready on {} (bonus scale {}%, maximum tier {})",
				Services.PLATFORM.name(), Config.bonusScale, Config.maxTier);
	}
}
