package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.ForgemarkTags;
import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import com.gabbymaybelle.forgemark.forge.Forging;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.forge.Glowing;
import com.gabbymaybelle.forgemark.forge.Heat;
import com.gabbymaybelle.forgemark.forge.HearthBlock;
import com.gabbymaybelle.forgemark.forge.HearthBlockEntity;
import com.gabbymaybelle.forgemark.forge.Metal;
import com.gabbymaybelle.forgemark.quality.Quality;
import com.gabbymaybelle.forgemark.quality.Temper;
import com.gabbymaybelle.forgemark.quality.Tier;
import java.util.Optional;
import java.util.function.BiConsumer;
import com.gabbymaybelle.forgemark.harness.ClientGameTestContext;
import com.gabbymaybelle.forgemark.harness.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The forge in a real world: the hearth lit with flint and steel, the anvil, the hammer, the strikes
 * (decided on the server), the quench in a cauldron, and the settings around them. Called from
 * {@link ForgemarkClientTest}; the {@code check} it's given collects failures there.
 */
final class ForgeTest {
	private final BiConsumer<Boolean, String> check;
	private BlockPos hearthPos;
	private BlockPos anvilPos;
	private BlockPos cauldronPos;

	ForgeTest(BiConsumer<Boolean, String> check) {
		this.check = check;
	}

	/** Builds the forge next to the player and plays through it on the server thread. */
	void run(ClientGameTestContext ctx, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos base = player.blockPosition();
			// South of the player, which is where a new world's player faces: the anvil straight ahead.
			hearthPos = base.offset(2, 0, 3);
			anvilPos = base.offset(0, 0, 3);
			cauldronPos = base.offset(-2, 0, 3);
			level.setBlock(hearthPos.below(), Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(hearthPos, ForgemarkBlocks.HEARTH.get().defaultBlockState(), 3);
			level.setBlock(anvilPos.below(), Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(anvilPos, ForgemarkBlocks.FORGING_ANVIL.get().defaultBlockState(), 3);
			level.setBlock(cauldronPos.below(), Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(cauldronPos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
			check.accept(!done(player, "root") && !done(player, "into_the_fire") && !done(player, "tempered"), "the advancement trail starts empty");
			hearth(level, player);
			anvil(level, player);
			strikesPerHeat(level, player);
			settings(level, player);
			beyondTheDragon(level, player);
			// Leave a session running for the screenshot and the reopen: item and hot ingot on the anvil, two strikes in.
			ForgingAnvilBlockEntity anvil = anvil(level);
			anvil.placeItem(new ItemStack(Items.IRON_SWORD));
			anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ForgemarkBlocks.SMITHING_HAMMER.get()));
			aim(level, anvil);
			anvil.strike(player, anvil.getZoneCenter());
			anvil.strike(player, goodNear(aim(level, anvil)));
		});
		ctx.waitTicks(5);
		// The client owns its own view: face south and a little down, onto the anvil three blocks away.
		ctx.runOnClient(client -> {
			if (client.player != null) {
				client.player.setYRot(0.0F);
				client.player.setXRot(20.0F);
				client.player.yRotO = 0.0F;
				client.player.xRotO = 20.0F;
			}
		});
		ctx.waitTicks(10);
		boolean lookingAtAnvil = ctx.computeOnClient(client -> client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(anvilPos)
				&& client.level != null && client.level.getBlockEntity(anvilPos) instanceof ForgingAnvilBlockEntity anvil && anvil.canStrike() && anvil.getStrikes().size() == 2);
		check.accept(lookingAtAnvil, "the client sees the anvil mid-session (the strike bar is drawn in the screenshot)");
		world.getServer().runOnServer(server -> check.accept(done(server.getPlayerList().getPlayers().getFirst(), "root"),
				"the trail's root is awarded once the player holds the hammer (an inventory change, seen on the next tick)"));
		ctx.takeScreenshot("forgemark_strike_bar");
	}

	/** After the world is reopened: the anvil still holds the session. */
	void afterReopen(MinecraftServer server) {
		ForgingAnvilBlockEntity anvil = anvil(server.overworld());
		check.accept(anvil.getItem().is(Items.IRON_SWORD) && anvil.getIngot().is(Items.IRON_INGOT) && anvil.getStrikes().size() == 2,
				"the anvil keeps its item, ingot and strikes through a save and reopen");
	}

	private HearthBlockEntity hearth(ServerLevel level, BlockPos pos) {
		return (HearthBlockEntity) level.getBlockEntity(pos);
	}

	private ForgingAnvilBlockEntity anvil(ServerLevel level) {
		return (ForgingAnvilBlockEntity) level.getBlockEntity(anvilPos);
	}

	/**
	 * Strikes per heat with time passing as in play: the ingot cools while the marker sweeps, so
	 * each strike here comes two seconds (one full sweep) after the last. A heat gives the metal's
	 * count (iron 3, gold 2, netherite 4) while the ingot glows, and a trip to the hearth starts a
	 * new count. Before 0.1.4 each strike took its share of the heat on top of the cooling, so iron
	 * gave only two.
	 */
	private void strikesPerHeat(ServerLevel level, ServerPlayer player) {
		ForgingAnvilBlockEntity anvil = anvil(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ForgemarkBlocks.SMITHING_HAMMER.get()));
		Object[][] metals = { { Items.IRON_SWORD, Items.IRON_INGOT, 3 }, { Items.GOLDEN_SWORD, Items.GOLD_INGOT, 2 }, { Items.NETHERITE_SWORD, Items.NETHERITE_INGOT, 4 } };
		for (Object[] metal : metals) {
			net.minecraft.world.item.Item ingot = (net.minecraft.world.item.Item) metal[1];
			int perHeat = (int) metal[2];
			anvil.placeItem(new ItemStack((net.minecraft.world.item.Item) metal[0]));
			anvil.placeIngot(hotIngot(level, ingot));
			for (int i = 0; i < perHeat; i++) {
				cool(anvil, 40);
				aim(level, anvil);
				anvil.strike(player, anvil.getZoneCenter());
			}
			check.accept(anvil.getStrikes().size() == perHeat, ingot + ": a heat gives " + perHeat + " strikes, two seconds apart (got " + anvil.getStrikes().size() + ")");
			cool(anvil, 40);
			aim(level, anvil);
			anvil.strike(player, anvil.getZoneCenter());
			check.accept(anvil.getStrikes().size() == perHeat, ingot + ": the next strike is refused until it's reheated");
			anvil.takeItem();
			anvil.takeIngot();
		}

		// The glow is the limit: an iron ingot 17 seconds out of the hearth can be struck, one 20 seconds out can't.
		anvil.placeItem(new ItemStack(Items.IRON_SWORD));
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		cool(anvil, 400);
		aim(level, anvil);
		anvil.strike(player, anvil.getZoneCenter());
		check.accept(anvil.getStrikes().isEmpty(), "an ingot that has stopped glowing (20 seconds out) can't be struck");
		anvil.takeIngot();
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		cool(anvil, 340);
		aim(level, anvil);
		anvil.strike(player, anvil.getZoneCenter());
		check.accept(anvil.getStrikes().size() == 1, "an ingot that still glows (17 seconds out) can");

		// A trip to the hearth starts a new count, and the strikes on the anvil are kept: two strikes
		// from a fresh heat (three made in all), then back to the hearth.
		anvil.takeIngot();
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		for (int i = 0; i < 2; i++) {
			cool(anvil, 40);
			aim(level, anvil);
			anvil.strike(player, anvil.getZoneCenter());
		}
		check.accept(anvil.getStrikes().size() == 3 && Heat.strikesUsed(anvil.getIngot()) == 2 && anvil.strikesLeftThisHeat() == 1,
				"two strikes from a fresh heat leave one (got " + anvil.getStrikes().size() + " made, " + anvil.strikesLeftThisHeat() + " left)");
		HearthBlockEntity hearth = hearth(level, hearthPos);
		hearth.insert(anvil.takeIngot());
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		anvil.placeIngot(hearth.takeOut());
		check.accept(anvil.getStrikes().size() == 3 && Heat.strikesUsed(anvil.getIngot()) == 0 && anvil.strikesLeftThisHeat() == 3,
				"reheating gives a fresh three strikes and keeps the three made");
		anvil.takeItem();
		anvil.takeIngot();

		// An ingot saved before 0.1.4 has no strike count: it reads as none used.
		Heat old = Heat.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("{\"heat\":0.9,\"at\":5}")).getOrThrow();
		check.accept(old.heat() == 0.9F && old.at() == 5L && old.strikes() == 0, "an ingot heated before the strike count reads as no strikes used");
	}

	/** Moves the ingot on the anvil this many ticks further from the hearth: it cools as if the time had passed. */
	private static void cool(ForgingAnvilBlockEntity anvil, int ticks) {
		Heat heat = Heat.on(anvil.getIngot());
		if (heat != null) {
			anvil.getIngot().set(Heat.type(), new Heat(heat.heat(), heat.at() - ticks, heat.strikes()));
		}
	}

	/** An ingot straight from a hot hearth. */
	private ItemStack hotIngot(ServerLevel level, net.minecraft.world.item.Item item) {
		HearthBlockEntity hearth = hearth(level, hearthPos);
		hearth.insert(new ItemStack(item));
		for (int i = 0; i < Metal.SLOW_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		return hearth.takeOut();
	}

	/** A right-click on the top of a block with whatever the player holds, as the game does it. */
	private static void click(ServerLevel level, ServerPlayer player, BlockPos pos) {
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	/** Whether the player has one of Forgemark's advancements. */
	private static boolean done(ServerPlayer player, String name) {
		return player.getAdvancements().getOrStartProgress(player.level().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(Forgemark.MOD_ID, name))).isDone();
	}

	private void hearth(ServerLevel level, ServerPlayer player) {
		HearthBlockEntity hearth = hearth(level, hearthPos);
		hearth.serverTick();
		check.accept(!level.getBlockState(hearthPos).getValue(HearthBlock.LIT), "a new hearth starts out");
		// Lit like a campfire, with a real right-click.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		click(level, player, hearthPos);
		check.accept(level.getBlockState(hearthPos).getValue(HearthBlock.LIT) && player.getMainHandItem().getDamageValue() == 1,
				"flint and steel lights it, for one use of the flint (damage " + player.getMainHandItem().getDamageValue() + ")");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		hearth.insert(new ItemStack(Items.IRON_INGOT));
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS / 2; i++) {
			hearth.serverTick();
		}
		check.accept(hearth.getHeat() > 0.45F && hearth.getHeat() < 0.55F && !hearth.isHot(), "an iron ingot is half heated after 5 seconds (" + hearth.getHeat() + ")");
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS / 2; i++) {
			hearth.serverTick();
		}
		check.accept(hearth.isHot(), "...and at forging heat after 10");
		ItemStack ingot = hearth.takeOut();
		long now = level.getGameTime();
		check.accept(ingot.is(Items.IRON_INGOT) && Heat.now(ingot, now) == 1.0F && hearth.getIngot().isEmpty(), "the ingot comes out hot, with its heat on it");
		check.accept(Heat.now(ingot, now + Heat.COOLING_TICKS / 2) == 0.5F && Heat.now(ingot, now + Heat.COOLING_TICKS) == 0.0F, "it cools to nothing in 30 seconds out of the hearth");

		hearth.insert(new ItemStack(Items.GOLD_INGOT));
		for (int i = 0; i < Metal.QUICK_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		check.accept(hearth.isHot(), "gold heats in 6 seconds");
		hearth.takeOut();
		hearth.insert(new ItemStack(Items.NETHERITE_INGOT));
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		check.accept(!hearth.isHot() && hearth.getHeat() > 0.45F, "netherite is only halfway after 10 seconds");
		hearth.takeOut();

		BlockPos cold = hearthPos.offset(0, 0, 4);
		// A fire underneath does nothing any more: the hearth is lit on its own or not at all.
		level.setBlock(cold.below(), Blocks.CAMPFIRE.defaultBlockState(), 3);
		level.setBlock(cold, ForgemarkBlocks.HEARTH.get().defaultBlockState(), 3);
		HearthBlockEntity coldHearth = hearth(level, cold);
		coldHearth.insert(new ItemStack(Items.IRON_INGOT));
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS; i++) {
			coldHearth.serverTick();
		}
		check.accept(!level.getBlockState(cold).getValue(HearthBlock.LIT) && coldHearth.getHeat() == 0.0F, "an unlit hearth stays cold, even over a campfire");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE, 2));
		click(level, player, cold);
		check.accept(level.getBlockState(cold).getValue(HearthBlock.LIT) && player.getMainHandItem().getCount() == 1, "a fire charge lights it too, and is used up");
		coldHearth.serverTick();
		check.accept(coldHearth.getHeat() > 0.0F, "once lit, the ingot in it starts to heat");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		click(level, player, cold);
		check.accept(!level.getBlockState(cold).getValue(HearthBlock.LIT) && player.getMainHandItem().getDamageValue() == 1, "a shovel puts it out, like a campfire");
		float warm = coldHearth.getHeat();
		coldHearth.serverTick();
		check.accept(coldHearth.getHeat() < warm, "and out, the ingot cools");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		level.setBlock(cold.below(), Blocks.STONE.defaultBlockState(), 3);
		coldHearth.takeOut();
		check.accept(!Metal.isHeatable(new ItemStack(Items.STICK)) && !Metal.isHeatable(new ItemStack(Items.LEATHER)) && Metal.isHeatable(new ItemStack(Items.DIAMOND)),
				"the hearth takes ingots and gems, not sticks or leather");
		// Another mod's metal, in no tag at all: it looks like a metal by name, so it heats; its nugget doesn't.
		ItemStack testium = new ItemStack(Testium.INGOT.get());
		check.accept(!testium.is(ForgemarkTags.HEATABLE) && Metal.isHeatable(testium), "a modded ingot in none of the tags is heatable by its name");
		check.accept(!Metal.isHeatable(new ItemStack(Testium.NUGGET.get())) && !Metal.isHeatable(new ItemStack(Items.PHANTOM_MEMBRANE)) && !Metal.isHeatable(new ItemStack(Items.OAK_PLANKS)),
				"a nugget, a membrane and planks aren't");
		hearth.insert(testium);
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		check.accept(hearth.isHot() && Metal.strikesPerHeat(hearth.getIngot()) == Metal.DEFAULT_STRIKES_PER_HEAT, "it heats in the default 10 seconds and is good for the default 3 strikes");
		hearth.takeOut();

		// "Into the Fire": an ingot put into a lit hearth, through the same path a right-click takes (vanilla's item_used_on_block).
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 2));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cold), Direction.UP, cold, false));
		check.accept(coldHearth.getIngot().is(Items.IRON_INGOT) && !done(player, "into_the_fire"), "an ingot in a cold hearth doesn't earn Into the Fire");
		coldHearth.takeOut();
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(hearthPos), Direction.UP, hearthPos, false));
		check.accept(hearth.getIngot().is(Items.IRON_INGOT) && done(player, "into_the_fire"), "an ingot in a lit hearth does");
		hearth.takeOut();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private void anvil(ServerLevel level, ServerPlayer player) {
		ForgingAnvilBlockEntity anvil = anvil(level);
		BlockState state = level.getBlockState(anvilPos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(anvilPos), Direction.UP, anvilPos, false);
		ItemStack hammer = new ItemStack(ForgemarkBlocks.SMITHING_HAMMER.get());

		// Placing things through the block, as a player would.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		check.accept(anvil.getItem().isEmpty(), "a stick can't be laid on the anvil");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		check.accept(anvil.getItem().is(Items.IRON_PICKAXE) && player.getMainHandItem().isEmpty(), "an iron pickaxe goes on the anvil");
		player.setItemInHand(InteractionHand.MAIN_HAND, hotIngot(level, Items.GOLD_INGOT));
		state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		check.accept(anvil.getIngot().isEmpty(), "a gold ingot is refused: it doesn't repair an iron pickaxe");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
		state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		check.accept(anvil.getIngot().is(Items.IRON_INGOT), "a cold iron ingot is accepted (it just can't be struck yet)");
		player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
		anvil.strike(player, anvil.getZoneCenter());
		check.accept(anvil.getStrikes().isEmpty(), "a cold ingot can't be struck");
		state.useWithoutItem(level, player, hit);
		check.accept(anvil.getIngot().isEmpty() && anvil.getItem().is(Items.IRON_PICKAXE), "an empty hand takes the ingot back first");
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));

		// Five perfect strikes, with the reheat iron needs after three.
		player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
		for (int i = 0; i < 3; i++) {
			aim(level, anvil);
			anvil.strike(player, anvil.getZoneCenter());
		}
		check.accept(anvil.getStrikes().size() == 3 && anvil.getStrikes().stream().allMatch(s -> s == Forging.PERFECT), "three strikes on the zone centre are perfect");
		check.accept(hammer.getDamageValue() == 3, "the hammer wears one point per strike (got " + hammer.getDamageValue() + ")");
		anvil.strike(player, anvil.getZoneCenter());
		check.accept(anvil.getStrikes().size() == 3, "the fourth strike is refused: iron is good for three per heat");
		ItemStack dim = anvil.takeIngot();
		check.accept(anvil.getStrikes().size() == 3, "taking the ingot off to reheat keeps the strikes");
		HearthBlockEntity hearth = hearth(level, hearthPos);
		hearth.insert(dim);
		for (int i = 0; i < Metal.DEFAULT_HEAT_TICKS; i++) {
			hearth.serverTick();
		}
		anvil.placeIngot(hearth.takeOut());
		aim(level, anvil);
		anvil.strike(player, anvil.getZoneCenter());
		aim(level, anvil);
		anvil.strike(player, anvil.getZoneCenter());
		ItemStack pickaxe = anvil.getItem();
		Temper temper = Temper.on(pickaxe);
		long now = level.getGameTime();
		check.accept(temper != null && temper.tier() == Tier.MASTERWORK && !temper.quenched() && temper.maker().equals(Optional.of(player.getName().getString())),
				"five perfect strikes make a Masterwork pickaxe signed by the player");
		check.accept(anvil.getIngot().isEmpty() && anvil.getStrikes().isEmpty(), "the ingot is spent and the anvil is ready for the next");
		check.accept(Glowing.isHot(pickaxe, now) && !Glowing.isHot(pickaxe, now + Glowing.TICKS + 1), "the finished item glows for 15 seconds");
		check.accept(done(player, "tempered") && done(player, "fine_work") && done(player, "masterwork"), "a Masterwork temper earns Tempered, A Steady Hand and Masterwork");
		check.accept(!done(player, "quenched") && !done(player, "patient_way"), "...but not Hiss or The Patient Way yet");

		// The quench.
		ItemStack finished = anvil.takeItem();
		player.setItemInHand(InteractionHand.MAIN_HAND, finished);
		BlockState cauldron = level.getBlockState(cauldronPos);
		BlockHitResult cauldronHit = new BlockHitResult(Vec3.atCenterOf(cauldronPos), Direction.UP, cauldronPos, false);
		cauldron.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, cauldronHit);
		temper = Temper.on(player.getMainHandItem());
		check.accept(temper != null && temper.quenched() && temper.tier() == Tier.MASTERWORK, "dipping it in a water cauldron quenches it");
		check.accept(level.getBlockState(cauldronPos).getValue(LayeredCauldronBlock.LEVEL) == 2, "the cauldron loses a level");
		check.accept(!Glowing.isHot(player.getMainHandItem(), now), "a quenched item stops glowing");
		check.accept(done(player, "quenched"), "the quench earns Hiss");
		cauldron = level.getBlockState(cauldronPos);
		cauldron.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, cauldronHit);
		check.accept(level.getBlockState(cauldronPos).getValue(LayeredCauldronBlock.LEVEL) == 2, "dipping it again does nothing");
		ItemStack plain = new ItemStack(Items.IRON_SWORD);
		player.setItemInHand(InteractionHand.MAIN_HAND, plain);
		cauldron.useItemOn(plain, level, player, InteractionHand.MAIN_HAND, cauldronHit);
		check.accept(Temper.on(plain) == null && level.getBlockState(cauldronPos).getValue(LayeredCauldronBlock.LEVEL) == 2, "an untempered item in the cauldron is left to vanilla");

		// Misses, mixed strikes, and re-tempering.
		player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 0, 0, 5) == Tier.ROUGH, "five misses make a Rough sword");
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 0, 2, 3) == Tier.ROUGH, "two good strikes and three misses are still Rough");
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 0, 3, 2) == Tier.STANDARD, "three good strikes make Standard");
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 0, 5, 0) == Tier.FINE, "five good strikes make Fine");
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 2, 3, 0) == Tier.SUPERIOR, "two perfect and three good make Superior");
		check.accept(forge(level, player, new ItemStack(Items.IRON_SWORD), 4, 1, 0) == Tier.SUPERIOR, "four perfect and one good are Superior, not Masterwork");
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		forge(level, player, sword, 5, 0, 0);
		Tier again = forge(level, player, sword, 0, 0, 5);
		check.accept(again == Tier.MASTERWORK, "tempering a Masterwork sword again with misses keeps Masterwork");
		Temper.put(sword, Temper.of(Tier.FINE, true, null));
		forge(level, player, sword, 0, 5, 0);
		temper = Temper.on(sword);
		check.accept(temper != null && temper.tier() == Tier.FINE && temper.quenched(), "an equal result keeps the old quench");

		// Lag: a report within the window counts where the player saw it; one far off is scored where the server saw it.
		anvil.placeItem(new ItemStack(Items.IRON_SWORD));
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		anvil.strike(player, missNear(aim(level, anvil)));
		check.accept(anvil.getStrikes().size() == 1 && anvil.getStrikes().getFirst() == Forging.MISS, "a press reported just outside the zone, within the lag window, is a miss as reported");
		aim(level, anvil);
		float farAway = (anvil.getZoneCenter() + 0.5F) % 1.0F;
		anvil.strike(player, farAway);
		check.accept(anvil.getStrikes().size() == 2 && anvil.getStrikes().get(1) == Forging.PERFECT,
				"a press reported half a bar away from the server's marker is scored where the server saw it (on the centre)");
		anvil.takeItem();
		check.accept(Forging.plausible(0.50F, 0.60F) && !Forging.plausible(0.50F, 0.70F), "the lag window is 0.15 of the bar");

		// No-timing mode: sneak to make every strike perfect, for a second ingot at the end.
		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
		anvil.placeItem(new ItemStack(Items.IRON_SWORD));
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		player.setShiftKeyDown(true);
		anvil.strike(player, missNear(aim(level, anvil)));
		check.accept(anvil.getStrikes().isEmpty(), "no-timing mode is refused without the extra ingot in the inventory");
		player.getInventory().add(new ItemStack(Items.IRON_INGOT, 3));
		anvil.strike(player, missNear(aim(level, anvil)));
		check.accept(anvil.getStrikes().size() == 1 && anvil.getStrikes().getFirst() == Forging.PERFECT, "sneaking with the extra ingot makes the strike perfect wherever the marker is");
		for (int i = 0; i < 4; i++) {
			if (i == 2) {
				anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
			}
			anvil.strike(player, missNear(aim(level, anvil)));
		}
		temper = Temper.on(anvil.getItem());
		check.accept(temper != null && temper.tier() == Tier.MASTERWORK, "no-timing mode reaches Masterwork");
		check.accept(player.getInventory().countItem(Items.IRON_INGOT) == 2, "...and takes the extra ingot at the end (3 left 2)");
		check.accept(done(player, "patient_way"), "a no-timing temper earns The Patient Way");
		player.setShiftKeyDown(false);
		anvil.takeItem();
		Config.noTimingMode = false;
		anvil.placeItem(new ItemStack(Items.IRON_SWORD));
		anvil.placeIngot(hotIngot(level, Items.IRON_INGOT));
		player.setShiftKeyDown(true);
		anvil.strike(player, missNear(aim(level, anvil)));
		check.accept(anvil.getStrikes().size() == 1 && anvil.getStrikes().getFirst() == Forging.MISS, "with no-timing mode off, sneaking is an ordinary timed strike");
		player.setShiftKeyDown(false);
		Config.noTimingMode = true;
		anvil.takeItem();
		player.getInventory().clearContent();

		// A Rough temper never breaks a worn item.
		ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		forge(level, player, worn, 0, 0, 5);
		check.accept(Temper.on(worn) != null && Temper.on(worn).tier() == Tier.ROUGH && worn.getDamageValue() == worn.getMaxDamage() - 1,
				"a Rough temper on a nearly worn-out pickaxe leaves it one use from breaking, not broken");

		// Eligibility now that the ingot must be heatable.
		check.accept(!Quality.isTemperable(new ItemStack(Items.LEATHER_HELMET)) && !Quality.isTemperable(new ItemStack(Items.TURTLE_HELMET)),
				"leather and turtle armour can't be tempered: nothing to heat");
		check.accept(Quality.isTemperable(hammer), "the smithing hammer, repaired with iron, can be tempered itself");

		// Another mod's sword, repaired through that mod's own tag, tempered with that mod's ingot.
		ItemStack testiumSword = new ItemStack(Testium.SWORD.get());
		check.accept(Quality.isTemperable(testiumSword) && Metal.repairs(testiumSword, new ItemStack(Testium.INGOT.get())) && !Metal.repairs(testiumSword, new ItemStack(Items.IRON_INGOT)),
				"a modded sword repaired through its own tag can be tempered, and asks for its own ingot");
		check.accept(forge(level, player, testiumSword, Testium.INGOT.get(), 2, 3, 0) == Tier.SUPERIOR, "...and reaches Superior with two perfect and three good strikes on its ingot");
	}

	/**
	 * With Beyond the Dragon installed (-PtestBeyondTheDragon): aphelite gear, repaired through
	 * that mod's own tag and with its ingot in no shared tag, tempers like netherite gear. The
	 * items are looked up by id, since the test never compiles against the mod.
	 */
	private void beyondTheDragon(ServerLevel level, ServerPlayer player) {
		if (!FabricLoader.getInstance().isModLoaded("beyond_the_dragon")) {
			return;
		}
		Item ingotItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("beyond_the_dragon", "aphelite_ingot"));
		Item swordItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("beyond_the_dragon", "aphelite_sword"));
		ItemStack ingot = new ItemStack(ingotItem);
		ItemStack sword = new ItemStack(swordItem);
		check.accept(!sword.is(Items.AIR) && !ingot.is(Items.AIR), "Beyond the Dragon's aphelite sword and ingot are there");
		check.accept(Metal.isHeatable(ingot) && Metal.heatTicks(ingot) == Metal.SLOW_HEAT_TICKS && Metal.strikesPerHeat(ingot) == Metal.HARD_STRIKES_PER_HEAT,
				"an aphelite ingot heats slowly and is good for four strikes, like netherite");
		check.accept(Quality.isTemperable(sword) && Metal.repairs(sword, ingot) && !Metal.repairs(sword, new ItemStack(Items.NETHERITE_INGOT)),
				"an aphelite sword can be tempered and asks for aphelite, not netherite");
		int base = sword.getMaxDamage();
		check.accept(forge(level, player, sword, ingotItem, 5, 0, 0) == Tier.MASTERWORK, "five perfect strikes with aphelite make it Masterwork");
		check.accept(sword.getMaxDamage() == Math.round(base * 1.15F), "...with +15% durability worked out from aphelite's own " + base + " (got " + sword.getMaxDamage() + ")");
		ItemStack pickaxe = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("beyond_the_dragon", "aphelite_pickaxe")));
		check.accept(forge(level, player, pickaxe, ingotItem, 0, 5, 0) == Tier.FINE, "five good strikes make an aphelite pickaxe Fine");
	}

	/**
	 * Lines the server's own marker up with the zone centre, as a well-timed player would. The
	 * server samples the marker on whole ticks, so it lands within 0.025 of the centre; returns
	 * where it actually is, which is what a report has to be close to.
	 */
	private static float aim(ServerLevel level, ForgingAnvilBlockEntity anvil) {
		anvil.setSweepStart(level.getGameTime() - Math.round(anvil.getZoneCenter() * Forging.SWEEP_TICKS / 2));
		return Forging.markerPosition(level.getGameTime() - anvil.getSweepStart());
	}

	/** A report at the edge of the lag window: outside the wide zone however the tick rounded. */
	private static float missNear(float serverMarker) {
		return serverMarker + 0.149F <= 1.0F ? serverMarker + 0.149F : serverMarker - 0.149F;
	}

	/** A report inside the wide zone but outside the narrow one, however the tick rounded. */
	private static float goodNear(float serverMarker) {
		return serverMarker + 0.08F <= 1.0F ? serverMarker + 0.08F : serverMarker - 0.08F;
	}

	/** Lays the item down, heats an iron ingot and strikes: so many perfect, good and missed. Returns the tier. */
	private Tier forge(ServerLevel level, ServerPlayer player, ItemStack item, int perfect, int good, int miss) {
		return forge(level, player, item, Items.IRON_INGOT, perfect, good, miss);
	}

	private Tier forge(ServerLevel level, ServerPlayer player, ItemStack item, Item ingot, int perfect, int good, int miss) {
		ForgingAnvilBlockEntity anvil = anvil(level);
		if (!player.getMainHandItem().is(ForgemarkBlocks.SMITHING_HAMMER.get())) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ForgemarkBlocks.SMITHING_HAMMER.get()));
		}
		anvil.placeItem(item);
		anvil.placeIngot(hotIngot(level, ingot));
		int struck = 0;
		for (int i = 0; i < Forging.STRIKES; i++) {
			if (struck == Metal.DEFAULT_STRIKES_PER_HEAT) {
				anvil.placeIngot(hotIngot(level, ingot));
				struck = 0;
			}
			float at = aim(level, anvil);
			float position = i < perfect ? at : i < perfect + good ? goodNear(at) : missNear(at);
			anvil.strike(player, position);
			struck++;
		}
		Temper temper = Temper.on(anvil.getItem());
		anvil.takeItem();
		return temper == null ? null : temper.tier();
	}

	private void settings(ServerLevel level, ServerPlayer player) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		Temper.put(sword, Temper.of(Tier.MASTERWORK, true, null));
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		DamageSource source = level.damageSources().playerAttack(player);
		check.accept(Quality.pvpDamage(source, 10.0F) == 10.0F, "with affect-PvP on, a hit on a player keeps the temper bonus");
		Config.affectPvp = false;
		// Iron sword: +5 modifier, shown as 6; Masterwork quenched is +10% of 6 = 0.6.
		check.accept(Math.abs(Quality.pvpDamage(source, 10.0F) - 9.4F) < 0.001F, "with it off, the bonus (0.6) comes back out of the hit (got " + Quality.pvpDamage(source, 10.0F) + ")");
		Config.affectPvp = true;
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		Config.maxTier = "fine";
		Tier capped = forge(level, player, new ItemStack(Items.IRON_SWORD), 5, 0, 0);
		Config.maxTier = "masterwork";
		check.accept(capped == Tier.FINE, "the maximum-tier setting caps what the forge gives out");
	}
}
