package com.pocketrepose.world;

import com.pocketrepose.registry.ModBlocks;
import com.pocketrepose.registry.ModDimensions;
import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Locale;

/**
 * Creates and manages pocket dimensions. Runtime dimension creation is delegated to Commoble's
 * Infiniverse ({@link InfiniverseAPI#getOrCreateLevel}). Each pocket dimension is a void world with
 * a small spawn island built imperatively the first time it is created.
 */
public final class PocketDimensionManager {

    private PocketDimensionManager() {}

    /**
     * Convert an arbitrary (player-chosen) name into a valid {@link net.minecraft.resources.ResourceLocation}
     * path component. Only {@code [a-z0-9_.-]} survive; everything else is stripped or replaced.
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "unnamed";
        }
        String s = raw.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        String result = sb.toString();
        if (result.length() > 48) {
            result = result.substring(0, 48);
        }
        if (result.isEmpty()) {
            result = "unnamed";
        }
        return result;
    }

    /** True if the given level key belongs to a Pocket Repose pocket dimension. */
    public static boolean isPocketDimension(ResourceKey<Level> key) {
        return key.location().getNamespace().equals(com.pocketrepose.PocketRepose.MODID)
                && key.location().getPath().startsWith("pocket/");
    }

    /**
     * Extract the sanitized pocket-dimension name from a level key, e.g.
     * {@code pocketrepose:pocket/myhome -> "myhome"}. Returns {@code null} if the key is not a pocket
     * dimension.
     */
    public static String nameFromLevelKey(ResourceKey<Level> key) {
        if (!isPocketDimension(key)) {
            return null;
        }
        return key.location().getPath().substring("pocket/".length());
    }

    /**
     * Get the pocket dimension level for the given sanitized name, creating (and populating) it if it
     * does not yet exist.
     */
    public static ServerLevel getOrCreatePocketLevel(MinecraftServer server, String sanitizedName) {
        PocketReposeState state = PocketReposeState.get(server);
        boolean isNew = !state.getKnownDimensions().contains(sanitizedName);

        ResourceKey<Level> key = ModDimensions.levelKey(sanitizedName);
        ServerLevel level = InfiniverseAPI.get().getOrCreateLevel(server, key, () -> createStem(server));

        if (isNew) {
            generateSpawnPlatform(level, state.isGenerateSpawnIsland());
            state.addKnownDimension(sanitizedName);
        }
        return level;
    }

    /** Re-create a known dimension on server start without regenerating its island. */
    public static void registerExisting(MinecraftServer server, String sanitizedName) {
        ResourceKey<Level> key = ModDimensions.levelKey(sanitizedName);
        InfiniverseAPI.get().getOrCreateLevel(server, key, () -> createStem(server));
    }

    private static LevelStem createStem(MinecraftServer server) {
        RegistryAccess registryAccess = server.registryAccess();
        Holder<DimensionType> dimType = registryAccess
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ModDimensions.POCKET_DIMENSION_TYPE);
        Holder<Biome> voidBiome = registryAccess
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.THE_VOID);
        BiomeSource biomeSource = new FixedBiomeSource(voidBiome);
        ChunkGenerator generator = new PocketChunkGenerator(biomeSource);
        return new LevelStem(dimType, generator);
    }

    /**
     * Build the spawn island. A safe standing platform and an exit portal are always placed; the
     * decorative grass disk and tree are only added when {@code decorative} is true.
     */
    public static void generateSpawnPlatform(ServerLevel level, boolean decorative) {
        BlockPos entry = PocketReposeState.defaultEntry();
        int top = entry.getY();          // top surface block coordinate (player stands here)
        int surface = top - 1;           // grass layer
        int centerX = 0;
        int centerZ = 0;

        int radius = decorative ? 5 : 2;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos grassPos = new BlockPos(centerX + dx, surface, centerZ + dz);
                level.setBlock(grassPos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(grassPos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(grassPos.below(2), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        if (decorative) {
            placeTree(level, new BlockPos(centerX - 3, top, centerZ - 3));
        }

        // Exit portal at the centre; entry point is offset (see defaultEntry) so the player doesn't
        // immediately walk back into it.
        level.setBlock(new BlockPos(centerX, top, centerZ),
                ModBlocks.EXIT_PORTAL.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void placeTree(ServerLevel level, BlockPos base) {
        int trunkHeight = 5;
        for (int i = 0; i < trunkHeight; i++) {
            level.setBlock(base.above(i), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        var leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        // A small canopy around the top of the trunk.
        for (int dy = trunkHeight - 2; dy <= trunkHeight; dy++) {
            int r = (dy == trunkHeight) ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy < trunkHeight) {
                        continue; // keep the trunk
                    }
                    BlockPos leafPos = base.offset(dx, dy, dz);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, leaves, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
