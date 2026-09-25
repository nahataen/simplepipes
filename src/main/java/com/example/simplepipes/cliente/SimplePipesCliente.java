package com.example.simplepipes.cliente;

import com.example.simplepipes.SimplePipesMod;
import com.example.simplepipes.registro.BloquesMod;
import com.example.simplepipes.gui.FilterTooltipOverlay;
import com.example.simplepipes.render.TubeBlockEntityRenderer;
import com.example.simplepipes.entidades.TubeBlockEntity;
import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.example.simplepipes.entidades.ExtractionTubeBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint cliente para SimplePipes (Fabric).
 * Registra renderizadores y elementos HUD del cliente.
 */
public class SimplePipesCliente implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(SimplePipesMod.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("SimplePipes client initializing...");

        // Registrar renderizadores de BlockEntity
        BlockEntityRendererRegistry.<TubeBlockEntity, TubeBlockEntityRenderer.TubeRenderState>register(
                BloquesMod.TUBE_BE,
                context -> new TubeBlockEntityRenderer<>(context)
        );
        BlockEntityRendererRegistry.<FilterTubeBlockEntity, TubeBlockEntityRenderer.TubeRenderState>register(
                BloquesMod.FILTER_TUBE_BE,
                context -> new TubeBlockEntityRenderer<>(context)
        );
        BlockEntityRendererRegistry.<ExtractionTubeBlockEntity, TubeBlockEntityRenderer.TubeRenderState>register(
                BloquesMod.EXTRACTION_TUBE_BE,
                context -> new TubeBlockEntityRenderer<>(context)
        );

        // Registrar overlay HUD
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, "filter_tooltip"),
            FilterTooltipOverlay::render
        );

        // Registrar pantalla del Cofre Explorador
        net.minecraft.client.gui.screens.MenuScreens.register(
                BloquesMod.EXPLORER_CHEST_MENU,
                com.example.simplepipes.gui.ExplorerChestScreen::new
        );

        // Registrar receptor cliente para sincronizar ítems de la red
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.example.simplepipes.red.ExplorerChestPayloads.SyncItemsPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        com.example.simplepipes.gui.ExplorerChestScreen.updateNetworkItems(payload.items());
                        if (context.client().screen instanceof com.example.simplepipes.gui.ExplorerChestScreen screen) {
                            screen.updateNetworkPosAndItems(payload.explorerPos(), payload.items());
                        }
                    });
                }
        );
        LOGGER.info("SimplePipes client initialized successfully!");
    }
}
