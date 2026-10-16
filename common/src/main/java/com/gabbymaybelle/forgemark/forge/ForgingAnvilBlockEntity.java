package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.ForgemarkTags;
import com.gabbymaybelle.forgemark.network.StrikePayload;
import com.gabbymaybelle.forgemark.quality.Temper;
import com.gabbymaybelle.forgemark.quality.Tier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The anvil's state: the item being tempered, the ingot with its heat, the strikes so far, and
 * the sweep the client draws (when it started and where the target zone is). Everything is
 * saved and sent to clients, so the bar needs no packet of its own. The server decides every
 * strike in {@link #strike}; the client only reports where it saw the marker.
 */
public class ForgingAnvilBlockEntity extends BlockEntity {
	private ItemStack item = ItemStack.EMPTY;
	private ItemStack ingot = ItemStack.EMPTY;
	private final List<Integer> strikes = new ArrayList<>();
	private long sweepStart;
	private float zoneCenter = 0.5F;
	private boolean noTiming;

	public ForgingAnvilBlockEntity(BlockPos pos, BlockState state) {
		super(ForgemarkBlocks.FORGING_ANVIL_ENTITY.get(), pos, state);
	}

	public ItemStack getItem() {
		return item;
	}

	public ItemStack getIngot() {
		return ingot;
	}

	public List<Integer> getStrikes() {
		return Collections.unmodifiableList(strikes);
	}

	public long getSweepStart() {
		return sweepStart;
	}

	public float getZoneCenter() {
		return zoneCenter;
	}

	/** Whether there's something to strike: an item and an ingot (hot or not). */
	public boolean canStrike() {
		return !item.isEmpty() && !ingot.isEmpty();
	}

	/** The ingot's heat now, 0 to 1. */
	public float ingotHeat(long gameTime) {
		return Heat.now(ingot, gameTime);
	}

	/** The heat one strike uses up. */
	public float strikeCost() {
		return ingot.isEmpty() ? 1.0F : 1.0F / Metal.strikesPerHeat(ingot);
	}

	public boolean isHotEnough(long gameTime) {
		return ingotHeat(gameTime) >= strikeCost() - 0.001F;
	}

	public void placeItem(ItemStack stack) {
		item = stack;
		strikes.clear();
		noTiming = false;
		newSweep();
		sync();
	}

	public void placeIngot(ItemStack stack) {
		ingot = stack;
		newSweep();
		sync();
	}

	/** Taking the item off the anvil drops any strikes made so far. */
	public ItemStack takeItem() {
		ItemStack out = item;
		item = ItemStack.EMPTY;
		strikes.clear();
		noTiming = false;
		sync();
		return out;
	}

	/** The ingot can come off to be reheated; the strikes stay. */
	public ItemStack takeIngot() {
		ItemStack out = ingot;
		ingot = ItemStack.EMPTY;
		sync();
		return out;
	}

	/** For tests: moves the sweep so the server's own marker is wherever they need it. */
	public void setSweepStart(long gameTime) {
		sweepStart = gameTime;
	}

	private void newSweep() {
		Level level = getLevel();
		if (level != null) {
			sweepStart = level.getGameTime();
			zoneCenter = 0.2F + level.getRandom().nextFloat() * 0.6F;
		}
	}

	/** A press from a player: validated, checked against the server's own clock, scored. */
	public static void handleStrike(ServerPlayer player, StrikePayload payload) {
		if (!(player.level().getBlockEntity(payload.pos()) instanceof ForgingAnvilBlockEntity anvil)) {
			return;
		}
		double reach = player.blockInteractionRange() + 1.0;
		if (player.position().distanceToSqr(Vec3.atCenterOf(payload.pos())) > reach * reach) {
			return;
		}
		anvil.strike(player, Mth.clamp(payload.markerPosition(), 0.0F, 1.0F));
	}

	/**
	 * One strike. The server works out where its own marker is; the client's report is used
	 * when it's within the lag window, otherwise the server's position stands. Sneaking in
	 * no-timing mode makes every strike perfect for a higher ingot cost at the end.
	 */
	public void strike(ServerPlayer player, float reportedPosition) {
		Level level = getLevel();
		if (level == null || !canStrike() || strikes.size() >= Forging.STRIKES) {
			return;
		}
		ItemStack hammer = player.getMainHandItem();
		if (!hammer.is(ForgemarkTags.HAMMERS)) {
			return;
		}
		long now = level.getGameTime();
		float heat = ingotHeat(now);
		float cost = strikeCost();
		if (heat < cost - 0.001F) {
			player.displayClientMessage(Component.translatable("forgemark.anvil.reheat"), true);
			level.playSound(null, getBlockPos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 0.5F);
			return;
		}
		int score;
		if (player.isShiftKeyDown() && Config.noTimingMode) {
			if (countMatchingIngots(player.getInventory()) < Config.noTimingCost - 1) {
				player.displayClientMessage(Component.translatable("forgemark.anvil.no_timing_ingots", Config.noTimingCost), true);
				return;
			}
			noTiming = true;
			score = Forging.PERFECT;
		} else {
			float serverPosition = Forging.markerPosition(now - sweepStart);
			float position = Forging.plausible(reportedPosition, serverPosition) ? reportedPosition : serverPosition;
			score = Forging.score(position, zoneCenter);
		}
		strikes.add(score);
		Heat.put(ingot, heat - cost, now);
		hammer.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		level.playSound(null, getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.8F + score * 0.2F);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CRIT, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.1, getBlockPos().getZ() + 0.5, 2 + score * 2, 0.15, 0.05, 0.15, 0.1);
		}
		player.displayClientMessage(Component.translatable("forgemark.bar.hit." + score), true);
		if (strikes.size() >= Forging.STRIKES) {
			finish(player, now);
		} else {
			newSweep();
		}
		sync();
	}

	/** After the fifth strike: the points pick the tier, the better of old and new stays, the ingot is spent. */
	private void finish(ServerPlayer player, long now) {
		Level level = getLevel();
		int points = strikes.stream().mapToInt(Integer::intValue).sum();
		Tier max = Tier.byName(Config.maxTier);
		Tier tier = Forging.tierFor(points).min(max == null ? Tier.MASTERWORK : max);
		Temper old = Temper.on(item);
		if (old == null || !old.tier().isBetterThan(tier)) {
			boolean keepQuench = old != null && old.tier() == tier && old.quenched();
			Temper.put(item, Temper.of(tier, keepQuench, player.getName().getString()));
			Glowing.put(item, now);
			// A Rough temper can pull the maximum under a worn item's damage; it never breaks it.
			if (item.isDamageableItem() && item.getDamageValue() >= item.getMaxDamage()) {
				item.setDamageValue(item.getMaxDamage() - 1);
			}
			player.displayClientMessage(Component.translatable("forgemark.anvil.forged", item.getHoverName(), tier.displayName()), true);
		} else {
			player.displayClientMessage(Component.translatable("forgemark.anvil.kept", item.getHoverName(), old.tier().displayName()), true);
		}
		// The advancement trail hears about the tier the strikes earned, whether or not the item kept a better one.
		TemperedTrigger.get().trigger(player, Temper.of(tier, false, null), noTiming);
		ingot = ItemStack.EMPTY;
		if (noTiming) {
			takeMatchingIngots(player.getInventory(), Config.noTimingCost - 1);
		}
		strikes.clear();
		noTiming = false;
		if (level != null) {
			level.playSound(null, getBlockPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.LAVA, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.1, getBlockPos().getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.0);
			}
		}
	}

	/** Ingots in the inventory that would repair the item on the anvil: the extra cost of no-timing mode. */
	public int countMatchingIngots(Inventory inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (Metal.isHeatable(stack) && Metal.repairs(item, stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private void takeMatchingIngots(Inventory inventory, int count) {
		for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (Metal.isHeatable(stack) && Metal.repairs(item, stack)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
	}

	private void sync() {
		setChanged();
		Level level = getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("item", item.saveOptional(registries));
		tag.put("ingot", ingot.saveOptional(registries));
		tag.putIntArray("strikes", strikes.stream().mapToInt(Integer::intValue).toArray());
		tag.putLong("sweep_start", sweepStart);
		tag.putFloat("zone_center", zoneCenter);
		tag.putBoolean("no_timing", noTiming);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		item = ItemStack.parseOptional(registries, tag.getCompound("item"));
		ingot = ItemStack.parseOptional(registries, tag.getCompound("ingot"));
		strikes.clear();
		for (int score : tag.getIntArray("strikes")) {
			strikes.add(score);
		}
		sweepStart = tag.getLong("sweep_start");
		zoneCenter = tag.contains("zone_center") ? tag.getFloat("zone_center") : 0.5F;
		noTiming = tag.getBoolean("no_timing");
	}

	/** Drops the item and the ingot when the anvil is broken; ForgingAnvilBlock.onRemove calls it (1.21.1 has no preRemoveSideEffects). */
	public void dropContents(BlockPos pos) {
		if (level != null) {
			if (!item.isEmpty()) {
				Block.popResource(level, pos, item);
			}
			if (!ingot.isEmpty()) {
				Block.popResource(level, pos, ingot);
			}
			item = ItemStack.EMPTY;
			ingot = ItemStack.EMPTY;
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
