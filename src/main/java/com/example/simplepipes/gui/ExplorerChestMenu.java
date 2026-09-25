package com.example.simplepipes.gui;

import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu (ScreenHandler) para la interfaz del Cofre Explorador.
 * Conecta el inventario del jugador con la red del Cofre Explorador.
 */
public class ExplorerChestMenu extends AbstractContainerMenu {

    private BlockPos explorerPos;

    public static ExplorerChestMenu create(int containerId, Inventory playerInventory, BlockPos explorerPos) {
        return new ExplorerChestMenu(containerId, playerInventory, explorerPos);
    }

    public ExplorerChestMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }

    public ExplorerChestMenu(int containerId, Inventory playerInventory, BlockPos explorerPos) {
        super(BloquesMod.EXPLORER_CHEST_MENU, containerId);
        this.explorerPos = explorerPos != null ? explorerPos : BlockPos.ZERO;

        // Ranuras del inventario del jugador (filas 0..2)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 13 + col * 18, 140 + row * 18));
            }
        }

        // Ranuras de la barra rápida (Hotbar)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 13 + col * 18, 198));
        }
    }

    public BlockPos getExplorerPos() {
        return explorerPos;
    }

    public void setExplorerPos(BlockPos explorerPos) {
        this.explorerPos = explorerPos;
    }

    @Override
    public boolean stillValid(Player player) {
        if (explorerPos.equals(BlockPos.ZERO)) return true;
        return player.distanceToSqr(explorerPos.getX() + 0.5, explorerPos.getY() + 0.5, explorerPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-click desde el inventario del jugador envía la orden de depósito a la red
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            if (!player.level().isClientSide()) {
                long inserted = com.example.simplepipes.red.ExplorerNetworkHelper.depositIntoNetwork(player.level(), explorerPos, stackInSlot);
                if (inserted > 0) {
                    stackInSlot.shrink((int) inserted);
                    slot.set(stackInSlot.isEmpty() ? ItemStack.EMPTY : stackInSlot);
                    this.broadcastChanges();
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        com.example.simplepipes.red.ExplorerChestPayloads.syncNetworkToPlayer(serverPlayer, explorerPos);
                    }
                }
            } else {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new com.example.simplepipes.red.ExplorerChestPayloads.DepositItemPayload(explorerPos, slot.getContainerSlot(), true)
                );
            }
        }
        return ItemStack.EMPTY;
    }
}
