package com.pocketrepose.registry;

import com.pocketrepose.PocketRepose;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** A dedicated creative tab holding the Pocket Repose items. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PocketRepose.MODID);

    public static final Supplier<CreativeModeTab> POCKET_REPOSE_TAB = CREATIVE_MODE_TABS.register(
            "pocket_repose",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.pocketrepose"))
                    .icon(() -> new ItemStack(ModItems.SUITCASE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SUITCASE.get());
                        output.accept(ModItems.KEYSTONE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
