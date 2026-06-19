package com.pocketrepose.util;

import com.pocketrepose.block.SuitcaseBlockEntity;
import com.pocketrepose.world.PocketDimensionManager;
import com.pocketrepose.world.PocketReposeState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles moving players into and out of pocket dimensions. */
public final class TeleportHelper {
    /** Ticks during which a freshly-teleported player will not be auto-exited by the portal. */
    private static final int TELEPORT_COOLDOWN_TICKS = 40;
    /** How far from the recorded return position we search for the linked suitcase. */
    private static final int SUITCASE_SEARCH_RADIUS = 6;
    /**
     * A player who falls this many blocks below the pocket dimension's floor is treated as having
     * jumped into the void and is sent home. Kept well above the depth where vanilla deals
     * out-of-world damage (floor - 64), so the player is caught long before anything can hurt them.
     */
    private static final int VOID_EXIT_DROP = 8;

    private static final Map<UUID, Long> lastTeleportTime = new HashMap<>();

    private TeleportHelper() {}

    /**
     * Send a player into a pocket dimension.
     *
     * @param suitcasePos the position of the suitcase block they entered from (used so they can be
     *                    returned to its location, even if a friend moves it while they are inside).
     * @return true if the player entered; false if entry was denied (e.g. recursion disabled).
     */
    public static boolean enter(ServerPlayer player, ServerLevel pocketLevel, String dimName, BlockPos suitcasePos) {
        MinecraftServer server = player.server;
        PocketReposeState state = PocketReposeState.get(server);

        if (PocketDimensionManager.isPocketDimension(player.level().dimension()) && !state.isAllowRecursion()) {
            player.displayClientMessage(
                    Component.translatable("message.pocketrepose.recursion_disabled"), true);
            return false;
        }

        // Remember where to send the player back to.
        state.setReturnPoint(player.getUUID(), player.level().dimension(), suitcasePos);

        BlockPos entry = state.getEntryPoint(dimName);
        // Synchronously load the destination chunk before the teleport.
        pocketLevel.getChunk(entry.getX() >> 4, entry.getZ() >> 4);

        setCooldown(player);
        player.teleportTo(pocketLevel,
                entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        return true;
    }

    /** Send a player back out of the pocket dimension they are currently in. */
    public static void exit(ServerPlayer player) {
        MinecraftServer server = player.server;
        PocketReposeState state = PocketReposeState.get(server);

        String currentPath = player.level().dimension().location().getPath();
        String dimName = currentPath.startsWith("pocket/")
                ? currentPath.substring("pocket/".length())
                : currentPath;

        PocketReposeState.ReturnPoint ret = state.getReturnPoint(player.getUUID());
        ServerLevel targetLevel = ret != null ? server.getLevel(ret.dimension()) : null;

        if (ret == null || targetLevel == null) {
            // Fallback: overworld spawn.
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            setCooldown(player);
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            player.resetFallDistance();
            state.clearReturnPoint(player.getUUID());
            return;
        }

        // Make sure the area is loaded so we can look for the suitcase.
        targetLevel.getChunk(ret.pos().getX() >> 4, ret.pos().getZ() >> 4);

        BlockPos suitcase = findLinkedSuitcase(targetLevel, ret.pos(), dimName, SUITCASE_SEARCH_RADIUS);
        BlockPos dest = suitcase != null ? suitcase.above() : ret.pos();

        setCooldown(player);
        player.teleportTo(targetLevel, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.resetFallDistance();
        state.clearReturnPoint(player.getUUID());
    }

    /**
     * Called every server tick for each player. If the player has jumped off their pocket
     * dimension's island and fallen into the void below, send them back out to the suitcase they
     * entered from. They are caught well above the depth where the void would damage them, so the
     * trip home is harmless.
     */
    public static void tickVoidExit(ServerPlayer player) {
        if (!PocketDimensionManager.isPocketDimension(player.level().dimension())) {
            return;
        }
        if (player.getY() < player.level().getMinBuildHeight() - VOID_EXIT_DROP) {
            exit(player);
        }
    }

    private static BlockPos findLinkedSuitcase(ServerLevel level, BlockPos center, String dimName, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof SuitcaseBlockEntity suitcase
                            && dimName.equals(suitcase.getLinkedDimension())) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    public static boolean isOnCooldown(Level level, UUID player) {
        Long last = lastTeleportTime.get(player);
        if (last == null) {
            return false;
        }
        return level.getGameTime() - last < TELEPORT_COOLDOWN_TICKS;
    }

    private static void setCooldown(ServerPlayer player) {
        lastTeleportTime.put(player.getUUID(), player.level().getGameTime());
    }
}
