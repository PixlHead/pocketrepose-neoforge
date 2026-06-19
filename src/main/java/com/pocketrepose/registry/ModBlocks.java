package com.pocketrepose.registry;

import com.pocketrepose.PocketRepose;
import com.pocketrepose.block.ExitPortalBlock;
import com.pocketrepose.block.SuitcaseBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All blocks added by Pocket Repose. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PocketRepose.MODID);

    public static final DeferredBlock<SuitcaseBlock> SUITCASE = BLOCKS.register("suitcase",
            () -> new SuitcaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noLootTable()));

    /**
     * The exit portal that stands inside each pocket dimension. It has no collision so the player can
     * walk into it, and stepping inside (or right-clicking it) sends them back out.
     */
    public static final DeferredBlock<ExitPortalBlock> EXIT_PORTAL = BLOCKS.register("exit_portal",
            () -> new ExitPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3600000.0f) // unbreakable in survival
                    .lightLevel(state -> 11)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
