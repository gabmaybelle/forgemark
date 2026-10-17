package com.gabbymaybelle.forgemark.client;

import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlock;
import com.gabbymaybelle.forgemark.forge.ForgingAnvilBlockEntity;
import com.gabbymaybelle.forgemark.forge.HearthBlockEntity;
import com.gabbymaybelle.forgemark.forge.Heat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What sits on the blocks: the ingot in the hearth, glowing as it heats, and the item and
 * ingot on the anvil. Items are laid flat and drawn with the game's own item models, so any
 * mod's gear looks right.
 */
public final class ForgeRenderers {
	private ForgeRenderers() {
	}

	/** Full light once the ingot is hot enough to glow, the block's own light otherwise. */
	private static int glow(float heat, int blockLight) {
		return heat >= Heat.GLOWING ? LightCoordsUtil.FULL_BRIGHT : blockLight;
	}

	private static void extract(ItemModelResolver resolver, ItemStackRenderState into, ItemStack stack, @Nullable Level level, int seed) {
		resolver.updateForTopItem(into, stack, ItemDisplayContext.FIXED, level, null, seed);
	}

	/** The hearth's ingot, lying in the bed on top. */
	public static class Hearth implements BlockEntityRenderer<HearthBlockEntity, Hearth.State> {
		public static class State extends BlockEntityRenderState {
			public final ItemStackRenderState ingot = new ItemStackRenderState();
			public float heat;
		}

		private final ItemModelResolver resolver;

		public Hearth(BlockEntityRendererProvider.Context context) {
			resolver = context.itemModelResolver();
		}

		@Override
		public State createRenderState() {
			return new State();
		}

		@Override
		public void extractRenderState(HearthBlockEntity hearth, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
			BlockEntityRenderer.super.extractRenderState(hearth, state, partialTicks, camera, breakProgress);
			extract(resolver, state.ingot, hearth.getIngot(), hearth.getLevel(), (int) hearth.getBlockPos().asLong());
			state.heat = hearth.getHeat();
		}

		@Override
		public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
			if (state.ingot.isEmpty()) {
				return;
			}
			poseStack.pushPose();
			poseStack.translate(0.5F, 1.03F, 0.5F);
			poseStack.rotateDegrees(Axis.XP, 90.0F);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			state.ingot.submit(poseStack, collector, glow(state.heat, state.lightCoords), OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}

	/** The anvil's item across the top, with the ingot beside it. */
	public static class Anvil implements BlockEntityRenderer<ForgingAnvilBlockEntity, Anvil.State> {
		public static class State extends BlockEntityRenderState {
			public final ItemStackRenderState item = new ItemStackRenderState();
			public final ItemStackRenderState ingot = new ItemStackRenderState();
			public float heat;
			public Direction facing = Direction.NORTH;
		}

		private final ItemModelResolver resolver;

		public Anvil(BlockEntityRendererProvider.Context context) {
			resolver = context.itemModelResolver();
		}

		@Override
		public State createRenderState() {
			return new State();
		}

		@Override
		public void extractRenderState(ForgingAnvilBlockEntity anvil, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
			BlockEntityRenderer.super.extractRenderState(anvil, state, partialTicks, camera, breakProgress);
			int seed = (int) anvil.getBlockPos().asLong();
			extract(resolver, state.item, anvil.getItem(), anvil.getLevel(), seed);
			extract(resolver, state.ingot, anvil.getIngot(), anvil.getLevel(), seed + 1);
			Level level = anvil.getLevel();
			state.heat = level == null ? 0.0F : anvil.ingotHeat(level.getGameTime());
			state.facing = anvil.getBlockState().getValue(ForgingAnvilBlock.FACING);
		}

		@Override
		public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
			poseStack.pushPose();
			poseStack.translate(0.5F, 1.02F, 0.5F);
			poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
			poseStack.rotateDegrees(Axis.XP, 90.0F);
			if (!state.item.isEmpty()) {
				poseStack.pushPose();
				poseStack.translate(-0.12F, 0.0F, 0.0F);
				poseStack.scale(0.55F, 0.55F, 0.55F);
				state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				poseStack.popPose();
			}
			if (!state.ingot.isEmpty()) {
				poseStack.pushPose();
				poseStack.translate(0.22F, 0.0F, -0.01F);
				poseStack.scale(0.35F, 0.35F, 0.35F);
				state.ingot.submit(poseStack, collector, glow(state.heat, state.lightCoords), OverlayTexture.NO_OVERLAY, 0);
				poseStack.popPose();
			}
			poseStack.popPose();
		}
	}
}
