package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.compat.JeiPlugin;
import com.gabbymaybelle.forgemark.compat.ReiPlugin;
import java.util.List;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;

/**
 * Opens the Tempering tab in JEI or REI for a screenshot, when the game test runs with one of
 * them installed ({@code -PtestJei}, {@code -PtestRei}). Each method is only called when its
 * viewer is loaded, and this class implements nothing of theirs, so the other viewer's classes
 * are never needed. JEI's runtime comes through {@link JeiRuntimeHook}.
 */
public final class ViewerScreens {
	private ViewerScreens() {
	}

	/** On the client thread: JEI's recipe screen on the Tempering tab. Says whether it could. */
	public static boolean openJei() {
		if (JeiRuntimeHook.runtime == null) {
			return false;
		}
		JeiRuntimeHook.runtime.getRecipesGui().showTypes(List.of(JeiPlugin.TEMPERING));
		return true;
	}

	/** On the client thread: REI's view of the Tempering category. */
	public static boolean openRei() {
		return ViewSearchBuilder.builder().addCategory(ReiPlugin.TEMPERING).open();
	}
}
