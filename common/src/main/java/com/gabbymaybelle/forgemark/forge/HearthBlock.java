package com.gabbymaybelle.forgemark.forge;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The hearth: sits over a campfire, lava or magma and heats one ingot to forging heat. Right
 * click with an ingot to put it in, with an empty hand to take it out. {@code lit} is whether
 * there's a heat source under it; the ingot's glow is the block entity's business.
 */
public class HearthBlock extends Block implements EntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public HearthBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	/** A lit campfire, lava, magma or fire directly underneath. */
	public static boolean hasHeatSource(Level level, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState state = level.getBlockState(below);
		if (state.is(BlockTags.CAMPFIRES)) {
			return CampfireBlock.isLitCampfire(state);
		}
		return state.is(Blocks.MAGMA_BLOCK) || state.is(BlockTags.FIRE) || level.getFluidState(below).is(FluidTags.LAVA);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof HearthBlockEntity hearth) || !Metal.isHeatable(stack) || !hearth.getIngot().isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			hearth.insert(stack.split(1));
			level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof HearthBlockEntity hearth) || hearth.getIngot().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack ingot = hearth.takeOut();
			if (!player.getInventory().add(ingot)) {
				Block.popResource(level, pos.above(), ingot);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HearthBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ForgemarkBlocks.HEARTH_ENTITY.get()) {
			return null;
		}
		return (l, p, s, entity) -> ((HearthBlockEntity) entity).serverTick();
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) && random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.02, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
		}
	}

	/** The hearth as an item, with a line of teaching in its tooltip. */
	public static class Item extends BlockItem {
		public Item(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
			consumer.accept(Component.translatable("block.forgemark.hearth.desc").withStyle(ChatFormatting.GRAY));
		}
	}
}
