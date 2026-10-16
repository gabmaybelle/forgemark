package com.gabbymaybelle.forgemark.client;

import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlock;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.forge.HearthBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What sits on the blocks: the ingot in the hearth, glowing as it heats, and the item and
 * ingot on the anvil. Items are laid flat and drawn with the game's own item models, so any
 * mod's gear looks right. (1.21.1 draws straight from the block entity; 26.x goes through
 * render states first.)
 */
public final class ForgeRenderers {
	private ForgeRenderers() {
	}

	/** Full light once the ingot is hot enough to glow, the block's own light otherwise. */
	private static int glow(float heat, int blockLight) {
		return heat >= 0.4F ? LightTexture.FULL_BRIGHT : blockLight;
	}

	private static void draw(ItemRenderer items, ItemStack stack, int light, PoseStack poseStack, MultiBufferSource buffers, @Nullable Level level, int seed) {
		items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffers, level, seed);
	}

	/** The hearth's ingot, lying in the bed on top. */
	public static class Hearth implements BlockEntityRenderer<HearthBlockEntity> {
		private final ItemRenderer items;

		public Hearth(BlockEntityRendererProvider.Context context) {
			items = context.getItemRenderer();
		}

		@Override
		public void render(HearthBlockEntity hearth, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
			ItemStack ingot = hearth.getIngot();
			if (ingot.isEmpty()) {
				return;
			}
			poseStack.pushPose();
			poseStack.translate(0.5F, 1.03F, 0.5F);
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			poseStack.scale(0.5F, 0.5F, 0.5F);
			draw(items, ingot, glow(hearth.getHeat(), light), poseStack, buffers, hearth.getLevel(), (int) hearth.getBlockPos().asLong());
			poseStack.popPose();
		}
	}

	/** The anvil's item across the top, with the ingot beside it. */
	public static class Anvil implements BlockEntityRenderer<ForgingAnvilBlockEntity> {
		private final ItemRenderer items;

		public Anvil(BlockEntityRendererProvider.Context context) {
			items = context.getItemRenderer();
		}

		@Override
		public void render(ForgingAnvilBlockEntity anvil, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
			Level level = anvil.getLevel();
			int seed = (int) anvil.getBlockPos().asLong();
			Direction facing = anvil.getBlockState().getValue(ForgingAnvilBlock.FACING);
			poseStack.pushPose();
			poseStack.translate(0.5F, 1.02F, 0.5F);
			poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			if (!anvil.getItem().isEmpty()) {
				poseStack.pushPose();
				poseStack.translate(-0.12F, 0.0F, 0.0F);
				poseStack.scale(0.55F, 0.55F, 0.55F);
				draw(items, anvil.getItem(), light, poseStack, buffers, level, seed);
				poseStack.popPose();
			}
			if (!anvil.getIngot().isEmpty()) {
				float heat = level == null ? 0.0F : anvil.ingotHeat(level.getGameTime());
				poseStack.pushPose();
				poseStack.translate(0.22F, 0.0F, -0.01F);
				poseStack.scale(0.35F, 0.35F, 0.35F);
				draw(items, anvil.getIngot(), glow(heat, light), poseStack, buffers, level, seed + 1);
				poseStack.popPose();
			}
			poseStack.popPose();
		}
	}
}
