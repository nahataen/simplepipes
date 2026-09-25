package com.example.simplepipes.entidades;

import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de bloque para el tubo de filtro de destino.
 * Almacena la lista de items aceptados (whitelist) para el cofre adjunto.
 */
public class FilterTubeBlockEntity extends TubeBlockEntity {

    private final List<ItemStack> filterItems = new ArrayList<>();

    public FilterTubeBlockEntity(BlockPos pos, BlockState state) {
        super(BloquesMod.FILTER_TUBE_BE, pos, state);
    }

    public List<ItemStack> getFilterItems() {
        return filterItems;
    }

    public boolean toggleFilterItem(ItemStack item) {
        for (int i = 0; i < filterItems.size(); i++) {
            if (ItemStack.isSameItemSameComponents(filterItems.get(i), item)) {
                filterItems.remove(i);
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                return false;
            }
        }
        filterItems.add(item.copy());
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public void clearFilters() {
        filterItems.clear();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean matchesFilter(ItemStack stack) {
        if (filterItems.isEmpty()) return false; // Un filtro sin ítems configurados no acepta ningún objeto por defecto
        for (ItemStack filter : filterItems) {
            if (ItemStack.isSameItemSameComponents(filter, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var listOutput = output.list("filterItems", ItemStack.CODEC);
        for (ItemStack filter : filterItems) {
            if (!filter.isEmpty()) {
                listOutput.add(filter);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filterItems.clear();
        for (ItemStack stack : input.listOrEmpty("filterItems", ItemStack.CODEC)) {
            if (!stack.isEmpty()) {
                filterItems.add(stack);
            }
        }
    }
}
