package com.pocketrepose.item;

import com.pocketrepose.registry.ModDataComponents;
import com.pocketrepose.world.PocketDimensionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Keystone. Rename it in an anvil, then right-click in the air to "activate" it: the name becomes
 * a pocket dimension identifier (creating that dimension if it does not exist). Carry the activated
 * Keystone and right-click a Traveler's Suitcase to link them.
 */
public class KeystoneItem extends Item {
    public KeystoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            player.displayClientMessage(
                    Component.translatable("message.pocketrepose.key_needs_name"), true);
            return InteractionResultHolder.fail(stack);
        }

        String raw = stack.get(DataComponents.CUSTOM_NAME).getString();
        String dimName = PocketDimensionManager.sanitize(raw);
        stack.set(ModDataComponents.BOUND_DIMENSION.get(), dimName);

        if (level instanceof ServerLevel serverLevel) {
            // Create the dimension up-front so it exists before the player links a suitcase.
            PocketDimensionManager.getOrCreatePocketLevel(serverLevel.getServer(), dimName);
        }

        player.displayClientMessage(
                Component.translatable("message.pocketrepose.key_activated", dimName), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        String bound = stack.get(ModDataComponents.BOUND_DIMENSION.get());
        return (bound != null && !bound.isEmpty()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String bound = stack.get(ModDataComponents.BOUND_DIMENSION.get());
        if (bound != null && !bound.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.pocketrepose.bound", bound)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.pocketrepose.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
