package com.example.simplepipes.bloques;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class that centralises the logic for determining whether tube blocks can connect to
 * neighbouring blocks or inventories. This removes duplicated code from {@link TubeBlock},
 * {@link FilterTubeBlock} and {@link ExtractionTubeBlock}.
 *
 * The methods are deliberately static and contain no mutable state – they are pure helpers.
 */
public final class TubeConnectionHelper {
    private TubeConnectionHelper() {}

    /**
     * Used by {@link TubeBlock}. Returns true if the neighbour is a tube block of any kind,
     * or if a Fabric Transfer API storage capability (or vanilla chest/barrel/shulker) exists at the
     * neighbour position.
     */
    public static boolean canConnectToAny(LevelReader level, BlockPos neighborPos, Direction direction) {
        BlockState state = level.getBlockState(neighborPos);
        if (state.getBlock() instanceof TubeBlock ||
            state.getBlock() instanceof FilterTubeBlock ||
            state.getBlock() instanceof ExtractionTubeBlock ||
            state.getBlock() instanceof ExplorerChestBlock) {
            return true;
        }
        // For plain TubeBlock we check storage regardless of direction.
        if (level instanceof Level realLevel) {
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    realLevel,
                    neighborPos,
                    direction.getOpposite()
            );
            return storage != null;
        }
        return false;
    }

    /**
     * Used by {@link FilterTubeBlock} and {@link ExtractionTubeBlock}. The connection is only
     * considered when the checked direction matches the tube's facing direction; otherwise the
     * method returns false for non‑tube neighbours.
     */
    public static boolean canConnectTo(LevelReader level, BlockPos neighborPos, Direction direction, Direction tubeFacing) {
        BlockState state = level.getBlockState(neighborPos);
        if (state.getBlock() instanceof TubeBlock ||
            state.getBlock() instanceof FilterTubeBlock ||
            state.getBlock() instanceof ExtractionTubeBlock ||
            state.getBlock() instanceof ExplorerChestBlock) {
            return true;
        }
        // Only attempt to access a storage capability if we are looking at the side the tube faces.
        if (direction == tubeFacing && level instanceof Level realLevel) {
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    realLevel,
                    neighborPos,
                    direction.getOpposite()
            );
            return storage != null;
        }
        return false;
    }

    /**
     * Helper that determines whether a block position contains an inventory (chest, barrel, shulker)
     * or a Fabric Transfer API storage capability. This replaces the duplicated logic formerly in
     * {@link FilterTubeBlock#isChestAt} and {@link ExtractionTubeBlock#isChestAt} as well as the
     * whitelist‑facing check in {@link FilterTubeBlock#isFacingValidChest}.
     */
    public static boolean isChest(LevelReader level, BlockPos pos, Direction fromDirection) {
        if (level instanceof Level realLevel) {
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    realLevel,
                    pos,
                    fromDirection.getOpposite()
            );
            return storage != null;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock ||
               state.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock ||
               state.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
    }
}
