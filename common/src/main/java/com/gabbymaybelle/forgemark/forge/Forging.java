package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.quality.Tier;

/**
 * The strike minigame's numbers, shared by the server (which decides every strike) and the
 * client (which only draws the bar). A marker sweeps from one end of the bar to the other and
 * back, {@link #SWEEP_TICKS} for a full trip; each strike has a target zone: inside the wide
 * zone is a good strike, inside the narrow zone in its middle a perfect one. Five strikes make
 * a temper, and the points they score pick the tier.
 */
public final class Forging {
	public static final int STRIKES = 5;
	/** One trip along the bar and back. */
	public static final int SWEEP_TICKS = 40;
	/** Half-widths of the target zones, as a share of the bar. */
	public static final float WIDE_ZONE = 0.12F;
	public static final float NARROW_ZONE = 0.04F;
	/** How far the client's reported marker may sit from the server's own before it's distrusted: about 150 ms of lag. */
	public static final float LAG_TOLERANCE = 0.15F;
	public static final int PERFECT = 2;
	public static final int GOOD = 1;
	public static final int MISS = 0;

	private Forging() {
	}

	/** Where the marker is, 0 to 1, after this many ticks of sweeping. */
	public static float markerPosition(double ticksSinceStart) {
		double t = ((ticksSinceStart % SWEEP_TICKS) + SWEEP_TICKS) % SWEEP_TICKS / SWEEP_TICKS;
		return (float) (t < 0.5 ? t * 2 : 2 - t * 2);
	}

	/** Points for a strike at {@code position} when the zone is centred on {@code zoneCenter}. */
	public static int score(float position, float zoneCenter) {
		float distance = Math.abs(position - zoneCenter);
		if (distance <= NARROW_ZONE) {
			return PERFECT;
		}
		return distance <= WIDE_ZONE ? GOOD : MISS;
	}

	/** The tier five strikes' points earn: the design doc's "How it's reached" column. */
	public static Tier tierFor(int points) {
		if (points >= STRIKES * PERFECT) {
			return Tier.MASTERWORK;
		}
		if (points >= 7) {
			return Tier.SUPERIOR;
		}
		if (points >= 5) {
			return Tier.FINE;
		}
		return points >= 3 ? Tier.STANDARD : Tier.ROUGH;
	}

	/** Whether to trust the client's reported marker position over the server's own. */
	public static boolean plausible(float reported, float serverPosition) {
		return Math.abs(reported - serverPosition) <= LAG_TOLERANCE;
	}
}
