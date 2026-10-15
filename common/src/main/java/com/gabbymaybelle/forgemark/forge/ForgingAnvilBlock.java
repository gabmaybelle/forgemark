package com.gabbymaybelle.forgemark.forge;

import com.gabbymaybelle.forgemark.ForgemarkTags;
import com.gabbymaybelle.forgemark.quality.Quality;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The forging anvil: holds the item being tempered and the hot ingot, and runs the strike
 * minigame. Right click with a temperable item to lay it down, with the matching hot ingot
 * to add it, with the hammer to strike, and with an empty hand to take things back (the ingot
 * first). Striking happens through {@link #clientStrike}: the client reports where it saw
 * the marker, and the server decides ({@link ForgingAnvilBlockEntity#strike}).
 */
public class ForgingAnvilBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE_NS = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 3, 12, 10, 13), Block.box(3, 10, 0, 13, 16, 16));
	private static final VoxelShape SHAPE_EW = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(3, 4, 4, 13, 10, 12), Block.box(0, 10, 3, 16, 16, 13));

	/**
	 * Set by the client entrypoints: sends the press for the anvil at this position, with the
	 * marker position the player saw. Null on a dedicated server, where it's never called.
	 */
	public static @Nullable Consumer<BlockPos> clientStrike;

	public ForgingAnvilBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof ForgingAnvilBlockEntity anvil)) {
			return InteractionResult.PASS;
		}
		if (stack.is(ForgemarkTags.HAMMERS)) {
			if (!anvil.canStrike()) {
				if (!level.isClientSide()) {
					player.sendOverlayMessage(Component.translatable(anvil.getItem().isEmpty() ? "forgemark.anvil.needs_item" : "forgemark.anvil.needs_ingot"));
				}
				return InteractionResult.SUCCESS;
			}
			// The client reports where it saw the marker; the server waits for that packet.
			if (level.isClientSide() && clientStrike != null) {
				clientStrike.accept(pos);
			}
			return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
		}
		if (anvil.getItem().isEmpty() && Quality.isTemperable(stack)) {
			if (!level.isClientSide()) {
				anvil.placeItem(stack.split(1));
			}
			return InteractionResult.SUCCESS;
		}
		if (!anvil.getItem().isEmpty() && anvil.getIngot().isEmpty() && Metal.isHeatable(stack)) {
			if (!anvil.getItem().isValidRepairItem(stack)) {
				if (!level.isClientSide()) {
					player.sendOverlayMessage(Component.translatable("forgemark.anvil.wrong_ingot", anvil.getItem().getHoverName()));
				}
				return InteractionResult.SUCCESS;
			}
			if (!level.isClientSide()) {
				anvil.placeIngot(stack.split(1));
			}
			return InteractionResult.SUCCESS;
		}
		if (anvil.getItem().isEmpty() && !stack.isEmpty() && !level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("forgemark.anvil.not_temperable", stack.getHoverName()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof ForgingAnvilBlockEntity anvil) || (anvil.getItem().isEmpty() && anvil.getIngot().isEmpty())) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack taken = anvil.getIngot().isEmpty() ? anvil.takeItem() : anvil.takeIngot();
			if (!player.getInventory().add(taken)) {
				Block.popResource(level, pos.above(), taken);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ForgingAnvilBlockEntity(pos, state);
	}

	/** The anvil as an item, with a line of teaching in its tooltip. */
	public static class Item extends BlockItem {
		public Item(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
			consumer.accept(Component.translatable("block.forgemark.forging_anvil.desc").withStyle(ChatFormatting.GRAY));
		}
	}
}
