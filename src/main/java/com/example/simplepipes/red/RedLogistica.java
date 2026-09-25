package com.example.simplepipes.red;

import com.example.simplepipes.bloques.ExtractionTubeBlock;
import com.example.simplepipes.bloques.FilterTubeBlock;
import com.example.simplepipes.bloques.TubeBlock;
import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.example.simplepipes.entidades.TubeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Servicio de logística responsable de calcular rutas válidas (BFS) en la red de tuberías.
 */
public class RedLogistica {

    /**
     * Busca una ruta válida desde un tubo inicial hasta un cofre destino que tenga un FilterTube que acepte el ítem.
     */
    public static ResultadoRuta findRoute(Level level, BlockPos startTubePos, BlockPos sourceChestPos, ItemStack itemToTransport) {
        List<ResultadoRuta> routes = findAllRoutes(level, startTubePos, sourceChestPos, itemToTransport);
        return routes.isEmpty() ? null : routes.get(0);
    }

    /**
     * Encuentra TODAS las rutas válidas hacia filtros destinos que acepten el ítem y tengan espacio disponible.
     */
    public static List<ResultadoRuta> findAllRoutes(Level level, BlockPos startTubePos, BlockPos sourceChestPos, ItemStack itemToTransport) {
        Set<BlockPos> networkTubes = getAllConnectedTubes(level, startTubePos, sourceChestPos);

        Queue<PathNode> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        List<ResultadoRuta> routes = new ArrayList<>();

        queue.add(new PathNode(startTubePos, new ArrayList<>(List.of(startTubePos))));
        visited.add(startTubePos);
        visited.add(sourceChestPos);

        Set<BlockPos> claimedChests = new HashSet<>();

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.pos.relative(dir);
                if (neighborPos.equals(sourceChestPos)) continue;

                BlockState neighborState = level.getBlockState(neighborPos);

                // Comprobar si el vecino es un FilterTube conectado a un cofre destino
                if (neighborState.getBlock() instanceof FilterTubeBlock) {
                    Direction facing = neighborState.getValue(FilterTubeBlock.FACING);
                    BlockEntity be = level.getBlockEntity(neighborPos);
                    if (be instanceof FilterTubeBlockEntity filterBE) {
                        BlockPos chestPos = neighborPos.relative(facing);

                        if (!claimedChests.contains(chestPos) && filterBE.matchesFilter(itemToTransport)) {
                            Storage<ItemVariant> targetStorage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                                    level, chestPos, facing.getOpposite()
                            );

                            if (targetStorage != null || level.getBlockEntity(chestPos) instanceof Container) {
                                // Contar ítems en tránsito en TODA la red de tubos hacia este cofre destino
                                int inTransit = getInTransitAmount(level, networkTubes, chestPos, itemToTransport);

                                int realSpaceAvailable = calculateRealInsertSpace(level, chestPos, facing.getOpposite(), targetStorage, itemToTransport);
                                int netSpaceRemaining = realSpaceAvailable - inTransit;

                                if (netSpaceRemaining > 0) {
                                    claimedChests.add(chestPos);

                                    List<BlockPos> fullPath = new ArrayList<>(current.path);
                                    fullPath.add(neighborPos);
                                    routes.add(new ResultadoRuta(chestPos, fullPath, facing.getOpposite(), netSpaceRemaining));
                                }
                            }
                        }
                    }
                }

                // Recorrer tubos de cualquier tipo (TubeBlock, FilterTubeBlock, ExtractionTubeBlock)
                boolean isPipe = neighborState.getBlock() instanceof TubeBlock ||
                                neighborState.getBlock() instanceof FilterTubeBlock ||
                                neighborState.getBlock() instanceof ExtractionTubeBlock;

                if (isPipe && !visited.contains(neighborPos)) {
                    visited.add(neighborPos);
                    List<BlockPos> newPath = new ArrayList<>(current.path);
                    newPath.add(neighborPos);
                    queue.add(new PathNode(neighborPos, newPath));
                }
            }
        }
        return routes;
    }

    private static Set<BlockPos> getAllConnectedTubes(Level level, BlockPos startTubePos, BlockPos sourceChestPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startTubePos);
        visited.add(startTubePos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                if (neighborPos.equals(sourceChestPos) || visited.contains(neighborPos)) continue;

                BlockState state = level.getBlockState(neighborPos);
                if (state.getBlock() instanceof TubeBlock ||
                    state.getBlock() instanceof FilterTubeBlock ||
                    state.getBlock() instanceof ExtractionTubeBlock) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                }
            }
        }
        return visited;
    }

    private static int getInTransitAmount(Level level, Set<BlockPos> networkTubes, BlockPos chestPos, ItemStack itemToTransport) {
        int inTransit = 0;
        for (BlockPos tubePos : networkTubes) {
            BlockEntity be = level.getBlockEntity(tubePos);
            if (be instanceof TubeBlockEntity tubeBE) {
                for (var tItem : tubeBE.getTravelingItems()) {
                    if (tItem.destinationPos.equals(chestPos) && ItemStack.isSameItemSameComponents(tItem.stack, itemToTransport)) {
                        inTransit += tItem.stack.getCount();
                    }
                }
            }
        }
        return inTransit;
    }

    private static int calculateRealInsertSpace(Level level, BlockPos chestPos, Direction insertDirection, Storage<ItemVariant> storage, ItemStack toInsert) {
        Container container = null;
        BlockState state = level.getBlockState(chestPos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            container = ChestBlock.getContainer(chestBlock, state, level, chestPos, true);
        } else {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container c) {
                container = c;
            }
        }

        if (container != null) {
            WorldlyContainer sidedContainer = container instanceof WorldlyContainer ? (WorldlyContainer) container : null;
            int[] slots;
            if (sidedContainer != null) {
                slots = sidedContainer.getSlotsForFace(insertDirection);
            } else {
                slots = new int[container.getContainerSize()];
                for (int i = 0; i < slots.length; i++) {
                    slots[i] = i;
                }
            }

            int totalSpace = 0;
            int maxStack = toInsert.getMaxStackSize();

            for (int slot : slots) {
                ItemStack slotStack = container.getItem(slot);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                    if (container.canPlaceItem(slot, toInsert) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, toInsert, insertDirection))) {
                        int slotMax = Math.min(slotStack.getMaxStackSize(), container.getMaxStackSize());
                        int space = slotMax - slotStack.getCount();
                        if (space > 0) {
                            totalSpace += space;
                        }
                    }
                }
            }

            for (int slot : slots) {
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    if (container.canPlaceItem(slot, toInsert) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, toInsert, insertDirection))) {
                        int slotMax = Math.min(maxStack, container.getMaxStackSize());
                        totalSpace += slotMax;
                    }
                }
            }

            return totalSpace;
        }

        if (storage == null) return 0;

        ItemVariant variant = ItemVariant.of(toInsert);
        int maxStack = toInsert.getMaxStackSize();
        long largeAmount = (long) maxStack * 54;

        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, largeAmount, tx);
            return (int) Math.min(inserted, Integer.MAX_VALUE);
        }
    }

    private static class PathNode {
        final BlockPos pos;
        final List<BlockPos> path;
        PathNode(BlockPos pos, List<BlockPos> path) {
            this.pos = pos;
            this.path = path;
        }
    }
}
