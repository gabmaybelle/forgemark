package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.quality.Temper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The water quench: a freshly forged, still-glowing item dipped in a water cauldron sets its
 * temper as quenched (the trade in the tier table), with a hiss and steam, and the cauldron
 * loses a level. Reached from {@code AbstractCauldronBlockMixin}.
 */
public final class Quench {
	private Quench() {
	}

	/** Quenches the item if it's glowing and this is a water cauldron; says whether it did. */
	public static boolean tryQuench(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
		if (!state.is(Blocks.WATER_CAULDRON) || !Glowing.isHot(stack, level.getGameTime())) {
			return false;
		}
		Temper temper = Temper.on(stack);
		if (temper == null) {
			Glowing.clear(stack);
			return false;
		}
		if (!level.isClientSide()) {
			Temper quenched = new Temper(temper.tier(), true, temper.maker());
			Temper.put(stack, quenched);
			if (player instanceof ServerPlayer serverPlayer) {
				TemperedTrigger.get().trigger(serverPlayer, quenched, false);
			}
			Glowing.clear(stack);
			LayeredCauldronBlock.lowerFillLevel(state, level, pos);
			level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.02);
			}
			player.displayClientMessage(Component.translatable("forgemark.quenched", stack.getHoverName()), true);
		}
		return true;
	}
}
