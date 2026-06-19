package com.pocketrepose.item;

import com.pocketrepose.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/**
 * The item form of the Traveler's Suitcase. Besides placing the block, it can capture a single living
 * entity (right-click a mob) and release it later (right-click a block face). Captured mobs are stored
 * as NBT in the {@link ModDataComponents#CAPTURED_ENTITY} data component so they survive being carried
 * around and dropped.
 */
public class SuitcaseItem extends BlockItem {
    public SuitcaseItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Right-click a living entity to capture it (if the suitcase is empty and the mob is allowed). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity entity, InteractionHand hand) {
        Level level = entity.level();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        // Never capture players or boss mobs.
        if (entity instanceof Player || entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return InteractionResult.PASS;
        }

        // Only one passenger at a time.
        if (stack.has(ModDataComponents.CAPTURED_ENTITY.get())) {
            player.displayClientMessage(
                    Component.translatable("message.pocketrepose.suitcase_full"), true);
            return InteractionResult.CONSUME;
        }

        CompoundTag tag = new CompoundTag();
        if (!entity.save(tag)) {
            // Entity refused to serialize (e.g. is a passenger or otherwise unsaveable).
            return InteractionResult.PASS;
        }

        stack.set(ModDataComponents.CAPTURED_ENTITY.get(), tag);
        player.displayClientMessage(
                Component.translatable("message.pocketrepose.captured", entity.getType().getDescription()),
                true);
        entity.discard();
        return InteractionResult.sidedSuccess(false);
    }

    /**
     * Right-click a block. If the suitcase holds a captured mob, release it at the clicked face;
     * otherwise fall back to normal block placement.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!stack.has(ModDataComponents.CAPTURED_ENTITY.get())) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        CompoundTag tag = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        Optional<Entity> created = EntityType.create(tag, level);
        if (created.isEmpty()) {
            return InteractionResult.FAIL;
        }

        BlockPos releasePos = context.getClickedPos().relative(context.getClickedFace());
        Entity entity = created.get();
        entity.moveTo(releasePos.getX() + 0.5D, releasePos.getY(), releasePos.getZ() + 0.5D,
                entity.getYRot(), entity.getXRot());
        level.addFreshEntity(entity);

        stack.remove(ModDataComponents.CAPTURED_ENTITY.get());

        Player player = context.getPlayer();
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.pocketrepose.released", entity.getType().getDescription()),
                    true);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CAPTURED_ENTITY.get()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        if (tag != null) {
            EntityType.by(tag).ifPresentOrElse(
                    type -> tooltip.add(Component.translatable("tooltip.pocketrepose.holding",
                            type.getDescription()).withStyle(ChatFormatting.AQUA)),
                    () -> tooltip.add(Component.translatable("tooltip.pocketrepose.holding_unknown")
                            .withStyle(ChatFormatting.AQUA)));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
