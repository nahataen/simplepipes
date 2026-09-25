package com.example.simplepipes.red;

import com.example.simplepipes.bloques.ExtractionTubeBlock;
import com.example.simplepipes.bloques.FilterTubeBlock;
import com.example.simplepipes.bloques.TubeBlock;
import com.example.simplepipes.bloques.ExplorerChestBlock;
import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
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
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Servicio auxiliar para consultar y manipular inventarios reales conectados
 * en la red de tuberías de un Cofre Explorador.
 * Escanea y detecta TODOS los ítems y contenedores de la red sin omisiones.
 */
public class ExplorerNetworkHelper {

    public record NetworkContainerInfo(BlockPos containerPos, Direction insertDirection, @Nullable FilterTubeBlockEntity filterBE) {}

    /**
     * Recorre toda la red de tuberías conectada a startPos y encuentra TODOS los contenedores/inventarios asociados.
     */
    public static List<NetworkContainerInfo> getNetworkContainers(Level level, BlockPos startPos) {
        Set<BlockPos> visitedPipes = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        List<NetworkContainerInfo> containerInfos = new ArrayList<>();
        Set<BlockPos> claimedPos = new HashSet<>();

        queue.add(startPos);
        visitedPipes.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (isPipeOrExplorerChest(neighborState)) {
                    if (visitedPipes.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else {
                    if (claimedPos.contains(neighborPos)) continue;

                    FilterTubeBlockEntity filterBE = null;
                    BlockState currentState = level.getBlockState(currentPos);
                    if (currentState.getBlock() instanceof FilterTubeBlock) {
                        Direction filterFacing = currentState.getValue(FilterTubeBlock.FACING);
                        if (filterFacing != dir) {
                            continue;
                        }
                        BlockEntity be = level.getBlockEntity(currentPos);
                        if (be instanceof FilterTubeBlockEntity fbe) {
                            filterBE = fbe;
                        }
                    }

                    if (isContainerOrStorage(level, neighborPos, dir.getOpposite())) {
                        claimedPos.add(neighborPos);
                        BlockPos otherHalf = getOtherChestHalf(neighborState, neighborPos);
                        if (otherHalf != null) {
                            claimedPos.add(otherHalf);
                        }
                        containerInfos.add(new NetworkContainerInfo(neighborPos, dir.getOpposite(), filterBE));
                    }
                }
            }
        }

        return containerInfos;
    }

    private static boolean isPipeOrExplorerChest(BlockState state) {
        return state.getBlock() instanceof TubeBlock ||
               state.getBlock() instanceof FilterTubeBlock ||
               state.getBlock() instanceof ExtractionTubeBlock ||
               state.getBlock() instanceof ExplorerChestBlock;
    }

    private static boolean isContainerOrStorage(Level level, BlockPos pos, Direction side) {
        if (getContainer(level, pos) != null) {
            return true;
        }
        Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, pos, side);
        return storage != null;
    }

    private static @Nullable BlockPos getOtherChestHalf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                Direction connectedDir = ChestBlock.getConnectedDirection(state);
                return pos.relative(connectedDir);
            }
        }
        return null;
    }

    /**
     * Obtiene el resumen de TODOS los ítems disponibles en todos los contenedores conectados a la red.
     */
    public static Map<ItemVariant, Long> getNetworkItemSummary(Level level, BlockPos startPos) {
        List<NetworkContainerInfo> containers = getNetworkContainers(level, startPos);
        Map<ItemVariant, Long> summary = new HashMap<>();

        for (NetworkContainerInfo info : containers) {
            readChestContents(level, info, summary);
        }

        return summary;
    }

    private static void readChestContents(Level level, NetworkContainerInfo info, Map<ItemVariant, Long> summary) {
        Container container = getContainer(level, info.containerPos());
        if (container != null) {
            WorldlyContainer sidedContainer = container instanceof WorldlyContainer ? (WorldlyContainer) container : null;
            int[] slots = sidedContainer != null ? sidedContainer.getSlotsForFace(info.insertDirection()) : null;
            int size = slots != null ? slots.length : container.getContainerSize();

            for (int i = 0; i < size; i++) {
                int slot = slots != null ? slots[i] : i;
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    ItemVariant variant = ItemVariant.of(stack);
                    summary.merge(variant, (long) stack.getCount(), Long::sum);
                }
            }
            return;
        }

        Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                level, info.containerPos(), info.insertDirection()
        );

        if (storage != null) {
            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank()) continue;
                long amount = view.getAmount();
                if (amount <= 0) continue;

                ItemVariant variant = view.getResource();
                summary.merge(variant, amount, Long::sum);
            }
        }
    }

    /**
     * Extrae de forma física la cantidad solicitada de un ítem recorriendo los contenedores conectados de la red.
     */
    public static long extractFromNetwork(Level level, BlockPos startPos, ItemVariant variant, long amountRequested) {
        if (amountRequested <= 0) return 0;
        List<NetworkContainerInfo> containers = getNetworkContainers(level, startPos);
        long stillToExtract = amountRequested;

        for (NetworkContainerInfo info : containers) {
            if (stillToExtract <= 0) break;

            ItemStack stackRep = variant.toStack(1);

            Container container = getContainer(level, info.containerPos());
            if (container != null) {
                WorldlyContainer sidedContainer = container instanceof WorldlyContainer ? (WorldlyContainer) container : null;
                int[] slots = sidedContainer != null ? sidedContainer.getSlotsForFace(info.insertDirection()) : null;
                int size = slots != null ? slots.length : container.getContainerSize();

                for (int i = 0; i < size; i++) {
                    if (stillToExtract <= 0) break;
                    int slot = slots != null ? slots[i] : i;
                    ItemStack slotStack = container.getItem(slot);
                    if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stackRep)) {
                        int toRemove = (int) Math.min(slotStack.getCount(), stillToExtract);
                        slotStack.shrink(toRemove);
                        container.setItem(slot, slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
                        container.setChanged();
                        stillToExtract -= toRemove;
                    }
                }
                continue;
            }

            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    level, info.containerPos(), info.insertDirection()
            );

            if (storage != null) {
                try (Transaction tx = Transaction.openOuter()) {
                    long extracted = storage.extract(variant, stillToExtract, tx);
                    if (extracted > 0) {
                        tx.commit();
                        stillToExtract -= extracted;
                    }
                }
            }
        }

        return amountRequested - stillToExtract;
    }

    /**
     * Deposita un ítem físicamente únicamente en los contenedores conectados de la red que tengan un filtro que lo acepte (Pasada 1).
     */
    public static long depositIntoNetwork(Level level, BlockPos startPos, ItemStack toDepositStack) {
        if (toDepositStack.isEmpty()) return 0;
        List<NetworkContainerInfo> containers = getNetworkContainers(level, startPos);
        ItemVariant variant = ItemVariant.of(toDepositStack);
        long remainingToInsert = toDepositStack.getCount();

        // Pasada 1 ÚNICA: Solo deposita en contenedores con un filtro que acepte el ítem
        for (NetworkContainerInfo info : containers) {
            if (remainingToInsert <= 0) break;
            if (info.filterBE() != null && info.filterBE().matchesFilter(toDepositStack)) {
                remainingToInsert -= tryInsertIntoContainer(level, info, variant, remainingToInsert);
            }
        }

        return toDepositStack.getCount() - remainingToInsert;
    }

    private static long tryInsertIntoContainer(Level level, NetworkContainerInfo info, ItemVariant variant, long amount) {
        Container container = getContainer(level, info.containerPos());
        if (container != null) {
            WorldlyContainer sidedContainer = container instanceof WorldlyContainer ? (WorldlyContainer) container : null;
            int[] slots = sidedContainer != null ? sidedContainer.getSlotsForFace(info.insertDirection()) : null;
            int size = slots != null ? slots.length : container.getContainerSize();

            ItemStack insertingStack = variant.toStack((int) amount);
            long initialCount = insertingStack.getCount();

            // Pasada 1: Apilar en slots existentes
            for (int i = 0; i < size; i++) {
                if (insertingStack.isEmpty()) break;
                int slot = slots != null ? slots[i] : i;
                ItemStack slotStack = container.getItem(slot);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, insertingStack)) {
                    if (container.canPlaceItem(slot, insertingStack) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, insertingStack, info.insertDirection()))) {
                        int slotMax = Math.min(slotStack.getMaxStackSize(), container.getMaxStackSize());
                        int space = slotMax - slotStack.getCount();
                        if (space > 0) {
                            int toAdd = Math.min(insertingStack.getCount(), space);
                            container.setItem(slot, slotStack.copyWithCount(slotStack.getCount() + toAdd));
                            insertingStack.shrink(toAdd);
                            container.setChanged();
                        }
                    }
                }
            }

            // Pasada 2: Insertar en slots vacíos
            for (int i = 0; i < size; i++) {
                if (insertingStack.isEmpty()) break;
                int slot = slots != null ? slots[i] : i;
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    if (container.canPlaceItem(slot, insertingStack) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, insertingStack, info.insertDirection()))) {
                        int slotMax = Math.min(insertingStack.getMaxStackSize(), container.getMaxStackSize());
                        int toAdd = Math.min(insertingStack.getCount(), slotMax);
                        container.setItem(slot, insertingStack.copyWithCount(toAdd));
                        insertingStack.shrink(toAdd);
                        container.setChanged();
                    }
                }
            }

            return initialCount - insertingStack.getCount();
        }

        Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                level, info.containerPos(), info.insertDirection()
        );

        if (storage != null) {
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = storage.insert(variant, amount, tx);
                if (inserted > 0) {
                    tx.commit();
                    return inserted;
                }
            }
        }

        return 0;
    }

    private static Container getContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, level, pos, true);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            return container;
        }
        return null;
    }
}
