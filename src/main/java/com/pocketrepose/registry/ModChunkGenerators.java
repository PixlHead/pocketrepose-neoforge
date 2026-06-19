package com.pocketrepose.registry;

import com.mojang.serialization.MapCodec;
import com.pocketrepose.PocketRepose;
import com.pocketrepose.world.PocketChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers our custom void chunk generator's codec. */
public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, PocketRepose.MODID);

    public static final Supplier<MapCodec<PocketChunkGenerator>> POCKET =
            CHUNK_GENERATORS.register("pocket", () -> PocketChunkGenerator.CODEC);

    private ModChunkGenerators() {}
}
