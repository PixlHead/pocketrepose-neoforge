package com.pocketrepose.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * A minimal chunk generator that produces an empty (void) world. The spawn island is placed
 * imperatively by {@link PocketDimensionManager} after the level is created, so this generator
 * intentionally writes no blocks.
 *
 * <p>All abstract methods are implemented for Minecraft 1.21.1. Note that, as of 1.21,
 * {@code fillFromNoise} no longer takes an {@link java.util.concurrent.Executor}.</p>
 */
public class PocketChunkGenerator extends ChunkGenerator {

    public static final MapCodec<PocketChunkGenerator> CODEC =
        RecordCodecBuilder.mapCodec(instance ->
            instance
                .group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(
                        ChunkGenerator::getBiomeSource
                    )
                )
                .apply(instance, PocketChunkGenerator::new)
        );

    public PocketChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(
        WorldGenRegion level,
        long seed,
        RandomState random,
        BiomeManager biomeManager,
        StructureManager structureManager,
        ChunkAccess chunk,
        GenerationStep.Carving step
    ) {
        // No carving in a void world.
    }

    @Override
    public void buildSurface(
        WorldGenRegion level,
        StructureManager structureManager,
        RandomState random,
        ChunkAccess chunk
    ) {
        // No surface in a void world.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // No world-gen mob spawns.
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        ChunkAccess chunk
    ) {
        // Leave the chunk empty.
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(
        int x,
        int z,
        Heightmap.Types type,
        LevelHeightAccessor level,
        RandomState random
    ) {
        return getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(
        int x,
        int z,
        LevelHeightAccessor height,
        RandomState random
    ) {
        return new NoiseColumn(
            getMinY(),
            new net.minecraft.world.level.block.state.BlockState[0]
        );
    }

    @Override
    public void addDebugScreenInfo(
        List<String> info,
        RandomState random,
        BlockPos pos
    ) {
        // Nothing to report.
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return 64;
    }
}
