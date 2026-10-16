package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The forge's blocks and items: the hearth, the forging anvil and the smithing hammer. Each
 * registration goes through the platform so NeoForge can hand it over in its registry event;
 * the suppliers are only valid once the game has loaded.
 */
public final class ForgemarkBlocks {
	public static final Supplier<Block> HEARTH = block("hearth", () -> new HearthBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(HearthBlock.LIT) ? 13 : 0)));
	public static final Supplier<Block> FORGING_ANVIL = block("forging_anvil", () -> new ForgingAnvilBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(5.0F, 1200.0F).sound(SoundType.ANVIL).noOcclusion().requiresCorrectToolForDrops()));

	public static final Supplier<Item> HEARTH_ITEM = item("hearth", () -> new HearthBlock.Item(HEARTH.get(), new Item.Properties()));
	public static final Supplier<Item> FORGING_ANVIL_ITEM = item("forging_anvil",
			() -> new ForgingAnvilBlock.Item(FORGING_ANVIL.get(), new Item.Properties()));
	public static final Supplier<Item> SMITHING_HAMMER = item("smithing_hammer", () -> new SmithingHammerItem(
			SmithingHammerItem.properties(new Item.Properties().durability(250))));

	public static final Supplier<BlockEntityType<HearthBlockEntity>> HEARTH_ENTITY = blockEntity("hearth",
			() -> Services.PLATFORM.blockEntityType(HearthBlockEntity::new, HEARTH.get()));
	public static final Supplier<BlockEntityType<ForgingAnvilBlockEntity>> FORGING_ANVIL_ENTITY = blockEntity("forging_anvil",
			() -> Services.PLATFORM.blockEntityType(ForgingAnvilBlockEntity::new, FORGING_ANVIL.get()));

	private ForgemarkBlocks() {
	}

	/** Loads this class, which queues every registration, and files the items under the creative tabs. */
	public static void register() {
		// The tab keys are just ids.
		ResourceKey<CreativeModeTab> functional = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("functional_blocks"));
		ResourceKey<CreativeModeTab> tools = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities"));
		Services.PLATFORM.addToCreativeTab(functional, HEARTH_ITEM);
		Services.PLATFORM.addToCreativeTab(functional, FORGING_ANVIL_ITEM);
		Services.PLATFORM.addToCreativeTab(tools, SMITHING_HAMMER);
	}

	public static ResourceLocation id(String name) {
		return ResourceLocation.fromNamespaceAndPath(Forgemark.MOD_ID, name);
	}



	private static Supplier<Block> block(String name, Supplier<Block> factory) {
		return Services.PLATFORM.register(Registries.BLOCK, id(name), factory);
	}

	private static Supplier<Item> item(String name, Supplier<Item> factory) {
		return Services.PLATFORM.register(Registries.ITEM, id(name), factory);
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, Supplier<BlockEntityType<T>> factory) {
		Supplier<BlockEntityType<?>> registered = Services.PLATFORM.register(Registries.BLOCK_ENTITY_TYPE, id(name), factory::get);
		return () -> (BlockEntityType<T>) registered.get();
	}
}
