package com.gabbymaybelle.forgemark.forge;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The smithing hammer: strikes the forging anvil and wears with use like any tool. It hits
 * like an iron axe but slowly, so it's a weapon in a pinch, and being repaired with iron it
 * can be tempered itself.
 */
public class SmithingHammerItem extends Item {
	public SmithingHammerItem(Properties properties) {
		super(properties);
	}

	public static Properties properties(Properties properties) {
		return properties.attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable("item.forgemark.smithing_hammer.desc").withStyle(ChatFormatting.GRAY));
	}

	/** Mended with iron at an anvil, like iron tools (26.x says this with the repairable component). */
	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairWith) {
		return repairWith.is(Items.IRON_INGOT);
	}
}
