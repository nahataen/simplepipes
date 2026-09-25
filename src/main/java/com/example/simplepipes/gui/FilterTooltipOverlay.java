package com.example.simplepipes.gui;

import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay HUD que se renderiza cuando el jugador mira directamente a un FilterTubeBlock.
 */
public class FilterTooltipOverlay {

    /**
     * Renderiza el overlay. Debe llamarse cada tick desde ClientTickEvents.
     */
    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();

        // Verificar que el bloque es un FilterTube
        if (mc.level.getBlockState(pos).getBlock() != BloquesMod.FILTER_TUBE) return;

        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof FilterTubeBlockEntity filterBE)) return;

        List<ItemStack> filters = filterBE.getFilterItems();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        // Construir líneas del tooltip
        List<Component> lines = new ArrayList<>();
        if (filters.isEmpty()) {
            lines.add(Component.literal("§6◈ §7Tubo de Filtro — §cVacío (rechaza todo)"));
        } else {
            lines.add(Component.literal("§6◈ §7Tubo de Filtro (§b" + filters.size() + "§7):"));
            for (ItemStack filter : filters) {
                lines.add(Component.literal("  §6▸ §f" + filter.getHoverName().getString()));
            }
        }

        // Calcular posición del tooltip
        int tooltipX = cx + 12;
        int tooltipY = cy - (lines.size() * mc.font.lineHeight) / 2 - 6;

        int maxWidth = 0;
        for (Component line : lines) {
            int w = mc.font.width(line);
            if (w > maxWidth) maxWidth = w;
        }
        if (tooltipX + maxWidth > screenWidth - 4) {
            tooltipX = cx - maxWidth - 14;
        }
        if (tooltipY < 2) tooltipY = 2;
        if (tooltipY + lines.size() * (mc.font.lineHeight + 2) > screenHeight - 4) {
            tooltipY = screenHeight - 4 - lines.size() * (mc.font.lineHeight + 2);
        }

        // Fondo semitransparente con borde
        int bgColor = 0xC0101010;
        int borderColor = 0xFF44AA66;
        int padding = 4;
        int lineHeight = mc.font.lineHeight + 1;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = lines.size() * lineHeight + padding * 2;

        guiGraphics.fill(tooltipX - padding, tooltipY - padding,
                tooltipX + boxWidth, tooltipY + boxHeight, bgColor);
        guiGraphics.fill(tooltipX - padding, tooltipY - padding,
                tooltipX + boxWidth, tooltipY - padding + 1, borderColor);
        guiGraphics.fill(tooltipX - padding, tooltipY + boxHeight - 1,
                tooltipX + boxWidth, tooltipY + boxHeight, borderColor);
        guiGraphics.fill(tooltipX - padding, tooltipY - padding,
                tooltipX - padding + 1, tooltipY + boxHeight, borderColor);
        guiGraphics.fill(tooltipX + boxWidth - 1, tooltipY - padding,
                tooltipX + boxWidth, tooltipY + boxHeight, borderColor);

        // Dibujar texto
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.text(mc.font, lines.get(i), tooltipX, tooltipY + i * lineHeight, -1, false);
        }
    }
}