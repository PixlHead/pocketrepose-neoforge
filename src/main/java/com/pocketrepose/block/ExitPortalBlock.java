package com.pocketrepose.block;

import com.mojang.serialization.MapCodec;
import com.pocketrepose.util.TeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The exit portal that stands on each pocket dimension's spawn island. Walking into it (or
 * right-clicking it) sends the player back to where they entered from.
 */
public class ExitPortalBlock extends Block {
    public static final MapCodec<ExitPortalBlock> CODEC = simpleCodec(ExitPortalBlock::new);

    public ExitPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (entity instanceof ServerPlayer player && !TeleportHelper.isOnCooldown(level, player.getUUID())) {
            TeleportHelper.exit(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TeleportHelper.exit(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
