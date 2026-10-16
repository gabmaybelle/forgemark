package com.gabbymaybelle.forgemark.fabric;

import com.gabbymaybelle.forgemark.platform.Platform;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FabricPlatform implements Platform {
	@Override
	public String name() {
		return "Fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, ResourceLocation id, Supplier<T> factory) {
		Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.get(registry.location());
		T value = Registry.register(target, id, factory.get());
		return () -> value;
	}

	@Override
	public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
		ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(item.get()));
	}

	@Override
	public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block) {
		return FabricBlockEntityTypeBuilder.create(factory::apply, block).build();
	}
}
