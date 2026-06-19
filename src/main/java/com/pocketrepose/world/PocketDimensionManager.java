package com.pocketrepose.world;

import com.pocketrepose.registry.ModDimensions;
import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
     * Build the spawn island. When {@code decorative} is true a larger, irregularly shaped floating
     * island is grown, with gentle hills, scattered trees, boulders and ground cover; otherwise a
     * small, safe standing platform is placed. The area around the default entry point is always kept
     * flat and clear so players land safely. Players leave by walking off the edge and dropping into
     * the void, which sends them home (see {@link com.pocketrepose.util.TeleportHelper#tickVoidExit}).
     */
    public static void generateSpawnPlatform(ServerLevel level, boolean decorative) {
        BlockPos entry = PocketReposeState.defaultEntry();
        int groundY = entry.getY() - 1; // y of the top (grass) layer; players stand at groundY + 1

        if (decorative) {
            // Seed from the dimension name so each pocket has its own stable shape, while the same
            // pocket always regenerates identically.
            RandomSource random = RandomSource.create(level.dimension().location().toString().hashCode());
            generateNaturalIsland(level, groundY, random);
        } else {
            generateMinimalPlatform(level, groundY);
        }
    }

    /** A small, flat, always-safe disk for when decorative islands are disabled. */
    private static void generateMinimalPlatform(ServerLevel level, int groundY) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radius = 4; // covers the default entry point (offset 3) with a walking margin
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

    /**
     * Grow a natural-looking floating island centred on the origin: an irregular coastline, gentle
     * hills (kept flat near the centre), a tapered stone underside, plus trees, boulders and plants.
     */
    private static void generateNaturalIsland(ServerLevel level, int groundY, RandomSource random) {
        final double tau = Math.PI * 2.0;
        final int baseRadius = 24;    // rough island radius
        final int coreRadius = 5;     // flat, decoration-free clearing around the spawn/entry point
        final int maxThickness = 12;  // dirt + stone depth at the centre
        final int scan = baseRadius + 6;

        // A cabin sits on a flattened plot off to one side of the spawn clearing.
        final int cabinX = 10, cabinZ = 0, plotRadius = 7;

        // Random phases shape the coastline and the hills.
        double pe1 = random.nextDouble() * tau, pe2 = random.nextDouble() * tau, pe3 = random.nextDouble() * tau;
        double ph1 = random.nextDouble() * tau, ph2 = random.nextDouble() * tau;
        double ph3 = random.nextDouble() * tau, ph4 = random.nextDouble() * tau;

        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();

        int size = scan * 2 + 1;
        int[][] surfaceY = new int[size][size];
        for (int[] row : surfaceY) {
            java.util.Arrays.fill(row, Integer.MIN_VALUE); // MIN_VALUE marks void (no land)
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // ---- terrain ----
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dz = -scan; dz <= scan; dz++) {
                double d = Math.sqrt((double) dx * dx + (double) dz * dz);
                double angle = Math.atan2(dz, dx);
                double edge = baseRadius
                        + 3.0 * Math.sin(angle * 3 + pe1)
                        + 2.0 * Math.sin(angle * 5 + pe2)
                        + 1.4 * Math.sin(angle * 2 + pe3);
                if (d > edge) {
                    continue; // outside the coastline -> void
                }

                // Gentle hills, damped to perfectly flat inside the core clearing and faded back down
                // at the coastline so the rim isn't a sheer wall.
                double hill = 2.0 * Math.sin(dx * 0.16 + ph1) * Math.cos(dz * 0.16 + ph2)
                        + 1.2 * Math.sin(dx * 0.31 + ph3) * Math.cos(dz * 0.28 + ph4);
                double rise = smoothstep(clamp((d - coreRadius) / 4.0, 0.0, 1.0));
                double shore = clamp((edge - d) / 2.0, 0.0, 1.0);
                int height = (int) Math.round(Math.max(0.0, hill) * rise * shore * 1.6);
                int top = groundY + height;

                // Floating-island underside: thick at the centre, tapering to the rim.
                double norm = clamp(d / edge, 0.0, 1.0);
                int thickness = (int) Math.round(2 + (maxThickness - 2) * Math.sqrt(Math.max(0.0, 1.0 - norm * norm)));
                thickness += random.nextInt(2); // jagged underside
                int bottom = top - thickness;

                surfaceY[dx + scan][dz + scan] = top;

                boolean patch = random.nextInt(11) == 0; // occasional bare-dirt patch for texture
                for (int y = top; y >= bottom; y--) {
                    BlockState b;
                    if (y == top) {
                        b = patch ? coarse : grass;
                    } else if (y >= top - 2) {
                        b = dirt;
                    } else {
                        b = random.nextInt(6) == 0 ? andesite : stone;
                    }
                    level.setBlock(pos.set(dx, y, dz), b, Block.UPDATE_CLIENTS);
                }
            }
        }

        // ---- cabin ----
        buildCabin(level, cabinX, groundY, cabinZ, surfaceY, scan);

        // ---- boulders ----
        int boulders = 2 + random.nextInt(3);
        for (int placed = 0, attempts = 0; placed < boulders && attempts < boulders * 12; attempts++) {
            double a = random.nextDouble() * tau;
            double dist = coreRadius + 1 + random.nextDouble() * (baseRadius - coreRadius - 2);
            int bx = (int) Math.round(Math.cos(a) * dist);
            int bz = (int) Math.round(Math.sin(a) * dist);
            if (Math.abs(bx) > scan || Math.abs(bz) > scan) {
                continue;
            }
            int sy = surfaceY[bx + scan][bz + scan];
            if (sy == Integer.MIN_VALUE) {
                continue;
            }
            if ((bx - cabinX) * (bx - cabinX) + (bz - cabinZ) * (bz - cabinZ) < plotRadius * plotRadius) {
                continue; // keep the cabin plot clear
            }
            placeBoulder(level, bx, sy, bz, random);
            placed++;
        }

        // ---- trees ----
        int treeCount = 8 + random.nextInt(7);
        int[] txs = new int[treeCount];
        int[] tzs = new int[treeCount];
        int minTreeRadius = coreRadius + 2;
        for (int placed = 0, attempts = 0; placed < treeCount && attempts < treeCount * 12; attempts++) {
            double a = random.nextDouble() * tau;
            double dist = minTreeRadius + random.nextDouble() * Math.max(1.0, baseRadius - minTreeRadius - 1);
            int tx = (int) Math.round(Math.cos(a) * dist);
            int tz = (int) Math.round(Math.sin(a) * dist);
            if (Math.abs(tx) > scan || Math.abs(tz) > scan) {
                continue;
            }
            int sy = surfaceY[tx + scan][tz + scan];
            if (sy == Integer.MIN_VALUE) {
                continue;
            }
            if ((tx - cabinX) * (tx - cabinX) + (tz - cabinZ) * (tz - cabinZ) < plotRadius * plotRadius) {
                continue; // keep the cabin plot clear
            }
            // Only root trees on open grass (skips boulders, dirt patches and other trunks).
            if (!level.getBlockState(pos.set(tx, sy, tz)).is(Blocks.GRASS_BLOCK)) {
                continue;
            }
            boolean tooClose = false;
            for (int i = 0; i < placed; i++) {
                int ddx = txs[i] - tx, ddz = tzs[i] - tz;
                if (ddx * ddx + ddz * ddz < 9) { // keep trunks at least 3 apart
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }
            boolean birch = random.nextInt(3) == 0;
            int trunk = (birch ? 5 : 4) + random.nextInt(3);
            placeTree(level, new BlockPos(tx, sy + 1, tz),
                    birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG,
                    birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES,
                    trunk, random);
            txs[placed] = tx;
            tzs[placed] = tz;
            placed++;
        }

        // ---- ground cover (grass tufts, ferns, flowers) ----
        BlockState[] flowers = {
                Blocks.POPPY.defaultBlockState(),
                Blocks.DANDELION.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(),
                Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.AZURE_BLUET.defaultBlockState(),
        };
        BlockState shortGrass = Blocks.SHORT_GRASS.defaultBlockState();
        BlockState fern = Blocks.FERN.defaultBlockState();
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dz = -scan; dz <= scan; dz++) {
                int sy = surfaceY[dx + scan][dz + scan];
                if (sy == Integer.MIN_VALUE) {
                    continue;
                }
                if (dx * dx + dz * dz < (coreRadius + 1) * (coreRadius + 1)) {
                    continue; // keep the clearing tidy
                }
                if (!level.getBlockState(pos.set(dx, sy, dz)).is(Blocks.GRASS_BLOCK)) {
                    continue;
                }
                if (!level.getBlockState(pos.set(dx, sy + 1, dz)).isAir()) {
                    continue; // don't bury tree trunks etc.
                }
                int roll = random.nextInt(100);
                BlockState plant;
                if (roll < 24) {
                    plant = shortGrass;
                } else if (roll < 32) {
                    plant = fern;
                } else if (roll < 39) {
                    plant = flowers[random.nextInt(flowers.length)];
                } else {
                    continue;
                }
                level.setBlock(pos.set(dx, sy + 1, dz), plant, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * A small cosy cabin on a flattened plot: oak-framed walls, glass windows, a door, a spruce gable
     * roof, and a few furnishings (bed, chest, crafting table, lantern). Centred on (cx, cz); the door
     * faces west, back toward the spawn clearing.
     */
    private static void buildCabin(ServerLevel level, int cx, int groundY, int cz, int[][] surfaceY, int scan) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState logPost = Blocks.OAK_LOG.defaultBlockState();
        BlockState window = Blocks.GLASS_PANE.defaultBlockState();
        BlockState roofPlanks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState spruceStairs = Blocks.SPRUCE_STAIRS.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        final int half = 3;     // 7x7 footprint (walls at +-3)
        final int wallTop = 3;  // walls span groundY+1 .. groundY+3
        final int pad = half + 1;

        // Flatten the plot to groundY and clear the space above it (any hill, stray plant, etc.).
        for (int dx = -pad; dx <= pad; dx++) {
            for (int dz = -pad; dz <= pad; dz++) {
                int wx = cx + dx, wz = cz + dz;
                for (int y = groundY + 1; y <= groundY + 9; y++) {
                    level.setBlock(pos.set(wx, y, wz), air, Block.UPDATE_CLIENTS);
                }
                boolean footprint = Math.abs(dx) <= half && Math.abs(dz) <= half;
                level.setBlock(pos.set(wx, groundY, wz), footprint ? planks : grass, Block.UPDATE_CLIENTS);
                level.setBlock(pos.set(wx, groundY - 1, wz), dirt, Block.UPDATE_CLIENTS);
                if (Math.abs(wx) <= scan && Math.abs(wz) <= scan) {
                    surfaceY[wx + scan][wz + scan] = groundY; // keep later ground cover at the new level
                }
            }
        }

        // Walls: oak-log corner posts with plank infill.
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (Math.abs(dx) != half && Math.abs(dz) != half) {
                    continue; // interior
                }
                boolean corner = Math.abs(dx) == half && Math.abs(dz) == half;
                for (int y = groundY + 1; y <= groundY + wallTop; y++) {
                    level.setBlock(pos.set(cx + dx, y, cz + dz), corner ? logPost : planks, Block.UPDATE_CLIENTS);
                }
            }
        }

        // Windows on the three non-door walls, at eye level.
        level.setBlock(pos.set(cx + half, groundY + 2, cz), window, Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx, groundY + 2, cz - half), window, Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx, groundY + 2, cz + half), window, Block.UPDATE_CLIENTS);

        // Door in the centre of the west wall, facing out toward the spawn clearing.
        BlockState doorLower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        level.setBlock(pos.set(cx - half, groundY + 1, cz), doorLower, Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx - half, groundY + 2, cz),
                doorLower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
                Block.UPDATE_CLIENTS);

        // Gable end triangles at x = +-half (filled before the roof so the sloped eaves overwrite the
        // outer edge and read as stairs).
        for (int end = -half; end <= half; end += 2 * half) {
            for (int y = groundY + 4; y <= groundY + 7; y++) {
                int hWidth = (groundY + 7) - y; // 3, 2, 1, 0 -> a triangle
                for (int z = -hWidth; z <= hWidth; z++) {
                    level.setBlock(pos.set(cx + end, y, cz + z), roofPlanks, Block.UPDATE_CLIENTS);
                }
            }
        }

        // Gable roof: stairs ascend toward the ridge from both eaves (a 1-block overhang all round).
        BlockState slopeNorth = spruceStairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlockState slopeSouth = spruceStairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        for (int i = 0; i <= 4; i++) {
            int y = groundY + 3 + i;
            int z = 4 - i; // eave at z=+-4 (y=groundY+3) up to the ridge at z=0 (y=groundY+7)
            for (int x = -pad; x <= pad; x++) {
                if (i == 4) {
                    level.setBlock(pos.set(cx + x, y, cz), roofPlanks, Block.UPDATE_CLIENTS); // ridge cap
                } else {
                    level.setBlock(pos.set(cx + x, y, cz + z), slopeNorth, Block.UPDATE_CLIENTS); // +z slope, rises to -z
                    level.setBlock(pos.set(cx + x, y, cz - z), slopeSouth, Block.UPDATE_CLIENTS); // -z slope, rises to +z
                }
            }
        }

        // Furnishings.
        level.setBlock(pos.set(cx + 2, groundY + 1, cz + 2),
                Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx + 2, groundY + 1, cz - 2),
                Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx - 2, groundY + 1, cz + 2),
                Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);

        BlockState bedFoot = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH) // head is to the north of the foot
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        level.setBlock(pos.set(cx - 2, groundY + 1, cz - 1), bedFoot, Block.UPDATE_CLIENTS);
        level.setBlock(pos.set(cx - 2, groundY + 1, cz - 2),
                bedFoot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
    }

    /** A small half-buried rock blob for surface variety. */
    private static void placeBoulder(ServerLevel level, int cx, int groundTopY, int cz, RandomSource random) {
        BlockState[] palette = {
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                Blocks.ANDESITE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
        };
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = 1 + random.nextInt(2); // radius 1..2
        for (int dy = 0; dy <= r; dy++) {
            int rr = r - dy; // narrows toward the top
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr + 1) {
                        continue;
                    }
                    level.setBlock(pos.set(cx + dx, groundTopY + dy, cz + dz),
                            palette[random.nextInt(palette.length)], Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void placeTree(ServerLevel level, BlockPos base, Block logBlock, Block leafBlock,
                                  int trunkHeight, RandomSource random) {
        BlockState log = logBlock.defaultBlockState();
        BlockState leaves = leafBlock.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < trunkHeight; i++) {
            level.setBlock(pos.set(base.getX(), base.getY() + i, base.getZ()), log, Block.UPDATE_CLIENTS);
        }

        int topY = base.getY() + trunkHeight; // one above the highest log
        // Rounded canopy: a wider band low down narrowing to a single leaf at the tip, with a few
        // outer leaves randomly omitted for a ragged, natural silhouette.
        for (int dy = -2; dy <= 1; dy++) {
            int y = topY + dy;
            int r = (dy <= -1) ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && y < topY) {
                        continue; // keep the trunk clear below the tip
                    }
                    if (dx * dx + dz * dz > r * r + 1) {
                        continue; // round the corners
                    }
                    if ((Math.abs(dx) == r || Math.abs(dz) == r) && random.nextInt(4) == 0) {
                        continue; // ragged edge
                    }
                    pos.set(base.getX() + dx, y, base.getZ() + dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, leaves, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }
}
