package com.gabbymaybelle.forgemark.quality;

import com.gabbymaybelle.forgemark.Config;
import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.ForgemarkTags;
import com.gabbymaybelle.forgemark.forge.Metal;
import com.gabbymaybelle.forgemark.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Repairable;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

/**
 * The quality maths. Everything here is worked out on the spot from a {@link Temper} and the
 * item's own base stats (its max damage, Tool and attribute components), never stored. The
 * loaders' hooks ({@code ItemStackMixin}) call in from vanilla's durability, mining speed and
 * attribute code, and the tooltip comes from {@link #tooltip}.
 *
 * <p>Caps, quench included: +15% durability, +10% mining speed, +10% attack damage, +0.5
 * toughness. Masterwork plus a water quench lands exactly on each one.
 */
public final class Quality {
	/** A water quench trades this much durability... */
	public static final int QUENCH_DURABILITY_PERCENT = 3;
	/** ...for this much mining speed and attack damage... */
	public static final int QUENCH_PERCENT = 2;
	/** ...or this much armour toughness. */
	public static final double QUENCH_TOUGHNESS = 0.1;
	/** What a player hits for with nothing in hand; the tooltip adds it to a weapon's own damage. */
	private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
	/** The flat toughness modifier for armour that has no toughness modifier of its own. */
	public static final Identifier TOUGHNESS_ID = Identifier.fromNamespaceAndPath(Forgemark.MOD_ID, "toughness");

	/**
	 * Mods whose own quality bonuses arrive as extra attribute modifiers on the item. Those are
	 * left exactly as they are: Forgemark's percentages apply to the item's own modifiers only,
	 * so the two systems sit side by side and never multiply each other.
	 */
	public static final List<String> OTHER_QUALITY_MODS = List.of("apotheosis", "tiered");

	private static @Nullable Boolean otherQualityModInstalled;

	private Quality() {
	}

	// Settings

	/** The bonus-scale setting as a factor, or 0 when another quality mod is meant to win. */
	public static double scale() {
		if (!Config.bonusesAlongsideOtherQualityMods && otherQualityModInstalled()) {
			return 0.0;
		}
		return Config.bonusScale / 100.0;
	}

	private static boolean otherQualityModInstalled() {
		if (otherQualityModInstalled == null) {
			otherQualityModInstalled = OTHER_QUALITY_MODS.stream().anyMatch(Services.PLATFORM::isModLoaded);
		}
		return otherQualityModInstalled;
	}

	/** The stored tier, held down to the maximum-tier setting. */
	public static Tier effectiveTier(Temper temper) {
		Tier max = Tier.byName(Config.maxTier);
		return temper.tier().min(max == null ? Tier.MASTERWORK : max);
	}

	// Bonuses, quench and scale included

	public static double durabilityPercent(Temper temper) {
		return (effectiveTier(temper).durabilityPercent - (temper.quenched() ? QUENCH_DURABILITY_PERCENT : 0)) * scale();
	}

	public static double miningSpeedPercent(Temper temper) {
		return (effectiveTier(temper).miningSpeedPercent + (temper.quenched() ? QUENCH_PERCENT : 0)) * scale();
	}

	public static double attackDamagePercent(Temper temper) {
		return (effectiveTier(temper).attackDamagePercent + (temper.quenched() ? QUENCH_PERCENT : 0)) * scale();
	}

	public static double toughness(Temper temper) {
		return (effectiveTier(temper).toughness + (temper.quenched() ? QUENCH_TOUGHNESS : 0)) * scale();
	}

	// Applying them to an item's base stats

	public static int maxDamage(int base, Temper temper) {
		return Math.max(1, (int) Math.round(base * (1 + durabilityPercent(temper) / 100)));
	}

	/** Only speeds above 1 are scaled: 1 means "not a tool for this block", and must stay so. */
	public static float destroySpeed(float base, Temper temper) {
		return base > 1.0F ? (float) (base * (1 + miningSpeedPercent(temper) / 100)) : base;
	}

	/**
	 * A tempered item's version of one of its own attribute modifiers. The attack damage bonus
	 * is a share of the damage the tooltip shows (the item's base modifier plus the player's
	 * own 1), so "+8%" means what a player reads. Toughness is flat.
	 */
	public static AttributeModifier adjust(Temper temper, Holder<Attribute> attribute, AttributeModifier modifier) {
		if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE || OTHER_QUALITY_MODS.contains(modifier.id().getNamespace())) {
			return modifier;
		}
		if (is(attribute, Attributes.ATTACK_DAMAGE) && modifier.amount() > 0) {
			double shown = modifier.amount() + (modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID) ? PLAYER_BASE_ATTACK_DAMAGE : 0);
			return new AttributeModifier(modifier.id(), modifier.amount() + shown * attackDamagePercent(temper) / 100, modifier.operation());
		}
		if (is(attribute, Attributes.ARMOR_TOUGHNESS)) {
			return new AttributeModifier(modifier.id(), modifier.amount() + toughness(temper), modifier.operation());
		}
		return modifier;
	}

	/** Wraps vanilla's per-slot attribute walk so every modifier it reports is the tempered one. */
	public static BiConsumer<Holder<Attribute>, AttributeModifier> wrap(ItemStack stack, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		Temper temper = Temper.on(stack);
		if (temper == null) {
			return consumer;
		}
		boolean addToughness = needsToughnessModifier(stack, temper);
		return (attribute, modifier) -> {
			consumer.accept(attribute, adjust(temper, attribute, modifier));
			if (addToughness && is(attribute, Attributes.ARMOR)) {
				consumer.accept(Attributes.ARMOR_TOUGHNESS, toughnessModifier(temper));
			}
		};
	}

	/** The same for the walk the tooltip uses. */
	public static TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> wrap(
			ItemStack stack, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer) {
		Temper temper = Temper.on(stack);
		if (temper == null) {
			return consumer;
		}
		boolean addToughness = needsToughnessModifier(stack, temper);
		return (attribute, modifier, display) -> {
			consumer.accept(attribute, adjust(temper, attribute, modifier), display);
			if (addToughness && is(attribute, Attributes.ARMOR)) {
				consumer.accept(Attributes.ARMOR_TOUGHNESS, toughnessModifier(temper), ItemAttributeModifiers.Display.attributeModifiers());
			}
		};
	}

	/** Armour with no toughness modifier of its own (some modded sets) gets a separate one. */
	private static boolean needsToughnessModifier(DataComponentGetter item, Temper temper) {
		return toughness(temper) != 0 && hasModifier(item, Attributes.ARMOR) && !hasAnyModifier(item, Attributes.ARMOR_TOUGHNESS);
	}

	public static AttributeModifier toughnessModifier(Temper temper) {
		return new AttributeModifier(TOUGHNESS_ID, toughness(temper), AttributeModifier.Operation.ADD_VALUE);
	}

	private static boolean is(Holder<Attribute> attribute, Holder<Attribute> wanted) {
		return attribute.value() == wanted.value();
	}

	/**
	 * A hit on a player with the affect-PvP setting off: the weapon's temper bonus comes back
	 * out. Only the attribute part is known here, so crits and enchantments on top of it stay.
	 */
	public static float pvpDamage(DamageSource source, float damage) {
		if (Config.affectPvp || !(source.getEntity() instanceof Player attacker)) {
			return damage;
		}
		ItemStack weapon = attacker.getMainHandItem();
		Temper temper = Temper.on(weapon);
		if (temper == null) {
			return damage;
		}
		double[] bonus = {0};
		weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
			if (is(attribute, Attributes.ATTACK_DAMAGE)) {
				bonus[0] += adjust(temper, attribute, modifier).amount() - modifier.amount();
			}
		});
		return (float) Math.max(0.0, damage - bonus[0]);
	}

	// What kind of item it is

	/**
	 * Mines things: has a Tool component that works in creative. A sword's Tool only covers
	 * cobwebs and is flagged as not breaking blocks in creative, so it counts as a weapon only.
	 */
	public static boolean isTool(DataComponentGetter item) {
		Tool tool = item.get(DataComponents.TOOL);
		return tool != null && tool.canDestroyBlocksInCreative();
	}

	/**
	 * Hits things: adds attack damage. Vanilla's pickaxes, shovels and hoes do too, and get the
	 * damage bonus along with their mining speed; the tooltip says so.
	 */
	public static boolean isWeapon(DataComponentGetter item) {
		return hasModifier(item, Attributes.ATTACK_DAMAGE);
	}

	public static boolean isArmor(DataComponentGetter item) {
		return hasModifier(item, Attributes.ARMOR);
	}

	/** Whether the item adds to an attribute (a modifier above zero, like armour's +6). */
	private static boolean hasModifier(DataComponentGetter item, Holder<Attribute> attribute) {
		for (ItemAttributeModifiers.Entry entry : item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (is(entry.attribute(), attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE && entry.modifier().amount() > 0) {
				return true;
			}
		}
		return false;
	}

	/** Whether the item carries any modifier for an attribute, even a +0 one (iron armour's toughness). */
	private static boolean hasAnyModifier(DataComponentGetter item, Holder<Attribute> attribute) {
		for (ItemAttributeModifiers.Entry entry : item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (is(entry.attribute(), attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the forge will take an item: it wears out, it has a repair material (the ingot it
	 * asks for), it isn't opted out through the not-temperable tag, and its kind is in the
	 * eligible-items setting. For that setting the kinds don't overlap: a pickaxe hits things
	 * too, but it's a tool, and "weapons" means swords, tridents, maces and the like.
	 */
	public static boolean isTemperable(ItemStack stack) {
		if (stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE) || stack.is(ForgemarkTags.NOT_TEMPERABLE)) {
			return false;
		}
		Repairable repairable = stack.get(DataComponents.REPAIRABLE);
		if (repairable == null || repairable.items().stream().noneMatch(material -> Metal.isHeatable(new ItemStack(material)))) {
			return false;
		}
		if (isTool(stack)) {
			return Config.eligible("tools");
		}
		if (isArmor(stack)) {
			return Config.eligible("armor");
		}
		return isWeapon(stack) && Config.eligible("weapons");
	}

	// Tooltip

	/** The tier line, the bonus line and, on a Masterwork item, the maker's mark. */
	public static List<Component> tooltip(Temper temper, DataComponentGetter item) {
		List<Component> lines = new ArrayList<>(3);
		Tier tier = effectiveTier(temper);
		lines.add(Component.translatable(temper.quenched() ? "forgemark.tooltip.tier.quenched" : "forgemark.tooltip.tier", tier.displayName())
				.withStyle(ChatFormatting.GRAY));
		if (scale() == 0) {
			lines.add(Component.translatable("forgemark.tooltip.off").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			MutableComponent bonuses = Component.empty();
			bonuses.append(Component.translatable("forgemark.tooltip.durability", percent(durabilityPercent(temper))));
			if (isTool(item)) {
				bonuses.append(", ").append(Component.translatable("forgemark.tooltip.mining_speed", percent(miningSpeedPercent(temper))));
			}
			if (isWeapon(item)) {
				bonuses.append(", ").append(Component.translatable("forgemark.tooltip.attack_damage", percent(attackDamagePercent(temper))));
			}
			if (isArmor(item)) {
				bonuses.append(", ").append(Component.translatable("forgemark.tooltip.toughness", String.format(Locale.ROOT, "%+.1f", toughness(temper))));
			}
			lines.add(bonuses.withStyle(ChatFormatting.DARK_GRAY));
		}
		if (tier == Tier.MASTERWORK && Config.makersMark && temper.maker().isPresent()) {
			lines.add(Component.translatable("forgemark.tooltip.maker", temper.maker().get()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}
		return lines;
	}

	private static String percent(double value) {
		String number = value == Math.rint(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
		return (value >= 0 ? "+" : "") + number + "%";
	}
}
