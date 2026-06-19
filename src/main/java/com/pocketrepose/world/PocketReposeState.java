package com.pocketrepose.world;

import com.pocketrepose.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world persistent state for Pocket Repose, attached to the overworld's data storage.
 *
 * <p>Tracks: every pocket dimension name that has ever been created (so they can be re-registered on
 * server start, since Infiniverse does not persist runtime dimensions); each player's return point;
 * the per-dimension entry point (changeable with a bone); and the runtime-flippable command toggles
 * (seeded from {@link Config}).</p>
 */
public class PocketReposeState extends SavedData {
    private static final String DATA_NAME = "pocketrepose_state";

    /** A position to return a player to when they leave a pocket dimension. */
    public record ReturnPoint(ResourceKey<Level> dimension, BlockPos pos) {}

    private final Set<String> knownDimensions = new HashSet<>();
    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private final Map<String, BlockPos> entryPoints = new HashMap<>();

    private boolean allowRecursion;
    private boolean generateSpawnIsland;

    public PocketReposeState() {
        this.allowRecursion = Config.ALLOW_RECURSION.get();
        this.generateSpawnIsland = Config.GENERATE_SPAWN_ISLAND.get();
    }

    /** The default entry point for a freshly created pocket dimension (offset from the island centre). */
    public static BlockPos defaultEntry() {
        return new BlockPos(0, Config.ENTRY_HEIGHT.get(), 3);
    }

    public static PocketReposeState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new Factory<>(PocketReposeState::new, PocketReposeState::load), DATA_NAME);
    }

    // ---- known dimensions ----

    public Set<String> getKnownDimensions() {
        return knownDimensions;
    }

    public void addKnownDimension(String name) {
        if (knownDimensions.add(name)) {
            setDirty();
        }
    }

    // ---- return points ----

    public void setReturnPoint(UUID player, ResourceKey<Level> dimension, BlockPos pos) {
        returnPoints.put(player, new ReturnPoint(dimension, pos.immutable()));
        setDirty();
    }

    public ReturnPoint getReturnPoint(UUID player) {
        return returnPoints.get(player);
    }

    public void clearReturnPoint(UUID player) {
        if (returnPoints.remove(player) != null) {
            setDirty();
        }
    }

    // ---- entry points ----

    public BlockPos getEntryPoint(String dimensionName) {
        return entryPoints.getOrDefault(dimensionName, defaultEntry());
    }

    public void setEntryPoint(String dimensionName, BlockPos pos) {
        entryPoints.put(dimensionName, pos.immutable());
        setDirty();
    }

    public void resetEntryPoint(String dimensionName) {
        if (entryPoints.remove(dimensionName) != null) {
            setDirty();
        }
    }

    // ---- toggles ----

    public boolean isAllowRecursion() {
        return allowRecursion;
    }

    public void setAllowRecursion(boolean value) {
        this.allowRecursion = value;
        setDirty();
    }

    public boolean isGenerateSpawnIsland() {
        return generateSpawnIsland;
    }

    public void setGenerateSpawnIsland(boolean value) {
        this.generateSpawnIsland = value;
        setDirty();
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag dims = new ListTag();
        for (String name : knownDimensions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", name);
            dims.add(entry);
        }
        tag.put("knownDimensions", dims);

        ListTag returns = new ListTag();
        for (Map.Entry<UUID, ReturnPoint> e : returnPoints.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", e.getKey());
            entry.putString("dimension", e.getValue().dimension().location().toString());
            entry.putInt("x", e.getValue().pos().getX());
            entry.putInt("y", e.getValue().pos().getY());
            entry.putInt("z", e.getValue().pos().getZ());
            returns.add(entry);
        }
        tag.put("returnPoints", returns);

        ListTag entries = new ListTag();
        for (Map.Entry<String, BlockPos> e : entryPoints.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", e.getKey());
            entry.putInt("x", e.getValue().getX());
            entry.putInt("y", e.getValue().getY());
            entry.putInt("z", e.getValue().getZ());
            entries.add(entry);
        }
        tag.put("entryPoints", entries);

        tag.putBoolean("allowRecursion", allowRecursion);
        tag.putBoolean("generateSpawnIsland", generateSpawnIsland);
        return tag;
    }

    public static PocketReposeState load(CompoundTag tag, HolderLookup.Provider registries) {
        PocketReposeState state = new PocketReposeState();
        state.knownDimensions.clear();
        state.returnPoints.clear();
        state.entryPoints.clear();

        ListTag dims = tag.getList("knownDimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dims.size(); i++) {
            state.knownDimensions.add(dims.getCompound(i).getString("name"));
        }

        ListTag returns = tag.getList("returnPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < returns.size(); i++) {
            CompoundTag entry = returns.getCompound(i);
            UUID player = entry.getUUID("player");
            ResourceLocation loc = ResourceLocation.parse(entry.getString("dimension"));
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, loc);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            state.returnPoints.put(player, new ReturnPoint(dim, pos));
        }

        ListTag entries = tag.getList("entryPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            state.entryPoints.put(entry.getString("name"), pos);
        }

        if (tag.contains("allowRecursion")) {
            state.allowRecursion = tag.getBoolean("allowRecursion");
        }
        if (tag.contains("generateSpawnIsland")) {
            state.generateSpawnIsland = tag.getBoolean("generateSpawnIsland");
        }
        return state;
    }
}
