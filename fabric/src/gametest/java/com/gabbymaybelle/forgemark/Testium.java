package com.gabbymaybelle.forgemark;

import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * A made-up metal from a made-up mod, for the test: an ingot that is in none of Forgemark's
 * tags and no shared tag either, a sword repaired through the mod's own tag (the way Beyond
 * the Dragon's aphelite gear is), and a nugget that must not count as a metal. Registered by
 * the test mod's main entrypoint, so the checks see what a real mod's gear looks like.
 */
public final class Testium implements ModInitializer {
	public static final String MOD_ID = "forgemark_gametest";
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, id("testium_tool_materials"));
	/** Iron's numbers. */
	private static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, REPAIRS);

	public static final Supplier<Item> INGOT = item("testium_ingot", props -> new Item(props));
	public static final Supplier<Item> NUGGET = item("testium_nugget", props -> new Item(props));
	public static final Supplier<Item> SWORD = item("testium_sword", props -> new Item(props.sword(MATERIAL, 3.0F, -2.4F)));

	@Override
	public void onInitialize() {
		// Loading the class is the registration.
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(MOD_ID, name);
	}

	private static Supplier<Item> item(String name, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		return () -> item;
	}
}
