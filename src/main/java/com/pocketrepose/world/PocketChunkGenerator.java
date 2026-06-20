package com.pocketrepose.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A minimal chunk generator that produces an empty (void) world. The spawn island is placed
 * imperatively by {@link PocketDimensionManager} after the level is created, so this generator
 * intentionally writes no blocks.
 *
 * <p>It also suppresses all worldgen structures and biome decoration: {@link #createStructures} and
 * {@link #createReferences} never start or reference a structure, {@link #applyBiomeDecoration} places
 * no features, and {@link #findNearestMapStructure} locates nothing. Without these, the dimension's
 * biome (plains) would otherwise let villages, outposts and other structures generate in the void.</p>
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
    public void createStructures(
        RegistryAccess registryAccess,
        ChunkGeneratorStructureState structureState,
        StructureManager structureManager,
        ChunkAccess chunk,
        StructureTemplateManager structureTemplateManager
    ) {
        // Void world: never start any worldgen structure (villages, outposts, etc.).
    }

    @Override
    public void createReferences(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkAccess chunk
    ) {
        // No structures exist, so there are no references to record.
    }

    @Override
    public void applyBiomeDecoration(
        WorldGenLevel level,
        ChunkAccess chunk,
        StructureManager structureManager
    ) {
        // No biome features or structure decoration in a void world.
    }

    @Override
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
        ServerLevel level,
        HolderSet<Structure> structures,
        BlockPos pos,
        int searchRadius,
        boolean skipKnownStructures
    ) {
        // Nothing to locate in a void world.
        return null;
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
