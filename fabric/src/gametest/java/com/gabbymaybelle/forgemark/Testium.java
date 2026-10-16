package com.gabbymaybelle.forgemark;

import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * A made-up metal from a made-up mod, for the test: an ingot that is in none of Forgemark's
 * tags and no shared tag either, a sword repaired through the mod's own tag (the way Beyond
 * the Dragon's aphelite gear is), and a nugget that must not count as a metal. Registered by
 * the test mod's main entrypoint, so the checks see what a real mod's gear looks like.
 */
public final class Testium implements ModInitializer {
	public static final String MOD_ID = "forgemark_gametest";
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, id("testium_tool_materials"));
	/** Iron's numbers, as a 1.21.1 tool tier repaired through the mod's own tag. */
	private static final Tier MATERIAL = new Tier() {
		@Override
		public int getUses() {
			return 250;
		}

		@Override
		public float getSpeed() {
			return 6.0F;
		}

		@Override
		public float getAttackDamageBonus() {
			return 2.0F;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_IRON_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 14;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(REPAIRS);
		}
	};

	public static final Supplier<Item> INGOT = item("testium_ingot", () -> new Item(new Item.Properties()));
	public static final Supplier<Item> NUGGET = item("testium_nugget", () -> new Item(new Item.Properties()));
	public static final Supplier<Item> SWORD = item("testium_sword",
			() -> new SwordItem(MATERIAL, new Item.Properties().attributes(SwordItem.createAttributes(MATERIAL, 3, -2.4F))));

	@Override
	public void onInitialize() {
		// Loading the class is the registration.
	}

	private static ResourceLocation id(String name) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
	}

	private static Supplier<Item> item(String name, Supplier<Item> factory) {
		Item item = Registry.register(BuiltInRegistries.ITEM, id(name), factory.get());
		return () -> item;
	}
}
