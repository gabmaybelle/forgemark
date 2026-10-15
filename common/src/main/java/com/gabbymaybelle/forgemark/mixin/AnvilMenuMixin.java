package com.gabbymaybelle.forgemark.mixin;

import com.gabbymaybelle.forgemark.quality.Temper;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla anvil keeps the better temper when two tempered items are combined. Repairing
 * already keeps the left item's temper, since the result is a copy of it; this only steps in
 * when the right item is a better-tempered copy. Fabric has no anvil event, and NeoForge's
 * fires inside this same method, so one hook at its end serves both loaders.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Inject(method = "createResult", at = @At("RETURN"))
	private void forgemark$keepBetterTemper(CallbackInfo ci) {
		AnvilMenu self = (AnvilMenu) (Object) this;
		ItemStack result = self.getSlot(2).getItem();
		if (result.isEmpty()) {
			return;
		}
		Temper current = Temper.on(result);
		Temper best = Temper.better(current, Temper.on(self.getSlot(1).getItem()));
		if (best != null && best != current) {
			result.set(Temper.type(), best);
		}
	}
}
