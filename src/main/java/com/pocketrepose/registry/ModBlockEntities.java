package com.pocketrepose.registry;

import com.pocketrepose.PocketRepose;
import com.pocketrepose.block.SuitcaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Block entity types added by Pocket Repose. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PocketRepose.MODID);

    public static final Supplier<BlockEntityType<SuitcaseBlockEntity>> SUITCASE =
            BLOCK_ENTITIES.register("suitcase",
                    () -> BlockEntityType.Builder.of(SuitcaseBlockEntity::new, ModBlocks.SUITCASE.get())
                            .build(null));

    private ModBlockEntities() {}
}
