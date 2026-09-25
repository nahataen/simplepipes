package com.example.simplepipes;

import com.example.simplepipes.registro.BloquesMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal del mod SimplePipes (Fabric).
 * Punto de entrada: inicializa y registra bloques, items y pestañas creativas.
 */
public class SimplePipesMod implements ModInitializer {

    public static final String MOD_ID = "simplepipes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SimplePipes mod initializing (Fabric 26.1.2)...");

        // Inicializa registros estáticos de bloques/items del mod
        BloquesMod.init();

        // Registrar paquetes de red del Cofre Explorador
        com.example.simplepipes.red.ExplorerChestPayloads.registerCommon();

        // Registrar en pestañas del modo creativo
        registerCreativeTabEntries();

        LOGGER.info("SimplePipes mod initialized successfully!");
    }

    /**
     * Registra los bloques del mod en las pestañas del menú creativo.
     */
    private void registerCreativeTabEntries() {
        CreativeModeTabEvents.modifyOutputEvent(
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "redstone_blocks"))
        ).register(output -> {
            output.accept(BloquesMod.TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.FILTER_TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.EXTRACTION_TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.EXPLORER_CHEST_ITEM.getDefaultInstance());
        });

        CreativeModeTabEvents.modifyOutputEvent(
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "functional_blocks"))
        ).register(output -> {
            output.accept(BloquesMod.TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.FILTER_TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.EXTRACTION_TUBE_ITEM.getDefaultInstance());
            output.accept(BloquesMod.EXPLORER_CHEST_ITEM.getDefaultInstance());
        });
    }
}
