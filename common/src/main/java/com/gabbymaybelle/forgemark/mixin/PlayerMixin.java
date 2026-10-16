package com.gabbymaybelle.forgemark.mixin;

import com.gabbymaybelle.forgemark.quality.Quality;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The affect-PvP setting: with it off, a tempered weapon's damage bonus is taken back out of
 * hits on players. The bonus lives in an attribute modifier, so this is the one place a hit
 * knows who it's landing on. Same code on both loaders.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	// 1.21.1's Player.hurt(DamageSource, float); 26.x calls it hurtServer and passes the level too.
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float forgemark$noTemperBonusInPvp(float damage, DamageSource source, float original) {
		return Quality.pvpDamage(source, damage);
	}
}
