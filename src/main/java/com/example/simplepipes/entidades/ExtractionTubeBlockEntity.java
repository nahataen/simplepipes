package com.example.simplepipes.entidades;

import com.example.simplepipes.red.ResultadoRuta;
import com.example.simplepipes.red.RedLogistica;
import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractionTubeBlockEntity extends TubeBlockEntity {

    private int cooldown = 0;
    private int roundRobinIndex = 0;

    public ExtractionTubeBlockEntity(BlockPos pos, BlockState state) {
        super(BloquesMod.EXTRACTION_TUBE_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExtractionTubeBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        TubeBlockEntity.tick(level, pos, state, blockEntity);

        if (blockEntity.cooldown++ >= 20) {
            blockEntity.cooldown = 0;
            blockEntity.extractFromSource(level, pos);
        }
    }

    private void extractFromSource(Level level, BlockPos pos) {
        Map<ItemVariant, List<SourceSlot>> slotsByVariant = new HashMap<>();

        for (Direction dir : Direction.values()) {
            BlockPos sourcePos = pos.relative(dir);
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    level, sourcePos, dir.getOpposite());
            if (storage == null) continue;

            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank()) continue;
                long amountInSlot = view.getAmount();
                if (amountInSlot <= 0) continue;

                ItemVariant variant = view.getResource();
                slotsByVariant
                        .computeIfAbsent(variant, k -> new ArrayList<>())
                        .add(new SourceSlot(storage, sourcePos, variant, amountInSlot));
            }
        }

        if (slotsByVariant.isEmpty()) return;

        Map<BlockPos, Integer> claimedSpace = new HashMap<>();

        for (Map.Entry<ItemVariant, List<SourceSlot>> entry : slotsByVariant.entrySet()) {
            ItemVariant variant = entry.getKey();
            List<SourceSlot> slots = entry.getValue();
            int maxStack = variant.toStack(1).getMaxStackSize();
            long totalAvailable = slots.stream().mapToLong(s -> s.amount).sum();
            if (totalAvailable <= 0) continue;

            ItemStack representativeStack = variant.toStack((int) Math.min(totalAvailable, maxStack));
            BlockPos firstSourcePos = slots.get(0).sourcePos;
            List<ResultadoRuta> validRoutes = RedLogistica.findAllRoutes(level, pos, firstSourcePos, representativeStack);
            if (validRoutes.isEmpty()) continue;

            int routeCount = validRoutes.size();

            for (SourceSlot slot : slots) {
                long stillToExtractFromSlot = slot.amount;
                long slotInitialAmount = slot.amount;

                List<Integer> activeRouteIndices = new ArrayList<>();
                for (int i = 0; i < routeCount; i++) {
                    int candidateIdx = (roundRobinIndex + i) % routeCount;
                    ResultadoRuta candidate = validRoutes.get(candidateIdx);
                    int claimedSoFar = claimedSpace.getOrDefault(candidate.destination, 0);
                    if (candidate.maxSpace - claimedSoFar > 0) {
                        activeRouteIndices.add(candidateIdx);
                    }
                }

                int initialActiveCount = activeRouteIndices.size();
                if (initialActiveCount == 0) continue;

                long targetSharePerRoute = Math.max(1, (slotInitialAmount + initialActiveCount - 1) / initialActiveCount);

                while (stillToExtractFromSlot > 0 && !activeRouteIndices.isEmpty()) {
                    int selectedRouteIdx = activeRouteIndices.get(0);
                    ResultadoRuta selectedRoute = validRoutes.get(selectedRouteIdx);

                    int claimedSoFar = claimedSpace.getOrDefault(selectedRoute.destination, 0);
                    int netSpaceLeft = selectedRoute.maxSpace - claimedSoFar;

                    if (netSpaceLeft <= 0) {
                        activeRouteIndices.remove(0);
                        continue;
                    }

                    long batchSize = Math.min(stillToExtractFromSlot, maxStack);
                    batchSize = Math.min(batchSize, netSpaceLeft);
                    if (activeRouteIndices.size() > 1) {
                        batchSize = Math.min(batchSize, targetSharePerRoute);
                    }

                    try (Transaction tx = Transaction.openOuter()) {
                        long extracted = slot.storage.extract(variant, batchSize, tx);
                        if (extracted > 0) {
                            tx.commit();
                            ItemStack extractedStack = variant.toStack((int) extracted);
                            addTravelingItem(new TravelingItem(
                                    extractedStack, selectedRoute.path, selectedRoute.destination, selectedRoute.insertSide));

                            claimedSpace.merge(selectedRoute.destination, (int) extracted, Integer::sum);
                            stillToExtractFromSlot -= extracted;
                            roundRobinIndex = (selectedRouteIdx + 1) % routeCount;

                            activeRouteIndices.remove(0);
                            int updatedNetSpace = selectedRoute.maxSpace - claimedSpace.getOrDefault(selectedRoute.destination, 0);
                            if (updatedNetSpace > 0 && stillToExtractFromSlot > 0) {
                                activeRouteIndices.add(selectedRouteIdx);
                            }
                        } else {
                            activeRouteIndices.remove(0);
                        }
                    }
                }
            }
        }
    }

    private static class SourceSlot {
        final Storage<ItemVariant> storage;
        final BlockPos sourcePos;
        final ItemVariant variant;
        final long amount;

        SourceSlot(Storage<ItemVariant> storage, BlockPos sourcePos, ItemVariant variant, long amount) {
            this.storage = storage;
            this.sourcePos = sourcePos;
            this.variant = variant;
            this.amount = amount;
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("cooldown", cooldown);
        output.putInt("roundRobinIndex", roundRobinIndex);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        cooldown = input.getIntOr("cooldown", 0);
        roundRobinIndex = input.getIntOr("roundRobinIndex", 0);
    }
}
