package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.platform.Services;
import com.gabbymaybelle.forgemark.quality.Quality;
import com.gabbymaybelle.forgemark.quality.Temper;
import com.gabbymaybelle.forgemark.quality.Tier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mod in a real game. Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land
 * in fabric/build/run/clientGameTest/screenshots. Each feature adds its own checks here as it
 * lands, and each check is proven by running it once with the feature switched off.
 *
 * <p>The expected numbers are written out by hand from the design doc's tier table, never
 * read back from the code under test.
 */
public class ForgemarkClientTest implements FabricClientGameTest {
	/** Where the tempered pickaxe sits in the player's inventory across the save and reopen. */
	private static final int SAVED_SLOT = 9;
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		settingsFile();
		TestWorldSave save;
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			boolean loaded = world.getServer().computeOnServer(server -> FabricLoader.getInstance().isModLoaded(Forgemark.MOD_ID));
			check(loaded, "the mod is loaded in a new world");
			check(Services.PLATFORM.name().equals("Fabric"), "the Fabric platform is the one in use");
			check(Config.fromServer, "the server's settings arrive on the client when joining");
			world.getServer().runOnServer(server -> {
				component();
				tierMaths();
				quenchAndSettings();
				tooltips(server);
				eligibility();
				anvil(server);
				grindstone(server);
				netheriteUpgrade(server);
				command(server);
				ItemStack keepsake = new ItemStack(Items.DIAMOND_PICKAXE);
				Temper.put(keepsake, Temper.of(Tier.MASTERWORK, true, "Gabby"));
				player(server).getInventory().setItem(SAVED_SLOT, keepsake);
			});
			ctx.takeScreenshot("forgemark_in_world");
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				ItemStack keepsake = player(server).getInventory().getItem(SAVED_SLOT);
				Temper temper = Temper.on(keepsake);
				check(keepsake.is(Items.DIAMOND_PICKAXE) && temper != null && temper.tier() == Tier.MASTERWORK && temper.quenched()
						&& temper.maker().equals(Optional.of("Gabby")), "a temper survives saving and reopening the world");
				check(keepsake.getMaxDamage() == 1748, "the reopened pickaxe still has its +12% durability (1748)");
			});
		}
		if (!failures.isEmpty()) {
			throw new AssertionError(failures.size() + " check(s) failed:\n - " + String.join("\n - ", failures));
		}
	}

	/** The settings file is written with the defaults, survives a typo, and keeps unknown keys. */
	private void settingsFile() {
		Path file = Config.path();
		check(Files.exists(file), "the settings file is written on first start");
		check(Config.bonusScale == 100 && Config.maxTier.equals("masterwork") && Config.noTimingCost == 2,
				"the settings default to bonus scale 100, maximum tier masterwork, no-timing cost 2");
		try {
			String original = Files.readString(file, StandardCharsets.UTF_8);
			Files.writeString(file, original.replace("bonusScale=100", "bonusScale=lots").replace("maxTier=masterwork", "maxTier=legendary")
					+ "someFutureSetting=yes\n", StandardCharsets.UTF_8);
			Config.load();
			check(Config.bonusScale == 100, "a number with a typo falls back to its default");
			check(Config.maxTier.equals("masterwork"), "an unknown tier name falls back to masterwork");
			String rewritten = Files.readString(file, StandardCharsets.UTF_8);
			check(rewritten.contains("bonusScale=lots"), "a typo is left in the file for the player to fix (not overwritten)");
			check(rewritten.contains("someFutureSetting=yes"), "a setting this version doesn't know is kept");
			Files.writeString(file, original, StandardCharsets.UTF_8);
			Config.load();
		} catch (IOException e) {
			failures.add("couldn't edit the settings file: " + e);
		}
	}

	private void component() {
		check(Temper.ID.equals(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(Temper.type())), "the forgemark:temper data component is registered");
		check(!Temper.type().isTransient(), "the temper component is saved with the item");
	}

	/** Every tier on vanilla gear: the numbers from the design doc's table. */
	private void tierMaths() {
		BlockState stone = Blocks.STONE.defaultBlockState();
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		check(pickaxe.getMaxDamage() == 1561 && pickaxe.getDestroySpeed(stone) == 8.0F, "an untempered diamond pickaxe is untouched (1561, speed 8)");
		int[] durability = {1483, 1561, 1639, 1717, 1795};
		float[] speed = {8.0F, 8.0F, 8.24F, 8.48F, 8.64F};
		// A diamond sword's own modifier is +6 in 26.3 (the tooltip adds the player's 1 and shows 7).
		double[] swordDamage = {6.0, 6.0, 6.21, 6.42, 6.56};
		double[] chestplateToughness = {2.0, 2.0, 2.1, 2.2, 2.4};
		for (Tier tier : Tier.values()) {
			Temper temper = Temper.of(tier, false, null);
			Temper.put(pickaxe, temper);
			check(pickaxe.getMaxDamage() == durability[tier.ordinal()],
					tier + " diamond pickaxe durability is " + durability[tier.ordinal()] + " (got " + pickaxe.getMaxDamage() + ")");
			check(near(pickaxe.getDestroySpeed(stone), speed[tier.ordinal()]),
					tier + " diamond pickaxe mining speed is " + speed[tier.ordinal()] + " (got " + pickaxe.getDestroySpeed(stone) + ")");
			ItemStack sword = tempered(Items.DIAMOND_SWORD, temper);
			check(near(modifierTotal(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE), swordDamage[tier.ordinal()]),
					tier + " diamond sword attack damage modifier is " + swordDamage[tier.ordinal()] + " (got " + modifierTotal(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE) + ")");
			ItemStack chestplate = tempered(Items.DIAMOND_CHESTPLATE, temper);
			check(near(modifierTotal(chestplate, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS), chestplateToughness[tier.ordinal()]),
					tier + " diamond chestplate toughness is " + chestplateToughness[tier.ordinal()] + " (got " + modifierTotal(chestplate, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS) + ")");
		}
		Temper.put(pickaxe, Temper.of(Tier.FINE, false, null));
		check(near(modifierTotal(pickaxe, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE), 4.15), "a pickaxe hits things too: Fine takes its damage modifier from 4 to 4.15");
		ItemStack sword = tempered(Items.DIAMOND_SWORD, Temper.of(Tier.MASTERWORK, true, null));
		check(sword.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) == 15.0F, "a sword is a weapon, not a tool: its cobweb speed stays 15");
		check(near(modifierTotal(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED), -2.4), "a sword's attack speed is untouched");
		check(near(modifierTotal(sword, EquipmentSlot.OFFHAND, Attributes.ATTACK_DAMAGE), 0), "a sword still adds no damage in the off hand");
		ItemStack ironChestplate = tempered(Items.IRON_CHESTPLATE, Temper.of(Tier.FINE, false, null));
		check(near(modifierTotal(ironChestplate, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS), 0.1), "iron armour, which has no toughness, gets the flat +0.1");
		check(near(modifierTotal(ironChestplate, EquipmentSlot.CHEST, Attributes.ARMOR), 6), "armour points are untouched");
		ItemStack bare = tempered(Items.LEATHER_HELMET, Temper.of(Tier.MASTERWORK, false, null));
		bare.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(Quality.TOUGHNESS_ID.withSuffix("_test"), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
				.build());
		check(near(modifierTotal(bare, EquipmentSlot.HEAD, Attributes.ARMOR_TOUGHNESS), 0.4), "armour with no toughness modifier at all gets one added");
		check(near(modifierTotal(bare, EquipmentSlot.HEAD, Attributes.ARMOR_TOUGHNESS, EquipmentSlotGroup.HEAD), 0.4), "...in the tooltip's walk too");
	}

	/** The water quench and the bonus-scale and maximum-tier settings. */
	private void quenchAndSettings() {
		BlockState stone = Blocks.STONE.defaultBlockState();
		ItemStack pickaxe = tempered(Items.DIAMOND_PICKAXE, Temper.of(Tier.MASTERWORK, true, null));
		check(pickaxe.getMaxDamage() == 1748, "a quenched Masterwork pickaxe trades down to +12% durability (1748)");
		check(near(pickaxe.getDestroySpeed(stone), 8.8F), "...and lands on the +10% mining speed cap (8.8)");
		ItemStack sword = tempered(Items.DIAMOND_SWORD, Temper.of(Tier.MASTERWORK, true, null));
		check(near(modifierTotal(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE), 6.7), "a quenched Masterwork sword lands on the +10% damage cap (6 + 10% of 7 = 6.7)");
		ItemStack chestplate = tempered(Items.DIAMOND_CHESTPLATE, Temper.of(Tier.MASTERWORK, true, null));
		check(near(modifierTotal(chestplate, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS), 2.5), "quenched Masterwork armour lands on the +0.5 toughness cap (2.5)");
		Temper.put(pickaxe, Temper.of(Tier.ROUGH, true, null));
		check(pickaxe.getMaxDamage() == 1436, "a quenched Rough pickaxe is at -8% durability (1436)");
		check(near(pickaxe.getDestroySpeed(stone), 8.16F), "...with the quench's +2% speed (8.16)");

		Temper.put(pickaxe, Temper.of(Tier.MASTERWORK, false, null));
		Config.bonusScale = 50;
		check(pickaxe.getMaxDamage() == 1678, "bonus scale 50 halves a Masterwork pickaxe's durability bonus (1678)");
		Config.bonusScale = 0;
		check(pickaxe.getMaxDamage() == 1561 && pickaxe.getDestroySpeed(stone) == 8.0F, "bonus scale 0 turns the bonuses off");
		Config.bonusScale = 100;
		Config.maxTier = "fine";
		check(pickaxe.getMaxDamage() == 1639, "maximum tier fine holds a Masterwork pickaxe down to Fine (1639)");
		Config.maxTier = "masterwork";
		check(pickaxe.getMaxDamage() == 1795, "...and back to Masterwork when the setting is (1795)");
	}

	private void tooltips(MinecraftServer server) {
		Item.TooltipContext context = Item.TooltipContext.of(server.registryAccess());
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		String name = pickaxe.getTooltipLines(context, null, TooltipFlag.NORMAL).getFirst().getString();
		Temper.put(pickaxe, Temper.of(Tier.FINE, false, null));
		List<String> lines = pickaxe.getTooltipLines(context, null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.getFirst().equals(name), "the item's name is untouched");
		check(lines.size() > 2 && lines.get(1).equals("Fine temper"), "the tier has its own line right after the name (got " + lines + ")");
		check(lines.get(2).equals("+5% durability, +3% mining speed, +3% attack damage"), "the pickaxe's bonuses are spelled out (got " + lines.get(2) + ")");
		check(lines.stream().noneMatch(l -> l.contains("Forged by")), "no maker's mark below Masterwork");
		ItemStack sword = tempered(Items.DIAMOND_SWORD, Temper.of(Tier.MASTERWORK, true, "Gabby"));
		lines = sword.getTooltipLines(context, null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.get(1).equals("Masterwork temper, water-quenched"), "a quenched Masterwork sword says so (got " + lines.get(1) + ")");
		check(lines.get(2).equals("+12% durability, +10% attack damage"), "the sword's bonuses leave out mining speed (got " + lines.get(2) + ")");
		check(lines.get(3).equals("Forged by Gabby"), "a Masterwork item carries the maker's mark (got " + lines.get(3) + ")");
		lines = sword.getTooltipLines(context, player(server), TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.stream().anyMatch(l -> l.contains("7.7 Attack Damage")), "vanilla's attack damage line shows the tempered number, 7.7 (got " + lines + ")");
		Config.makersMark = false;
		lines = sword.getTooltipLines(context, null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.stream().noneMatch(l -> l.contains("Forged by")), "the maker's mark setting hides it");
		Config.makersMark = true;
		ItemStack chestplate = tempered(Items.IRON_CHESTPLATE, Temper.of(Tier.SUPERIOR, false, null));
		lines = chestplate.getTooltipLines(context, null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.get(2).equals("+10% durability, +0.2 toughness"), "armour's bonuses are durability and toughness (got " + lines.get(2) + ")");
		Config.bonusScale = 0;
		lines = chestplate.getTooltipLines(context, null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
		check(lines.get(2).equals("Tempering bonuses are turned off here"), "bonus scale 0 says the bonuses are off (got " + lines.get(2) + ")");
		Config.bonusScale = 100;
	}

	private void eligibility() {
		check(Quality.isTemperable(new ItemStack(Items.DIAMOND_PICKAXE)) && Quality.isTemperable(new ItemStack(Items.IRON_SWORD))
				&& Quality.isTemperable(new ItemStack(Items.GOLDEN_BOOTS)) && Quality.isTemperable(new ItemStack(Items.NETHERITE_AXE)),
				"tools, weapons and armour can be tempered");
		check(!Quality.isTemperable(new ItemStack(Items.STICK)) && !Quality.isTemperable(new ItemStack(Items.ENCHANTED_BOOK)), "things that don't wear out can't");
		check(!Quality.isTemperable(new ItemStack(Items.ELYTRA)) && !Quality.isTemperable(new ItemStack(Items.BOW)), "elytra and bows wait for a later version");
		Config.eligibleItems = "weapons";
		check(!Quality.isTemperable(new ItemStack(Items.DIAMOND_PICKAXE)) && Quality.isTemperable(new ItemStack(Items.IRON_SWORD)),
				"the eligible-items setting narrows it down");
		Config.eligibleItems = "tools,weapons,armor";
	}

	/** The vanilla anvil keeps the better temper when repairing or combining. */
	private void anvil(MinecraftServer server) {
		AnvilMenu anvil = new AnvilMenu(1, player(server).getInventory(), ContainerLevelAccess.NULL);
		ItemStack worn = tempered(Items.DIAMOND_SWORD, Temper.of(Tier.FINE, false, null));
		worn.setDamageValue(300);
		anvil.getSlot(0).set(worn);
		anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
		Temper repaired = Temper.on(anvil.getSlot(2).getItem());
		check(anvil.getSlot(2).getItem().getDamageValue() < 300 && repaired != null && repaired.tier() == Tier.FINE, "repairing with a diamond keeps the temper");
		anvil.getSlot(1).set(tempered(Items.DIAMOND_SWORD, Temper.of(Tier.MASTERWORK, false, "Gabby")));
		Temper combined = Temper.on(anvil.getSlot(2).getItem());
		check(combined != null && combined.tier() == Tier.MASTERWORK && combined.maker().equals(Optional.of("Gabby")),
				"combining with a better-tempered copy takes its temper");
		anvil.getSlot(1).set(tempered(Items.DIAMOND_SWORD, Temper.of(Tier.ROUGH, false, null)));
		combined = Temper.on(anvil.getSlot(2).getItem());
		check(combined != null && combined.tier() == Tier.FINE, "combining with a worse-tempered copy keeps the better one");
		anvil.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
		combined = Temper.on(anvil.getSlot(2).getItem());
		check(combined != null && combined.tier() == Tier.FINE, "combining with an untempered copy keeps the temper");
		anvil.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD).copyWithCount(1));
		anvil.getSlot(0).getItem().setDamageValue(300);
		anvil.getSlot(1).set(tempered(Items.DIAMOND_SWORD, Temper.of(Tier.SUPERIOR, true, null)));
		combined = Temper.on(anvil.getSlot(2).getItem());
		check(combined != null && combined.tier() == Tier.SUPERIOR && combined.quenched(), "an untempered item picks up the temper of the copy it's combined with");
	}

	/** The grindstone strips enchantments and leaves the temper alone. */
	private void grindstone(MinecraftServer server) {
		Holder<Enchantment> sharpness = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		ItemStack sword = tempered(Items.DIAMOND_SWORD, Temper.of(Tier.SUPERIOR, false, null));
		sword.enchant(sharpness, 2);
		GrindstoneMenu grindstone = new GrindstoneMenu(2, player(server).getInventory(), ContainerLevelAccess.NULL);
		grindstone.getSlot(0).set(sword);
		ItemStack result = grindstone.getSlot(2).getItem();
		Temper temper = Temper.on(result);
		check(!result.isEmpty() && !result.isEnchanted() && temper != null && temper.tier() == Tier.SUPERIOR, "the grindstone keeps the temper");
	}

	/** A netherite upgrade carries the tier over, and the bonuses are worked out from netherite. */
	private void netheriteUpgrade(MinecraftServer server) {
		ItemStack diamond = tempered(Items.DIAMOND_PICKAXE, Temper.of(Tier.SUPERIOR, true, null));
		SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), diamond, new ItemStack(Items.NETHERITE_INGOT));
		ItemStack upgraded = server.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, server.overworld())
				.map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
		Temper temper = Temper.on(upgraded);
		check(upgraded.is(Items.NETHERITE_PICKAXE) && temper != null && temper.tier() == Tier.SUPERIOR && temper.quenched(), "the smithing table carries the temper over to netherite");
		// Netherite: 2031 max damage, speed 9. Superior quenched: +7% durability, +8% speed.
		check(upgraded.getMaxDamage() == 2173, "the upgraded pickaxe's durability is worked out from netherite (2173, got " + upgraded.getMaxDamage() + ")");
		check(near(upgraded.getDestroySpeed(Blocks.STONE.defaultBlockState()), 9.72F), "...and so is its mining speed (9.72)");
	}

	/** {@code /forgemark temper} and {@code /forgemark untemper} on the held item. */
	private void command(MinecraftServer server) {
		ServerPlayer player = player(server);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "forgemark temper superior quenched");
		Temper temper = Temper.on(player.getMainHandItem());
		check(temper != null && temper.tier() == Tier.SUPERIOR && temper.quenched() && temper.maker().isEmpty(), "/forgemark temper superior quenched tempers the held axe");
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "forgemark temper masterwork");
		temper = Temper.on(player.getMainHandItem());
		check(temper != null && temper.tier() == Tier.MASTERWORK && !temper.quenched() && temper.maker().equals(Optional.of(player.getName().getString())),
				"/forgemark temper masterwork signs it with the player's name");
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "forgemark untemper");
		check(Temper.on(player.getMainHandItem()) == null, "/forgemark untemper clears it");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "forgemark temper fine");
		check(Temper.on(player.getMainHandItem()) == null, "a stick can't be tempered by command either");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ItemStack tempered(Item item, Temper temper) {
		ItemStack stack = new ItemStack(item);
		Temper.put(stack, temper);
		return stack;
	}

	/** What the item adds to an attribute in a slot, as the game applies it when equipped. */
	private static double modifierTotal(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
		double[] total = {0};
		stack.forEachModifier(slot, (a, modifier) -> {
			if (a.value() == attribute.value()) {
				total[0] += modifier.amount();
			}
		});
		return total[0];
	}

	/** The same through the walk the tooltip uses. */
	private static double modifierTotal(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute, EquipmentSlotGroup group) {
		double[] total = {0};
		stack.forEachModifier(group, (a, modifier, display) -> {
			if (a.value() == attribute.value()) {
				total[0] += modifier.amount();
			}
		});
		return total[0];
	}

	private static boolean near(double actual, double expected) {
		return Math.abs(actual - expected) < 0.001;
	}

	private void check(boolean ok, String what) {
		if (ok) {
			Forgemark.LOGGER.info("[test] ok: {}", what);
		} else {
			Forgemark.LOGGER.error("[test] FAILED: {}", what);
			failures.add(what);
		}
	}
}
