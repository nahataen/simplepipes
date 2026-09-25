package com.example.simplepipes.bloques;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockItem especializado para el FilterTube.
 * Persiste la whitelist de filtros en el componente CUSTOM_DATA del ItemStack
 * y la muestra en el tooltip.
 */
public class FilterTubeBlockItem extends BlockItem {

    private static final String FILTER_ITEMS_KEY = "FilterItems";

    public FilterTubeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, consumer, tooltipFlag);

        List<ItemStack> filters = getStoredFilters(stack, context.registries());

        if (filters.isEmpty()) {
            consumer.accept(Component.literal("§7Filtro: §cNinguno (rechaza todo)"));
        } else {
            consumer.accept(Component.literal("§7Filtros (§b" + filters.size() + "§7):"));
            for (ItemStack filter : filters) {
                consumer.accept(Component.literal("  §6▸ §f" + filter.getHoverName().getString()));
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static List<ItemStack> getStoredFilters(ItemStack stack, HolderLookup.Provider registries) {
        List<ItemStack> result = new ArrayList<>();
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(FILTER_ITEMS_KEY)) {
            ListTag list = tag.getListOrEmpty(FILTER_ITEMS_KEY);
            for (int i = 0; i < list.size(); i++) {
                var parsed = ItemStack.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), list.get(i));
                if (parsed.result().isPresent() && !parsed.result().get().isEmpty()) {
                    result.add(parsed.result().get());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public static void setStoredFilters(ItemStack stack, List<ItemStack> filters, HolderLookup.Provider registries) {
        var existingCustomData = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        CompoundTag tag = existingCustomData.copyTag();
        ListTag list = new ListTag();
        for (ItemStack filter : filters) {
            if (!filter.isEmpty()) {
                var encoded = ItemStack.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), filter);
                encoded.result().ifPresent(list::add);
            }
        }
        tag.put(FILTER_ITEMS_KEY, list);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
}
