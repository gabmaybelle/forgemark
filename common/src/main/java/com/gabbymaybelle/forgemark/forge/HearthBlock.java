package com.gabbymaybelle.forgemark.forge;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The hearth: lit like a campfire, it heats one ingot to forging heat. Flint and steel or a fire
 * charge lights it, a shovel puts it out, and it stays as it was left. Right click with an ingot
 * to put it in, with an empty hand to take it out. The ingot's glow is the block entity's business.
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

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		// Lit and put out the way a campfire is: flint and steel or a fire charge, then a shovel.
		if (!state.getValue(LIT) && stack.is(ItemTags.CREEPER_IGNITERS)) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
				boolean charge = stack.is(Items.FIRE_CHARGE);
				level.playSound(null, pos, charge ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				if (charge) {
					stack.consume(1, player);
				} else {
					stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
				}
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide());
		}
		if (state.getValue(LIT) && stack.is(ItemTags.SHOVELS)) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			} else {
				for (int i = 0; i < 12; i++) {
					level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + level.getRandom().nextDouble() * 0.4, pos.getY() + 1.02, pos.getZ() + 0.3 + level.getRandom().nextDouble() * 0.4, 0, 0.05, 0);
				}
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide());
		}
		if (!(level.getBlockEntity(pos) instanceof HearthBlockEntity hearth) || !Metal.isHeatable(stack) || !hearth.getIngot().isEmpty()) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide()) {
			hearth.insert(stack.split(1));
			level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
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
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	/** Drops what the block holds when it's broken or replaced (on 1.21.1 the block does this, not its block entity). */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HearthBlockEntity entity) {
			entity.dropContents(pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
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
		public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
			lines.add(Component.translatable("block.forgemark.hearth.desc").withStyle(ChatFormatting.GRAY));
		}
	}
}
