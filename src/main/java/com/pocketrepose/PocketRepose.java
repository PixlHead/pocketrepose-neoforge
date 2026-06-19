package com.pocketrepose;

import com.pocketrepose.registry.ModBlockEntities;
import com.pocketrepose.registry.ModBlocks;
import com.pocketrepose.registry.ModChunkGenerators;
import com.pocketrepose.registry.ModCreativeTabs;
import com.pocketrepose.registry.ModDataComponents;
import com.pocketrepose.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pocket Repose (NeoForge port / clone).
 *
 * <p>A small mod that adds the Traveler's Suitcase block and the Keystone item, granting access to
 * carriable, "bigger on the inside" pocket dimensions. Each pocket dimension is keyed to the name
 * you give a Keystone in an anvil, so names can be shared (to visit a friend's dimension) or kept
 * private.</p>
 *
 * <p>Dynamic dimension creation at runtime is delegated to Commoble's <b>Infiniverse</b> library,
 * which is the NeoForge analogue of the Fabric Dimension API the original mod relies on. Infiniverse
 * is server-side only and vanilla-client compatible.</p>
 */
@Mod(PocketRepose.MODID)
public final class PocketRepose {
    public static final String MODID = "pocketrepose";
    public static final Logger LOGGER = LoggerFactory.getLogger("Pocket Repose");

    public PocketRepose(IEventBus modBus, ModContainer container) {
        // Deferred registries
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        // Config
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        LOGGER.info("Pocket Repose initialising");
    }

    /** Convenience helper for building mod ResourceLocations. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
