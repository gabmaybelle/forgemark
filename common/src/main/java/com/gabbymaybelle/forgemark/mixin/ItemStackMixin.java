package com.gabbymaybelle.forgemark.mixin;

import com.gabbymaybelle.forgemark.quality.Quality;
import com.gabbymaybelle.forgemark.quality.Temper;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a tempered item's stats come from. Neither loader has an event for an item's maximum
 * damage or mining speed, and Fabric has none for attribute modifiers, so all three go through
 * these small hooks on the {@code ItemStack} methods every caller uses: the durability bar,
 * the anvil, block breaking, equipping and the tooltip. The methods have the same shape on
 * Fabric and NeoForge (NeoForge's {@code forEachModifier} still hands each modifier to the
 * same consumer), so one mixin serves both.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
	private void forgemark$temperedMaxDamage(CallbackInfoReturnable<Integer> cir) {
		Temper temper = Temper.on((ItemStack) (Object) this);
		if (temper != null && cir.getReturnValueI() > 0) {
			cir.setReturnValue(Quality.maxDamage(cir.getReturnValueI(), temper));
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void forgemark$temperedDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		ItemStack self = (ItemStack) (Object) this;
		Temper temper = Temper.on(self);
		if (temper != null && Quality.isTool(self)) {
			cir.setReturnValue(Quality.destroySpeed(cir.getReturnValueF(), temper));
		}
	}

	@ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), argsOnly = true)
	private BiConsumer<Holder<Attribute>, AttributeModifier> forgemark$temperedModifiers(BiConsumer<Holder<Attribute>, AttributeModifier> consumer, EquipmentSlot slot) {
		return Quality.wrap((ItemStack) (Object) this, consumer);
	}

	@ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("HEAD"), argsOnly = true)
	private TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> forgemark$temperedModifiersForTooltip(
			TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer, EquipmentSlotGroup slot) {
		return Quality.wrap((ItemStack) (Object) this, consumer);
	}
}
