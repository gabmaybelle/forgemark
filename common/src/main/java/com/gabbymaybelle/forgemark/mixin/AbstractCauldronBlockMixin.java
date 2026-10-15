package com.gabbymaybelle.forgemark.mixin;

import com.gabbymaybelle.forgemark.forge.Quench;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The water quench happens in a vanilla cauldron. Vanilla's cauldron interactions are keyed by
 * item, and a tempered item can be any item, so this looks at the held item before the
 * cauldron's own table does. Same code on both loaders.
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class AbstractCauldronBlockMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void forgemark$quench(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (Quench.tryQuench(stack, state, level, pos, player)) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
