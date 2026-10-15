package com.gabbymaybelle.forgemark.neoforge.compat;

import com.gabbymaybelle.forgemark.compat.ReiPlugin;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * REI finds plugins on NeoForge by this annotation, which only exists in REI's NeoForge jar;
 * the plugin itself is the common {@link ReiPlugin}. Only loaded when REI is installed.
 */
@REIPluginClient
public class ReiNeoForgePlugin extends ReiPlugin {
}
