package com.pocketrepose.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Optional integration with Sable, the physics engine behind Create: Aeronautics. A suitcase can be
 * placed on a physics object (an airship/contraption); Sable stores those blocks in an internal
 * "sublevel" whose coordinates sit millions of blocks away from the real world. Force-loading or
 * teleporting to such a position crashes Sable ("Cannot change blocks in nonexistent plot holder"),
 * so when a player leaves a pocket dimension we use this class to (a) recognise that their suitcase
 * lives inside a physics object and (b) resolve that object's current real-world location.
 *
 * <p>All access is reflective and fully guarded: if Sable is not installed (or its API differs) the
 * methods report "not a physics object" / return {@code null} and the caller falls back to its normal
 * behaviour. Nothing here references a Sable class at link time, so the mod loads fine without Sable
 * present.</p>
 */
public final class SableCompat {
    /**
     * A resolved world position farther than this from the origin is rejected. Real ships sit at
     * ordinary coordinates; a value this large means we accidentally read sublevel space, so we bail
     * out to a safe fallback rather than teleport the player into the crashing region.
     */
    private static final double MAX_REAL_WORLD_COORD = 1_000_000.0;

    private static final boolean AVAILABLE;
    private static final Method GET_CONTAINER;
    private static final Method IN_BOUNDS;
    private static final Method GET_PLOT;
    private static final Method PLOT_GET_SUBLEVEL;
    private static final Method SUBLEVEL_BOUNDING_BOX;
    private static final Method BB_MIN_X, BB_MAX_X, BB_MIN_Z, BB_MAX_Z, BB_MAX_Y;

    static {
        boolean available = false;
        Method getContainer = null, inBounds = null, getPlot = null, plotGetSubLevel = null,
                subLevelBoundingBox = null, minX = null, maxX = null, minZ = null, maxZ = null, maxY = null;
        try {
            Class<?> container = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> plot = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
            Class<?> subLevelAccess = Class.forName("dev.ryanhcode.sable.companion.SubLevelAccess");
            Class<?> boundingBox = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3dc");

            getContainer = container.getMethod("getContainer", ServerLevel.class);
            inBounds = container.getMethod("inBounds", BlockPos.class);
            getPlot = container.getMethod("getPlot", ChunkPos.class);
            plotGetSubLevel = plot.getMethod("getSubLevel");
            subLevelBoundingBox = subLevelAccess.getMethod("boundingBox");
            minX = boundingBox.getMethod("minX");
            maxX = boundingBox.getMethod("maxX");
            minZ = boundingBox.getMethod("minZ");
            maxZ = boundingBox.getMethod("maxZ");
            maxY = boundingBox.getMethod("maxY");
            available = true;
        } catch (Throwable ignored) {
            // Sable absent or its API changed — integration disabled, callers fall back gracefully.
        }

        AVAILABLE = available;
        GET_CONTAINER = getContainer;
        IN_BOUNDS = inBounds;
        GET_PLOT = getPlot;
        PLOT_GET_SUBLEVEL = plotGetSubLevel;
        SUBLEVEL_BOUNDING_BOX = subLevelBoundingBox;
        BB_MIN_X = minX;
        BB_MAX_X = maxX;
        BB_MIN_Z = minZ;
        BB_MAX_Z = maxZ;
        BB_MAX_Y = maxY;
    }

    private SableCompat() {}

    /** True if Sable is present and the given position lies inside a physics object's sublevel. */
    public static boolean isPhysicsObjectPos(ServerLevel level, BlockPos pos) {
        if (!AVAILABLE) {
            return false;
        }
        try {
            Object container = GET_CONTAINER.invoke(null, level);
            return container != null && (Boolean) IN_BOUNDS.invoke(container, pos);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * The real-world position to drop a player at when their suitcase is attached to a physics
     * object: just above the centre of the object's current world-space bounding box. Returns
     * {@code null} if Sable is absent, the position isn't in a physics object, the object isn't
     * currently loaded, or anything cannot be resolved.
     */
    public static Vec3 physicsObjectReturnPos(ServerLevel level, BlockPos pos) {
        if (!AVAILABLE) {
            return null;
        }
        try {
            Object container = GET_CONTAINER.invoke(null, level);
            if (container == null || !(Boolean) IN_BOUNDS.invoke(container, pos)) {
                return null;
            }
            Object plot = GET_PLOT.invoke(container, new ChunkPos(pos));
            if (plot == null) {
                return null;
            }
            Object subLevel = PLOT_GET_SUBLEVEL.invoke(plot);
            if (subLevel == null) {
                return null;
            }
            Object box = SUBLEVEL_BOUNDING_BOX.invoke(subLevel);
            if (box == null) {
                return null;
            }
            double x = ((Double) BB_MIN_X.invoke(box) + (Double) BB_MAX_X.invoke(box)) / 2.0;
            double z = ((Double) BB_MIN_Z.invoke(box) + (Double) BB_MAX_Z.invoke(box)) / 2.0;
            double y = (Double) BB_MAX_Y.invoke(box) + 1.0; // just above the highest block
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > MAX_REAL_WORLD_COORD || Math.abs(z) > MAX_REAL_WORLD_COORD) {
                return null; // not a sane world position — refuse rather than risk a crash
            }
            return new Vec3(x, y, z);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
