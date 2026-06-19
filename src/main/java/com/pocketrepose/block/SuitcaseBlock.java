package com.pocketrepose.block;

import com.mojang.serialization.MapCodec;
import com.pocketrepose.item.KeystoneItem;
import com.pocketrepose.registry.ModDataComponents;
import com.pocketrepose.registry.ModItems;
import com.pocketrepose.util.TeleportHelper;
import com.pocketrepose.world.PocketDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Traveler's Suitcase. Right-click it with an activated Keystone to link it to that key's pocket
 * dimension; then right-click (empty hand or carrying items) to step inside.
 */
public class SuitcaseBlock extends BaseEntityBlock {
    public static final MapCodec<SuitcaseBlock> CODEC = simpleCodec(SuitcaseBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0);

    public SuitcaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(OPEN, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuitcaseBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof KeystoneItem) {
            String bound = stack.get(ModDataComponents.BOUND_DIMENSION.get());
            if (bound == null || bound.isEmpty()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("message.pocketrepose.need_key"), true);
                }
                return ItemInteractionResult.SUCCESS;
            }
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SuitcaseBlockEntity be) {
                be.setLinkedDimension(bound);
                level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
                player.displayClientMessage(
                        Component.translatable("message.pocketrepose.linked", bound), true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        // Any other item (or empty hand) falls through to useWithoutItem so the player can enter.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SuitcaseBlockEntity be) || !be.isLinked()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.pocketrepose.not_linked"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            String dimName = be.getLinkedDimension();
            ServerLevel pocketLevel = PocketDimensionManager.getOrCreatePocketLevel(
                    serverLevel.getServer(), dimName);
            TeleportHelper.enter(serverPlayer, pocketLevel, dimName, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        String linked = stack.get(ModDataComponents.LINKED_DIMENSION.get());
        if (linked != null && !linked.isEmpty()) {
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof SuitcaseBlockEntity be) {
                be.setLinkedDimension(linked);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(ModItems.SUITCASE.get());
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof SuitcaseBlockEntity suitcase && suitcase.getLinkedDimension() != null) {
            stack.set(ModDataComponents.LINKED_DIMENSION.get(), suitcase.getLinkedDimension());
        }
        return List.of(stack);
    }
}
