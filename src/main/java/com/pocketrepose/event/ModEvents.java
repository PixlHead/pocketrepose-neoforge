package com.pocketrepose.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pocketrepose.PocketRepose;
import com.pocketrepose.util.TeleportHelper;
import com.pocketrepose.world.PocketDimensionManager;
import com.pocketrepose.world.PocketReposeState;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Game-bus event handlers: command registration, dimension re-registration on startup, mob-spawn
 * suppression inside pocket dimensions, and the bone "set entry point" interaction.
 */
@EventBusSubscriber(modid = PocketRepose.MODID)
public final class ModEvents {

    private ModEvents() {}

    /** Send a player who has jumped into the void below a pocket dimension back to the suitcase. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportHelper.tickVoidExit(player);
        }
    }

    /** Never let the void hurt a player inside a pocket dimension — they fall through it to leave. */
    @SubscribeEvent
    public static void onVoidDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                && PocketDimensionManager.isPocketDimension(player.level().dimension())) {
            event.setCanceled(true);
        }
    }

    /** Re-create every known pocket dimension so players who log out inside one return correctly. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        PocketReposeState state = PocketReposeState.get(server);
        for (String name : state.getKnownDimensions()) {
            PocketDimensionManager.registerExisting(server, name);
        }
    }

    /** Suppress automatic mob spawning inside pocket dimensions (the islands are safe spaces). */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION
                && type != MobSpawnType.PATROL) {
            return; // allow intentional spawns (spawn eggs, released mobs, breeding, etc.)
        }
        if (PocketDimensionManager.isPocketDimension(event.getLevel().getLevel().dimension())) {
            event.setSpawnCancelled(true);
        }
    }

    /** Right-click the ground with a bone inside a pocket dimension to set its entry point. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BONE)) {
            return;
        }
        String dimName = PocketDimensionManager.nameFromLevelKey(level.dimension());
        if (dimName == null) {
            return;
        }

        Player player = event.getEntity();
        BlockPos entry = event.getPos().above();
        PocketReposeState.get(level.getServer()).setEntryPoint(dimName, entry);
        player.displayClientMessage(Component.translatable("message.pocketrepose.entry_set"), true);

        // Don't consume the bone or place anything.
        event.setCanceled(true);
    }

    /** Register the {@code /pocketrepose} command tree. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pocketrepose")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("allowRecursion")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    MinecraftServer server = ctx.getSource().getServer();
                                    PocketReposeState.get(server).setAllowRecursion(value);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.pocketrepose.allow_recursion", value), true);
                                    return 1;
                                })))
                .then(Commands.literal("spawnIsland")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    MinecraftServer server = ctx.getSource().getServer();
                                    PocketReposeState.get(server).setGenerateSpawnIsland(value);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.pocketrepose.spawn_island", value), true);
                                    return 1;
                                })))
                .then(Commands.literal("resetplayerentry")
                        .then(Commands.argument("dimensionName", StringArgumentType.word())
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "dimensionName");
                                    String name = PocketDimensionManager.sanitize(raw);
                                    MinecraftServer server = ctx.getSource().getServer();
                                    PocketReposeState.get(server).resetEntryPoint(name);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.pocketrepose.reset_entry", name), true);
                                    return 1;
                                }))));
    }
}
