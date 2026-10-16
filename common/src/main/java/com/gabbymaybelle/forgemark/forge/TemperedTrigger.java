package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.platform.Services;
import com.gabbymaybelle.forgemark.quality.Temper;
import com.gabbymaybelle.forgemark.quality.Tier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * The {@code forgemark:tempered} advancement trigger: fired when a player finishes a temper at
 * the forging anvil (with the tier the strikes earned) and again when they quench the piece.
 * The conditions in a data file are all optional: {@code tier} is the lowest tier that counts,
 * {@code quenched} whether this was the quench, and {@code no_timing} whether the temper was
 * made in no-timing mode. The advancement trail in {@code data/forgemark/advancement} is built
 * on it by {@code tools/gen-data.js}.
 */
public class TemperedTrigger extends SimpleCriterionTrigger<TemperedTrigger.Instance> {
	public static final ResourceLocation ID = ForgemarkBlocks.id("tempered");
	private static final Supplier<CriterionTrigger<?>> REGISTERED = Services.PLATFORM.register(Registries.TRIGGER_TYPE, ID, TemperedTrigger::new);

	/** Loads this class, which queues the trigger's registration. */
	public static void register() {
	}

	public static TemperedTrigger get() {
		return (TemperedTrigger) REGISTERED.get();
	}

	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	/** A temper just made or quenched by this player. */
	public void trigger(ServerPlayer player, Temper temper, boolean noTiming) {
		trigger(player, instance -> instance.matches(temper, noTiming));
	}

	public record Instance(Optional<ContextAwarePredicate> player, Optional<Tier> tier, Optional<Boolean> quenched, Optional<Boolean> noTiming)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				Tier.CODEC.optionalFieldOf("tier").forGetter(Instance::tier),
				Codec.BOOL.optionalFieldOf("quenched").forGetter(Instance::quenched),
				Codec.BOOL.optionalFieldOf("no_timing").forGetter(Instance::noTiming)
		).apply(i, Instance::new));

		boolean matches(Temper temper, boolean noTiming) {
			return tier.map(lowest -> !lowest.isBetterThan(temper.tier())).orElse(true)
					&& quenched.map(wanted -> wanted == temper.quenched()).orElse(true)
					&& this.noTiming.map(wanted -> wanted == noTiming).orElse(true);
		}
	}
}
