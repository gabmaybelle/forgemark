package com.gabbymaybelle.forgemark;

import com.gabbymaybelle.forgemark.quality.Quality;
import com.gabbymaybelle.forgemark.quality.Temper;
import com.gabbymaybelle.forgemark.quality.Tier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /forgemark temper <tier> [quenched]} and {@code /forgemark untemper}: set or clear the
 * temper on the item in your main hand, for testing and for server admins. The forge itself
 * arrives with the hearth and anvil; until then this is the only way to temper something.
 */
public final class ForgemarkCommands {
	private ForgemarkCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Forgemark.MOD_ID)
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("temper")
						.then(Commands.argument("tier", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Tier.values()).map(Tier::getSerializedName), b))
								.executes(c -> temper(c, false))
								.then(Commands.literal("quenched").executes(c -> temper(c, true)))))
				.then(Commands.literal("untemper").executes(ForgemarkCommands::untemper)));
	}

	private static int temper(CommandContext<CommandSourceStack> context, boolean quenched) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String name = StringArgumentType.getString(context, "tier");
		Tier tier = Tier.byName(name);
		if (tier == null) {
			source.sendFailure(Component.translatable("commands.forgemark.unknown_tier", name));
			return 0;
		}
		ServerPlayer player = source.getPlayerOrException();
		ItemStack stack = player.getMainHandItem();
		if (!Quality.isTemperable(stack)) {
			source.sendFailure(Component.translatable("commands.forgemark.not_temperable", stack.getHoverName()));
			return 0;
		}
		Temper.put(stack, Temper.of(tier, quenched, player.getName().getString()));
		source.sendSuccess(() -> Component.translatable("commands.forgemark.temper.success", stack.getHoverName(), tier.displayName()), true);
		return 1;
	}

	private static int untemper(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ItemStack stack = source.getPlayerOrException().getMainHandItem();
		if (Temper.on(stack) == null) {
			source.sendFailure(Component.translatable("commands.forgemark.untemper.none", stack.getHoverName()));
			return 0;
		}
		Temper.put(stack, null);
		source.sendSuccess(() -> Component.translatable("commands.forgemark.untemper.success", stack.getHoverName()), true);
		return 1;
	}
}
