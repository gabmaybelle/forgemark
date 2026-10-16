package com.gabbymaybelle.forgemark.platform;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The few things that differ between loaders. {@code fabric} and {@code neoforge} each provide
 * one implementation through {@code META-INF/services}; common code reaches it as
 * {@link Services#PLATFORM}.
 */
public interface Platform {
	/** "Fabric" or "NeoForge", for the log. */
	String name();

	/** The game's config folder. */
	Path configDir();

	/** Whether another mod is installed, by its mod id. */
	boolean isModLoaded(String modId);

	/**
	 * Registers something (an item, a block, a block entity type, a data component type...).
	 * Fabric does it at once; NeoForge waits for its registry event, so the result is a supplier
	 * that is only valid once the game has finished loading.
	 */
	<T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, ResourceLocation id, Supplier<T> factory);

	/**
	 * A block entity type for one block. Vanilla's factory interface is private on 1.21.1:
	 * NeoForge opens it, Fabric API has its own builder.
	 */
	<T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block);

	/** Puts an item in one of the vanilla creative tabs. */
	void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item);
}
