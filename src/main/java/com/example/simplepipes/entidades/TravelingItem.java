package com.example.simplepipes.entidades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Representa un ítem que se está desplazando físicamente a través de los tubos del mod.
 * SIN CAMBIOS - esta clase no tiene dependencias de NeoForge.
 */
public class TravelingItem {
    public ItemStack stack;
    public final List<BlockPos> path;
    public int currentPathIndex;
    public float progress; // 0.0 a 1.0
    public final BlockPos destinationPos;
    public final Direction insertDirection;

    public TravelingItem(ItemStack stack, List<BlockPos> path, BlockPos destinationPos, Direction insertDirection) {
        this(stack, path, 0, destinationPos, insertDirection);
    }

    public TravelingItem(ItemStack stack, List<BlockPos> path, int currentPathIndex, BlockPos destinationPos, Direction insertDirection) {
        this.stack = stack;
        this.path = path;
        this.currentPathIndex = currentPathIndex;
        this.progress = 0.0f;
        this.destinationPos = destinationPos;
        this.insertDirection = insertDirection;
    }
}
