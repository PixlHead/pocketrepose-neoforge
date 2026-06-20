package com.pocketrepose.world;

import com.pocketrepose.registry.ModDimensions;
import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Locale;

/**
 * Creates and manages pocket dimensions. Each pocket is a void world (so the island is a single,
 * bounded landmass floating in the void) generated with a custom {@link PocketChunkGenerator}.
 *
 * <p>When a dimension is first created the spawn island is placed from the prebuilt
 * {@code pocket_island_01} structure — the same island the original Pocket Repose mod ships — anchored
 * so the player's entry point lands on its spawn floor (the foot of the ladder leading up to the exit
 * portal). If the spawn-island toggle is off, a minimal grass platform is placed instead.</p>
 */
public final class PocketDimensionManager {

    /** Biome used across the island (affects grass/sky colour and ambience only; terrain is built by us). */
    private static final ResourceKey<Biome> POCKET_BIOME = Biomes.PLAINS;

    /** The prebuilt spawn-island structure, ported verbatim from the original Pocket Repose mod. */
    private static final ResourceLocation ISLAND_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(com.pocketrepose.PocketRepose.MODID, "pocket_island_01");

    /**
     * Where the player stands inside {@link #ISLAND_STRUCTURE}, as an offset (in blocks) from the
     * structure's (0,0,0) corner: the oak-plank floor at the foot of the ladder — the exact spot the
     * original mod spawned players. The island is placed so this lands on the dimension's entry point.
     */
    private static final int ISLAND_SPAWN_OFFSET_X = 17;
    private static final int ISLAND_SPAWN_OFFSET_Y = 33;
    private static final int ISLAND_SPAWN_OFFSET_Z = 9;

    private PocketDimensionManager() {}

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

    public static boolean isPocketDimension(ResourceKey<Level> key) {
        return key.location().getNamespace().equals(com.pocketrepose.PocketRepose.MODID)
                && key.location().getPath().startsWith("pocket/");
    }

    public static String nameFromLevelKey(ResourceKey<Level> key) {
        if (!isPocketDimension(key)) {
            return null;
        }
        return key.location().getPath().substring("pocket/".length());
    }

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

    public static void registerExisting(MinecraftServer server, String sanitizedName) {
        ResourceKey<Level> key = ModDimensions.levelKey(sanitizedName);
        InfiniverseAPI.get().getOrCreateLevel(server, key, () -> createStem(server));
    }

    private static LevelStem createStem(MinecraftServer server) {
        RegistryAccess registryAccess = server.registryAccess();
        Holder<DimensionType> dimType = registryAccess
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ModDimensions.POCKET_DIMENSION_TYPE);
        Holder<Biome> biome = registryAccess
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(POCKET_BIOME);
        BiomeSource biomeSource = new FixedBiomeSource(biome);
        ChunkGenerator generator = new PocketChunkGenerator(biomeSource);
        return new LevelStem(dimType, generator);
    }

    public static void generateSpawnPlatform(ServerLevel level, boolean decorative) {
        BlockPos entry = PocketReposeState.defaultEntry();
        if (decorative) {
            placeIslandStructure(level, entry);
        } else {
            // top (grass) layer; players stand at groundY + 1
            generateMinimalPlatform(level, entry.getY() - 1);
        }
    }

    /**
     * Place the prebuilt pocket-island structure so the player's {@code entry} point lands on its spawn
     * floor. Falls back to a minimal platform if the structure template can't be loaded, so the player
     * always has somewhere safe to stand.
     */
    private static void placeIslandStructure(ServerLevel level, BlockPos entry) {
        StructureTemplateManager templates = level.getServer().getStructureManager();
        StructureTemplate template = templates.get(ISLAND_STRUCTURE).orElse(null);
        if (template == null) {
            generateMinimalPlatform(level, entry.getY() - 1);
            return;
        }

        // Offset the structure so its spawn floor sits under the entry point.
        BlockPos corner = new BlockPos(
                entry.getX() - ISLAND_SPAWN_OFFSET_X,
                entry.getY() - ISLAND_SPAWN_OFFSET_Y,
                entry.getZ() - ISLAND_SPAWN_OFFSET_Z);

        // Load every chunk the structure touches before writing to it.
        Vec3i size = template.getSize();
        int minChunkX = corner.getX() >> 4;
        int minChunkZ = corner.getZ() >> 4;
        int maxChunkX = (corner.getX() + size.getX() - 1) >> 4;
        int maxChunkZ = (corner.getZ() + size.getZ() - 1) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunk(cx, cz);
            }
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);
        template.placeInWorld(level, corner, corner, settings, level.getRandom(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static void generateMinimalPlatform(ServerLevel level, int groundY) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                level.setBlock(pos.set(dx, groundY, dz), grass, Block.UPDATE_CLIENTS);
                level.setBlock(pos.set(dx, groundY - 1, dz), dirt, Block.UPDATE_CLIENTS);
                level.setBlock(pos.set(dx, groundY - 2, dz), dirt, Block.UPDATE_CLIENTS);
            }
        }
    }
}
