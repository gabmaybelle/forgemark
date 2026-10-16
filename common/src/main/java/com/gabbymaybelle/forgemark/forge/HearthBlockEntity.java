package com.gabbymaybelle.forgemark.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds one ingot and its heat. While the hearth is lit the heat climbs to 1 over the metal's
 * heat time; while it's out the heat falls. Clients get the heat for the glow whenever it
 * crosses a tenth, not every tick.
 */
public class HearthBlockEntity extends BlockEntity {
	private ItemStack ingot = ItemStack.EMPTY;
	private float heat;
	private int lastSyncedTenth = -1;

	public HearthBlockEntity(BlockPos pos, BlockState state) {
		super(ForgemarkBlocks.HEARTH_ENTITY.get(), pos, state);
	}

	public ItemStack getIngot() {
		return ingot;
	}

	public float getHeat() {
		return heat;
	}

	public boolean isHot() {
		return heat >= 1.0F;
	}

	/** Puts an ingot in, carrying over any heat it still had. */
	public void insert(ItemStack stack) {
		Level level = getLevel();
		heat = level == null ? 0.0F : Heat.now(stack, level.getGameTime());
		ingot = stack.copyWithCount(1);
		Heat.put(ingot, 0.0F, 0);
		sync();
	}

	/** Takes the ingot out with its heat as a component, ready for the anvil. */
	public ItemStack takeOut() {
		ItemStack out = ingot;
		Level level = getLevel();
		if (level != null) {
			Heat.put(out, heat, level.getGameTime());
		}
		ingot = ItemStack.EMPTY;
		heat = 0.0F;
		sync();
		return out;
	}

	public void serverTick() {
		Level level = getLevel();
		if (level == null) {
			return;
		}
		boolean lit = getBlockState().getValue(HearthBlock.LIT);
		if (ingot.isEmpty()) {
			return;
		}
		float before = heat;
		if (lit) {
			heat = Math.min(1.0F, heat + 1.0F / Metal.heatTicks(ingot));
			if (heat > 0.9995F) {
				heat = 1.0F; // the steps don't add up to exactly 1 in floating point
			}
		} else {
			heat = Math.max(0.0F, heat - 1.0F / Heat.COOLING_TICKS);
		}
		if (heat != before) {
			setChanged();
			int tenth = (int) (heat * 10);
			if (tenth != lastSyncedTenth) {
				sync();
			}
		}
	}

	private void sync() {
		setChanged();
		lastSyncedTenth = (int) (heat * 10);
		Level level = getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("ingot", ItemStack.OPTIONAL_CODEC, ingot);
		output.putFloat("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ingot = input.read("ingot", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		heat = input.getFloatOr("heat", 0.0F);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !ingot.isEmpty()) {
			Block.popResource(level, pos, takeOut());
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
