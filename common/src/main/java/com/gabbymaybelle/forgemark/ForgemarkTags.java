package com.gabbymaybelle.forgemark;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags pack makers can fill. */
public final class ForgemarkTags {
	/** Items that can never be tempered, for special gear such as Botania's relics. */
	public static final TagKey<Item> NOT_TEMPERABLE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "not_temperable"));

	private ForgemarkTags() {
	}
}
