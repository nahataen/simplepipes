package com.example.simplepipes.entidades;

import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TubeBlockEntity extends BlockEntity {

    protected final List<TravelingItem> travelingItems = new ArrayList<>();

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(BloquesMod.TUBE_BE, pos, state);
    }

    protected TubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public List<TravelingItem> getTravelingItems() {
        return new ArrayList<>(travelingItems);
    }

    public void addTravelingItem(TravelingItem item) {
        this.travelingItems.add(item);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TubeBlockEntity blockEntity) {
        if (level.isClientSide() || blockEntity.travelingItems.isEmpty()) return;

        List<TravelingItem> copy = new ArrayList<>(blockEntity.travelingItems);
        List<TravelingItem> toRemove = new ArrayList<>();
        boolean changed = false;

        for (TravelingItem item : copy) {
            if (item.progress < 1.0f) {
                item.progress += 0.08f;
            }

            if (item.progress >= 1.0f) {
                item.progress = 1.0f;
                int nextIndex = item.currentPathIndex + 1;

                if (nextIndex < item.path.size()) {
                    BlockPos nextPos = item.path.get(nextIndex);
                    BlockEntity nextBE = level.getBlockEntity(nextPos);

                    if (nextBE instanceof TubeBlockEntity nextTube) {
                        TravelingItem movedItem = new TravelingItem(
                                item.stack, item.path, nextIndex, item.destinationPos, item.insertDirection
                        );
                        nextTube.addTravelingItem(movedItem);
                        toRemove.add(item);
                        changed = true;
                    } else {
                        // El tubo siguiente fue destruido: soltar item
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.stack);
                        toRemove.add(item);
                        changed = true;
                    }
                } else {
                    ItemStack remainder = insertStack(level, item.destinationPos, item.insertDirection, item.stack);
                    if (remainder.isEmpty()) {
                        toRemove.add(item);
                        changed = true;
                    } else {
                        item.stack = remainder;
                        changed = true;

                        boolean destinationExists = (getContainer(level, item.destinationPos) != null) ||
                                (net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, item.destinationPos, item.insertDirection) != null);

                        if (!destinationExists) {
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.stack);
                            toRemove.add(item);
                        }
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            blockEntity.travelingItems.removeAll(toRemove);
            changed = true;
        }

        if (changed) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var listOutput = output.childrenList("travelingItems");
        for (TravelingItem item : travelingItems) {
            if (item.stack.isEmpty()) continue;
            var child = listOutput.addChild();
            child.store("stack", ItemStack.CODEC, item.stack);
            child.putInt("currentPathIndex", item.currentPathIndex);
            child.putFloat("progress", item.progress);
            child.store("destinationPos", BlockPos.CODEC, item.destinationPos);
            child.putString("insertDirection", item.insertDirection.name());

            var pathList = child.list("path", BlockPos.CODEC);
            for (BlockPos p : item.path) {
                pathList.add(p);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        travelingItems.clear();
        var listInput = input.childrenListOrEmpty("travelingItems");
        for (ValueInput child : listInput) {
            ItemStack stack = child.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;

            int currentPathIndex = child.getIntOr("currentPathIndex", 0);
            float progress = child.getFloatOr("progress", 0.0f);
            BlockPos destinationPos = child.read("destinationPos", BlockPos.CODEC).orElse(BlockPos.ZERO);
            String dirStr = child.getStringOr("insertDirection", "UP");
            Direction insertDirection = Direction.valueOf(dirStr);

            List<BlockPos> path = new ArrayList<>();
            for (BlockPos p : child.listOrEmpty("path", BlockPos.CODEC)) {
                path.add(p);
            }

            TravelingItem tItem = new TravelingItem(stack, path, destinationPos, insertDirection);
            tItem.currentPathIndex = currentPathIndex;
            tItem.progress = progress;
            travelingItems.add(tItem);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
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

    public static ItemStack insertStack(Level level, BlockPos destinationPos, Direction insertDirection, ItemStack itemToInsert) {
        if (itemToInsert.isEmpty()) return ItemStack.EMPTY;

        Container container = getContainer(level, destinationPos);
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

            ItemStack toInsert = itemToInsert.copy();

            // Pasada 1: apilar en slots con el mismo item
            for (int slot : slots) {
                if (toInsert.isEmpty()) break;
                ItemStack slotStack = container.getItem(slot);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                    if (container.canPlaceItem(slot, toInsert) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, toInsert, insertDirection))) {
                        int maxStack = Math.min(slotStack.getMaxStackSize(), container.getMaxStackSize());
                        int space = maxStack - slotStack.getCount();
                        if (space > 0) {
                            int toAdd = Math.min(toInsert.getCount(), space);
                            ItemStack newStack = slotStack.copyWithCount(slotStack.getCount() + toAdd);
                            container.setItem(slot, newStack);
                            toInsert.shrink(toAdd);
                            container.setChanged();
                        }
                    }
                }
            }

            // Pasada 2: insertar en slots vacios
            for (int slot : slots) {
                if (toInsert.isEmpty()) break;
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    if (container.canPlaceItem(slot, toInsert) && (sidedContainer == null || sidedContainer.canPlaceItemThroughFace(slot, toInsert, insertDirection))) {
                        int maxStack = Math.min(toInsert.getMaxStackSize(), container.getMaxStackSize());
                        if (toInsert.getCount() <= maxStack) {
                            container.setItem(slot, toInsert.copy());
                            toInsert.setCount(0);
                        } else {
                            ItemStack newStack = toInsert.copy();
                            newStack.setCount(maxStack);
                            container.setItem(slot, newStack);
                            toInsert.shrink(maxStack);
                        }
                        container.setChanged();
                    }
                }
            }

            return toInsert.isEmpty() ? ItemStack.EMPTY : toInsert;
        }

        // Fallback: Fabric Transfer API
        Storage<ItemVariant> destStorage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                level, destinationPos, insertDirection
        );

        if (destStorage == null) {
            for (Direction fallback : Direction.values()) {
                if (fallback == insertDirection) continue;
                destStorage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                        level, destinationPos, fallback
                );
                if (destStorage != null) break;
            }
        }

        if (destStorage == null) {
            return itemToInsert;
        }

        ItemStack toInsert = itemToInsert.copy();
        ItemVariant variant = ItemVariant.of(toInsert);
        long remaining = toInsert.getCount();

        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : destStorage) {
                if (remaining <= 0) break;
                if (!view.isResourceBlank() && view.getResource().equals(variant)) {
                    if (view instanceof SingleSlotStorage<ItemVariant> slot) {
                        long inserted = slot.insert(variant, remaining, tx);
                        remaining -= inserted;
                    }
                }
            }

            if (remaining > 0) {
                for (StorageView<ItemVariant> view : destStorage) {
                    if (remaining <= 0) break;
                    if (view.isResourceBlank()) {
                        if (view instanceof SingleSlotStorage<ItemVariant> slot) {
                            long inserted = slot.insert(variant, remaining, tx);
                            remaining -= inserted;
                        }
                    }
                }
            }

            long totalInserted = toInsert.getCount() - remaining;
            if (totalInserted > 0) {
                tx.commit();
                toInsert.shrink((int) totalInserted);
            }
        }

        return toInsert.isEmpty() ? ItemStack.EMPTY : toInsert;
    }
}
