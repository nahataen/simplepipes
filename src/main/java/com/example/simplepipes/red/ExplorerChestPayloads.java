package com.example.simplepipes.red;

import com.example.simplepipes.SimplePipesMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registra y gestiona los paquetes de red para el Cofre Explorador.
 */
public class ExplorerChestPayloads {

    // 1. SyncItemsPayload (S2C): envía el resumen de ítems reales en la red al cliente
    public record SyncItemsPayload(BlockPos explorerPos, List<ItemEntry> items) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncItemsPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, "explorer_sync_items"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemsPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBlockPos(payload.explorerPos());
                    buf.writeInt(payload.items().size());
                    for (ItemEntry entry : payload.items()) {
                        ItemStack.STREAM_CODEC.encode(buf, entry.stack());
                        buf.writeVarLong(entry.count());
                    }
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int count = buf.readInt();
                    List<ItemEntry> items = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                        long cnt = buf.readVarLong();
                        items.add(new ItemEntry(stack, cnt));
                    }
                    return new SyncItemsPayload(pos, items);
                }
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ItemEntry(ItemStack stack, long count) {}

    // 2. ExtractItemPayload (C2S): cliente solicita extraer un ítem del Cofre Explorador
    public record ExtractItemPayload(BlockPos explorerPos, ItemStack requestedItem, int amount, boolean shiftClick) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ExtractItemPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, "explorer_extract_item"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBlockPos(payload.explorerPos());
                    ItemStack.STREAM_CODEC.encode(buf, payload.requestedItem());
                    buf.writeInt(payload.amount());
                    buf.writeBoolean(payload.shiftClick());
                },
                buf -> new ExtractItemPayload(
                        buf.readBlockPos(),
                        ItemStack.STREAM_CODEC.decode(buf),
                        buf.readInt(),
                        buf.readBoolean()
                )
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 3. DepositItemPayload (C2S): cliente deposita un ítem de un slot de su inventario en la red
    public record DepositItemPayload(BlockPos explorerPos, int slotIndex, boolean shiftClick) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DepositItemPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, "explorer_deposit_item"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DepositItemPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBlockPos(payload.explorerPos());
                    buf.writeInt(payload.slotIndex());
                    buf.writeBoolean(payload.shiftClick());
                },
                buf -> new DepositItemPayload(
                        buf.readBlockPos(),
                        buf.readInt(),
                        buf.readBoolean()
                )
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 4. DepositCarriedItemPayload (C2S): cliente deposita el ítem sujetado en el cursor en la red
    public record DepositCarriedItemPayload(BlockPos explorerPos, int amount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DepositCarriedItemPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, "explorer_deposit_carried"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DepositCarriedItemPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBlockPos(payload.explorerPos());
                    buf.writeInt(payload.amount());
                },
                buf -> new DepositCarriedItemPayload(
                        buf.readBlockPos(),
                        buf.readInt()
                )
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(SyncItemsPayload.TYPE, SyncItemsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExtractItemPayload.TYPE, ExtractItemPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DepositItemPayload.TYPE, DepositItemPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DepositCarriedItemPayload.TYPE, DepositCarriedItemPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ExtractItemPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleExtract(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(DepositItemPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleDeposit(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(DepositCarriedItemPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleDepositCarried(player, payload));
        });
    }

    private static void handleExtract(ServerPlayer player, ExtractItemPayload payload) {
        if (payload.requestedItem().isEmpty()) return;
        ItemVariant variant = ItemVariant.of(payload.requestedItem());
        long extractedCount = ExplorerNetworkHelper.extractFromNetwork(player.level(), payload.explorerPos(), variant, payload.amount());

        if (extractedCount > 0) {
            ItemStack extractedStack = variant.toStack((int) extractedCount);
            if (payload.shiftClick()) {
                if (!player.getInventory().add(extractedStack)) {
                    player.drop(extractedStack, false);
                }
            } else {
                ItemStack carrying = player.containerMenu.getCarried();
                if (carrying.isEmpty()) {
                    player.containerMenu.setCarried(extractedStack);
                } else if (ItemStack.isSameItemSameComponents(carrying, extractedStack)) {
                    int maxStack = carrying.getMaxStackSize();
                    int space = maxStack - carrying.getCount();
                    int toAdd = (int) Math.min(extractedCount, space);
                    if (toAdd > 0) {
                        carrying.grow(toAdd);
                        player.containerMenu.setCarried(carrying);
                        int excess = (int) extractedCount - toAdd;
                        if (excess > 0) {
                            ExplorerNetworkHelper.depositIntoNetwork(player.level(), payload.explorerPos(), variant.toStack(excess));
                        }
                    }
                } else {
                    if (!player.getInventory().add(extractedStack)) {
                        player.drop(extractedStack, false);
                    }
                }
            }
            player.containerMenu.broadcastChanges();
        }
        syncNetworkToPlayer(player, payload.explorerPos());
    }

    private static void handleDeposit(ServerPlayer player, DepositItemPayload payload) {
        if (payload.slotIndex() < 0 || payload.slotIndex() >= player.getInventory().getContainerSize()) return;
        ItemStack stackInSlot = player.getInventory().getItem(payload.slotIndex());
        if (stackInSlot.isEmpty()) return;

        long inserted = ExplorerNetworkHelper.depositIntoNetwork(player.level(), payload.explorerPos(), stackInSlot);
        if (inserted > 0) {
            stackInSlot.shrink((int) inserted);
            player.getInventory().setItem(payload.slotIndex(), stackInSlot.isEmpty() ? ItemStack.EMPTY : stackInSlot);
            player.containerMenu.broadcastChanges();
        }
        syncNetworkToPlayer(player, payload.explorerPos());
    }

    private static void handleDepositCarried(ServerPlayer player, DepositCarriedItemPayload payload) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) return;

        int toDepositCount = Math.min(carried.getCount(), payload.amount());
        if (toDepositCount <= 0) return;

        ItemStack stackToDeposit = carried.copyWithCount(toDepositCount);
        long inserted = ExplorerNetworkHelper.depositIntoNetwork(player.level(), payload.explorerPos(), stackToDeposit);

        if (inserted > 0) {
            carried.shrink((int) inserted);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            player.containerMenu.broadcastChanges();
        }
        syncNetworkToPlayer(player, payload.explorerPos());
    }

    public static void syncNetworkToPlayer(ServerPlayer player, BlockPos explorerPos) {
        Map<ItemVariant, Long> summary = ExplorerNetworkHelper.getNetworkItemSummary(player.level(), explorerPos);
        List<ItemEntry> entries = new ArrayList<>();

        for (Map.Entry<ItemVariant, Long> e : summary.entrySet()) {
            entries.add(new ItemEntry(e.getKey().toStack(1), e.getValue()));
        }

        ServerPlayNetworking.send(player, new SyncItemsPayload(explorerPos, entries));
    }
}
