package com.pocketrepose.registry;

import com.mojang.serialization.Codec;
import com.pocketrepose.PocketRepose;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Data components attached to our items.
 *
 * <ul>
 *   <li>{@link #BOUND_DIMENSION} – on an activated Keystone: the sanitized pocket dimension name.</li>
 *   <li>{@link #LINKED_DIMENSION} – on a Traveler's Suitcase (item form): the dimension it links to,
 *       so the link survives being broken and re-placed.</li>
 *   <li>{@link #CAPTURED_ENTITY} – on a Suitcase item: NBT of a captured mob, if any.</li>
 * </ul>
 */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PocketRepose.MODID);

    public static final Supplier<DataComponentType<String>> BOUND_DIMENSION =
            register("bound_dimension", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final Supplier<DataComponentType<String>> LINKED_DIMENSION =
            register("linked_dimension", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final Supplier<DataComponentType<CompoundTag>> CAPTURED_ENTITY =
            register("captured_entity", builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    private static <T> Supplier<DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return DATA_COMPONENTS.register(name,
                () -> builder.apply(DataComponentType.builder()).build());
    }

    private ModDataComponents() {}
}
