package com.pocketrepose.registry;

import com.pocketrepose.PocketRepose;
import com.pocketrepose.item.KeystoneItem;
import com.pocketrepose.item.SuitcaseItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All items added by Pocket Repose. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PocketRepose.MODID);

    /** The Keystone ("key"). Rename it in an anvil, activate it, then link a suitcase to it. */
    public static final DeferredItem<KeystoneItem> KEYSTONE = ITEMS.register("keystone",
            () -> new KeystoneItem(new Item.Properties().stacksTo(1)));

    /** Block-item form of the Traveler's Suitcase; also used to capture/release mobs. */
    public static final DeferredItem<SuitcaseItem> SUITCASE = ITEMS.register("suitcase",
            () -> new SuitcaseItem(ModBlocks.SUITCASE.get(), new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
