package com.example.simplepipes.entidades;

import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Entidad de bloque para el Cofre Explorador.
 * No almacena ítems propios, actúa como terminal de consulta y acceso a la red de tuberías.
 */
public class ExplorerChestBlockEntity extends BlockEntity implements MenuProvider {

    public ExplorerChestBlockEntity(BlockPos pos, BlockState state) {
        super(BloquesMod.EXPLORER_CHEST_BE, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simplepipes.explorer_chest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return com.example.simplepipes.gui.ExplorerChestMenu.create(containerId, playerInventory, this.worldPosition);
    }
}

