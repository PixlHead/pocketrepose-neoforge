package com.pocketrepose;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration. These values are read once when a world loads and are used to seed the
 * per-world {@link com.pocketrepose.world.PocketReposeState}. The in-game {@code /pocketrepose}
 * commands flip the live values stored in that SavedData, so they persist per-world and override
 * these defaults after first use.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_RECURSION;
    public static final ModConfigSpec.BooleanValue GENERATE_SPAWN_ISLAND;
    public static final ModConfigSpec.IntValue ENTRY_HEIGHT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Pocket Repose configuration (defaults used to seed each world's saved state).")
                .push("general");

        ALLOW_RECURSION = builder
                .comment("If true, suitcases function while you are already inside a pocket dimension",
                        "(letting you nest pocket dimensions inside one another).")
                .define("allowRecursion", true);

        GENERATE_SPAWN_ISLAND = builder
                .comment("If true, newly created pocket dimensions get the prebuilt pocket island",
                        "structure (with its house, ladder and exit portal). If false, a minimal grass",
                        "standing platform is generated instead.")
                .define("generateSpawnIsland", true);

        ENTRY_HEIGHT = builder
                .comment("The Y level of the spawn island top surface inside a pocket dimension.")
                .defineInRange("entryHeight", 64, 0, 256);

        builder.pop();

        SPEC = builder.build();
    }

    private Config() {}
}
