package com.gabbymaybelle.forgemark.compat;

import com.gabbymaybelle.forgemark.Forgemark;
import com.gabbymaybelle.forgemark.forge.ForgemarkBlocks;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlock;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.forge.Forging;
import com.gabbymaybelle.forgemark.forge.HearthBlock;
import com.gabbymaybelle.forgemark.forge.HearthBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade's overlay for the hearth (the ingot and how hot it is) and the forging anvil (the item,
 * the ingot and the strikes so far). Found through the {@code jade} entrypoint on Fabric and
 * the annotation on NeoForge, and only loaded when Jade is installed; Jade's API is compile-only.
 * Both block entities already sync their state to clients, so nothing is asked of the server.
 * The providers are nested classes reached only from {@link #registerClient}, so a dedicated
 * server never loads Jade's client-side tooltip classes.
 */
@WailaPlugin(Forgemark.MOD_ID)
public class JadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(Hearth.INSTANCE, HearthBlock.class);
		registration.registerBlockComponent(Anvil.INSTANCE, ForgingAnvilBlock.class);
	}

	private static Component ingotLine(ItemStack ingot, float heat, boolean hot) {
		return hot
				? Component.translatable("forgemark.jade.ingot.hot", ingot.getHoverName())
				: Component.translatable("forgemark.jade.ingot", ingot.getHoverName(), Math.round(heat * 100));
	}

	public enum Hearth implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!(accessor.getBlockEntity() instanceof HearthBlockEntity hearth)) {
				return;
			}
			if (!accessor.getBlockState().getValue(HearthBlock.LIT)) {
				tooltip.add(Component.translatable("forgemark.jade.hearth.cold"));
			}
			if (hearth.getIngot().isEmpty()) {
				tooltip.add(Component.translatable("forgemark.jade.empty"));
			} else {
				tooltip.add(ingotLine(hearth.getIngot(), hearth.getHeat(), hearth.isHot()));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return ForgemarkBlocks.id("hearth");
		}
	}

	public enum Anvil implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!(accessor.getBlockEntity() instanceof ForgingAnvilBlockEntity anvil)) {
				return;
			}
			if (anvil.getItem().isEmpty()) {
				tooltip.add(Component.translatable("forgemark.jade.empty"));
				return;
			}
			tooltip.add(Component.translatable("forgemark.jade.anvil.item", anvil.getItem().getHoverName()));
			long now = accessor.getLevel().getGameTime();
			if (!anvil.getIngot().isEmpty()) {
				tooltip.add(ingotLine(anvil.getIngot(), anvil.ingotHeat(now), anvil.isHotEnough(now)));
			}
			tooltip.add(Component.translatable("forgemark.jade.anvil.strikes", anvil.getStrikes().size(), Forging.STRIKES));
		}

		@Override
		public ResourceLocation getUid() {
			return ForgemarkBlocks.id("forging_anvil");
		}
	}
}
