package com.pocketrepose.registry;

import com.pocketrepose.PocketRepose;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Resource keys for the pocket dimension system.
 *
 * <p>Each uniquely-named pocket dimension lives at {@code pocketrepose:pocket/<name>}. The dimension
 * type is shared and defined by the datapack JSON {@code data/pocketrepose/dimension_type/pocket.json}.</p>
 */
public final class ModDimensions {
    /** Shared dimension type for all pocket dimensions (defined in datapack JSON). */
    public static final ResourceKey<DimensionType> POCKET_DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, PocketRepose.id("pocket"));

    private ModDimensions() {}

    /** The {@link Level} key for the pocket dimension with the given sanitized name. */
    public static ResourceKey<Level> levelKey(String sanitizedName) {
        return ResourceKey.create(Registries.DIMENSION, dimensionLocation(sanitizedName));
    }

    /** The {@link LevelStem} key matching a pocket dimension (same location, different registry). */
    public static ResourceKey<LevelStem> stemKey(String sanitizedName) {
        return ResourceKey.create(Registries.LEVEL_STEM, dimensionLocation(sanitizedName));
    }

    /** The {@link ResourceLocation} {@code pocketrepose:pocket/<name>} for a pocket dimension. */
    public static ResourceLocation dimensionLocation(String sanitizedName) {
        return PocketRepose.id("pocket/" + sanitizedName);
    }
}
