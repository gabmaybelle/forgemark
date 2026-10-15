package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.network.SettingsPayload;
import com.gabbymaybelle.forgemark.platform.Services;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The settings, in {@code config/forgemark.properties}: the "Server and pack settings" table of
 * the design doc. A missing file gets the defaults written out with a comment per setting; a
 * file with a typo keeps the typo (it's probably a setting from another version) and the default
 * is used with a warning in the log. The file is only rewritten when a setting is missing, and
 * then through a temp file so a crash mid-write can't leave it half done.
 *
 * <p>On a server these are the server's values. The ones the client needs to show and work out
 * the same numbers (bonus scale, maximum tier, maker's mark) are sent to players when they join
 * ({@link SettingsPayload}) and replace this file's values until they disconnect.
 */
public final class Config {
	public static final String FILE_NAME = Forgemark.MOD_ID + ".properties";
	public static final List<String> TIERS = List.of("rough", "standard", "fine", "superior", "masterwork");

	/** Whether the current bonus scale, maximum tier and maker's mark came from a server. */
	public static boolean fromServer = false;

	/** Multiplies every tier's bonus, in percent: 50 halves them, 0 turns them off. */
	public static int bonusScale = 100;
	/** The highest tier players can reach. */
	public static String maxTier = "masterwork";
	/** Whether weapon bonuses apply against players. */
	public static boolean affectPvp = true;
	/** "Forged by [player]" on Masterwork items. */
	public static boolean makersMark = true;
	/** Whether players may skip the minigame for a higher ingot cost. */
	public static boolean noTimingMode = true;
	/** Ingots per temper in no-timing mode. */
	public static int noTimingCost = 2;
	/** Which kinds of item can be tempered, comma-separated. */
	public static String eligibleItems = "tools,weapons,armor";
	/** Keep Forgemark's bonuses when Tiered or Apotheosis is installed (off: theirs only). */
	public static boolean bonusesAlongsideOtherQualityMods = true;

	private static final Map<String, String> COMMENTS = new LinkedHashMap<>();
	static {
		COMMENTS.put("bonusScale", "Multiplies every tier's bonus, in percent (50 halves them, 0 turns them off).");
		COMMENTS.put("maxTier", "The highest tier players can reach: rough, standard, fine, superior or masterwork.");
		COMMENTS.put("affectPvp", "Whether weapon bonuses apply against other players.");
		COMMENTS.put("makersMark", "Show \"Forged by [player]\" on Masterwork items.");
		COMMENTS.put("noTimingMode", "Let players skip the strike minigame for a higher ingot cost.");
		COMMENTS.put("noTimingCost", "Ingots per temper in no-timing mode.");
		COMMENTS.put("eligibleItems", "Which kinds of item can be tempered, comma-separated: tools, weapons, armor.");
		COMMENTS.put("bonusesAlongsideOtherQualityMods", "Keep Forgemark's bonuses when Tiered or Apotheosis is installed. Off: only theirs apply.");
	}

	private Config() {
	}

	public static Path path() {
		return Services.PLATFORM.configDir().resolve(FILE_NAME);
	}

	/** Takes the server's values on the client; {@link #load()} puts the file's values back. */
	public static void applyFromServer(SettingsPayload settings) {
		bonusScale = settings.bonusScale();
		maxTier = TIERS.contains(settings.maxTier()) ? settings.maxTier() : "masterwork";
		makersMark = settings.makersMark();
		fromServer = true;
	}

	/** Whether a kind of item ("tools", "weapons" or "armor") is in the eligible-items setting. */
	public static boolean eligible(String kind) {
		for (String entry : eligibleItems.split(",")) {
			String name = entry.trim();
			if (name.equals(kind) || (kind.equals("armor") && name.equals("armour"))) {
				return true;
			}
		}
		return false;
	}

	public static void load() {
		fromServer = false;
		Path file = path();
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				props.load(in);
			} catch (IOException e) {
				Forgemark.LOGGER.warn("Couldn't read {}; using the defaults", file, e);
			}
		}
		bonusScale = readInt(props, "bonusScale", 100, 0, 200);
		maxTier = readChoice(props, "maxTier", "masterwork", TIERS);
		affectPvp = readBool(props, "affectPvp", true);
		makersMark = readBool(props, "makersMark", true);
		noTimingMode = readBool(props, "noTimingMode", true);
		noTimingCost = readInt(props, "noTimingCost", 2, 1, 64);
		eligibleItems = props.getProperty("eligibleItems", "tools,weapons,armor").trim().toLowerCase();
		bonusesAlongsideOtherQualityMods = readBool(props, "bonusesAlongsideOtherQualityMods", true);

		boolean missing = false;
		for (String key : COMMENTS.keySet()) {
			if (!props.containsKey(key)) {
				missing = true;
				props.setProperty(key, current(key));
			}
		}
		if (missing) {
			save(file, props);
		}
	}

	private static String current(String key) {
		return switch (key) {
			case "bonusScale" -> Integer.toString(bonusScale);
			case "maxTier" -> maxTier;
			case "affectPvp" -> Boolean.toString(affectPvp);
			case "makersMark" -> Boolean.toString(makersMark);
			case "noTimingMode" -> Boolean.toString(noTimingMode);
			case "noTimingCost" -> Integer.toString(noTimingCost);
			case "eligibleItems" -> eligibleItems;
			case "bonusesAlongsideOtherQualityMods" -> Boolean.toString(bonusesAlongsideOtherQualityMods);
			default -> throw new IllegalArgumentException(key);
		};
	}

	private static int readInt(Properties props, String key, int fallback, int min, int max) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		try {
			int value = Integer.parseInt(raw.trim());
			if (value < min || value > max) {
				Forgemark.LOGGER.warn("{} must be between {} and {} (got {}); using {}", key, min, max, value, fallback);
				return fallback;
			}
			return value;
		} catch (NumberFormatException e) {
			Forgemark.LOGGER.warn("{} should be a whole number (got \"{}\"); using {}", key, raw, fallback);
			return fallback;
		}
	}

	private static boolean readBool(Properties props, String key, boolean fallback) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		String value = raw.trim().toLowerCase();
		if (value.equals("true") || value.equals("false")) {
			return Boolean.parseBoolean(value);
		}
		Forgemark.LOGGER.warn("{} should be true or false (got \"{}\"); using {}", key, raw, fallback);
		return fallback;
	}

	private static String readChoice(Properties props, String key, String fallback, List<String> choices) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		String value = raw.trim().toLowerCase();
		if (choices.contains(value)) {
			return value;
		}
		Forgemark.LOGGER.warn("{} should be one of {} (got \"{}\"); using {}", key, String.join(", ", choices), raw, fallback);
		return fallback;
	}

	/** Writes every setting, commented, keeping any keys we don't know, through a temp file. */
	private static void save(Path file, Properties props) {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(FILE_NAME + ".tmp");
			try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				out.write("# Forgemark settings. Changes apply when the game starts. On a server, the server's values win.\n");
				for (Map.Entry<String, String> entry : COMMENTS.entrySet()) {
					out.write("\n# " + entry.getValue() + "\n");
					out.write(entry.getKey() + "=" + props.getProperty(entry.getKey()) + "\n");
				}
				boolean unknownHeader = false;
				for (String key : props.stringPropertyNames()) {
					if (!COMMENTS.containsKey(key)) {
						if (!unknownHeader) {
							out.write("\n# Settings this version doesn't know (kept as they were)\n");
							unknownHeader = true;
						}
						out.write(key + "=" + props.getProperty(key) + "\n");
					}
				}
			}
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Forgemark.LOGGER.warn("Couldn't write {}", file, e);
		}
	}
}
