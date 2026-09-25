package com.example.simplepipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renderizador dedicado exclusivamente a la presentación visual (GUI)
 * del Cofre Explorador. No contiene lógica de negocio ni manipulación de estado.
 */
public class ExplorerChestGuiRenderer {

    // Paleta Procedural Vanilla (100% opaca - 0xFF...)
    public static final int C_VANILLA_PANEL  = 0xFFC6C6C6; // Gris claro vanilla
    public static final int C_TEST_PANEL     = 0xFF00FF00; // Verde llamativo
    public static final int C_BORDER_LIGHT   = 0xFFFFFFFF; // Borde luz blanco
    public static final int C_BORDER_DARK    = 0xFF373737; // Borde sombra gris oscuro
    public static final int C_SLOT_BG        = 0xFF8B8B8B; // Interior slot gris medio
    public static final int C_SEARCH_BG      = 0xFF000000; // Fondo caja de búsqueda negro

    /**
     * Color gris Vanilla activado (sin verde de prueba).
     */
    public static boolean USE_TEST_COLOR = false;

    /**
     * Dibuja un panel rectangular opaco con bordes redondos y estilo vanilla (bisel 3D).
     */
    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height, int panelColor) {
        // Fondo principal opaco con esquinas redondeadas
        context.fill(x + 3, y,                 x + width - 3, y + 1,          panelColor);
        context.fill(x + 2, y + 1,             x + width - 2, y + 2,          panelColor);
        context.fill(x + 1, y + 2,             x + width - 1, y + 3,          panelColor);
        context.fill(x,     y + 3,             x + width,     y + height - 3, panelColor);
        context.fill(x + 1, y + height - 3,     x + width - 1, y + height - 2, panelColor);
        context.fill(x + 2, y + height - 2,     x + width - 2, y + height - 1, panelColor);
        context.fill(x + 3, y + height - 1,     x + width - 3, y + height,     panelColor);

        // Bisel 3D redondeado (Luz superior e izquierda, Sombra inferior y derecha)
        // Borde superior (luz)
        context.fill(x + 3, y,         x + width - 3, y + 1, C_BORDER_LIGHT);
        context.fill(x + 2, y + 1,     x + 3,         y + 2, C_BORDER_LIGHT);
        context.fill(x + 1, y + 2,     x + 2,         y + 3, C_BORDER_LIGHT);
        context.fill(x + width - 3, y + 1, x + width - 2, y + 2, C_BORDER_LIGHT);

        // Borde izquierdo (luz)
        context.fill(x, y + 3, x + 1, y + height - 3, C_BORDER_LIGHT);

        // Borde inferior (sombra)
        context.fill(x + 3, y + height - 1, x + width - 3, y + height, C_BORDER_DARK);
        context.fill(x + 2, y + height - 2, x + 3,         y + height - 1, C_BORDER_DARK);
        context.fill(x + 1, y + height - 3, x + 2,         y + height - 2, C_BORDER_DARK);

        // Borde derecho (sombra)
        context.fill(x + width - 1, y + 3,          x + width,     y + height - 3, C_BORDER_DARK);
        context.fill(x + width - 3, y + height - 2, x + width - 2, y + height - 1, C_BORDER_DARK);
        context.fill(x + width - 2, y + height - 3, x + width - 1, y + height - 2, C_BORDER_DARK);
        context.fill(x + width - 2, y + 2,          x + width - 1, y + 3,          C_BORDER_DARK);
    }

    /**
     * Dibuja un slot individual de 18x18 píxeles procedural con bordes biselados hacia dentro (hundido).
     */
    public static void drawSlot(GuiGraphicsExtractor context, int x, int y) {
        // Relleno interior 16x16
        context.fill(x + 1, y + 1, x + 17, y + 17, C_SLOT_BG);

        // Bordes hundidos (sombra arriba/izquierda, luz abajo/derecha)
        context.fill(x, y, x + 18, y + 1, C_BORDER_DARK);
        context.fill(x, y, x + 1, y + 18, C_BORDER_DARK);
        context.fill(x + 17, y, x + 18, y + 18, C_BORDER_LIGHT);
        context.fill(x, y + 17, x + 18, y + 18, C_BORDER_LIGHT);
    }

    /**
     * Dibuja una rejilla de slots de cols x rows comenzando en (startX, startY).
     */
    public static void drawSlotGrid(GuiGraphicsExtractor context, int startX, int startY, int cols, int rows) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawSlot(context, startX + c * 18, startY + r * 18);
            }
        }
    }

    /**
     * Dibuja el fondo de la caja de búsqueda (hundido y con bordes suavizados).
     */
    public static void drawSearchBoxBg(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x + 1, y,     x + width - 1, y + height, C_SEARCH_BG);
        context.fill(x,     y + 1, x + width,     y + height - 1, C_SEARCH_BG);

        context.fill(x + 1, y - 1,      x + width - 1, y,          C_BORDER_DARK);
        context.fill(x - 1, y + 1,      x,             y + height - 1, C_BORDER_DARK);
        context.fill(x + 1, y + height, x + width - 1, y + height + 1, C_BORDER_LIGHT);
        context.fill(x + width, y + 1,  x + width + 1, y + height - 1, C_BORDER_LIGHT);
    }

    /**
     * Dibuja la pista de la barra de desplazamiento (scrollbar track).
     */
    public static void drawScrollbarTrack(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, C_SLOT_BG);
        context.fill(x, y, x + width, y + 1, C_BORDER_DARK);
        context.fill(x, y, x + 1, y + height, C_BORDER_DARK);
        context.fill(x, y + height - 1, x + width, y + height, C_BORDER_LIGHT);
        context.fill(x + width - 1, y, x + width, y + height, C_BORDER_LIGHT);
    }

    /**
     * Renderiza todo el fondo visual de la GUI del Cofre Explorador delegando a los métodos elementales.
     */
    public static void renderFullGuiBackground(GuiGraphicsExtractor context, int leftPos, int topPos, int imageWidth, int imageHeight) {
        int panelColor = USE_TEST_COLOR ? C_TEST_PANEL : C_VANILLA_PANEL;

        // 1. Panel principal redondeado
        drawPanel(context, leftPos, topPos, imageWidth, imageHeight, panelColor);

        // 2. Fondo de la caja de búsqueda
        drawSearchBoxBg(context, leftPos + 13, topPos + 18, 168, 14);

        // 3. Rejilla de la Red de ítems (9x4 slots)
        drawSlotGrid(context, leftPos + 12, topPos + 37, 9, 4);

        // 4. Rejilla del Inventario del Jugador (9x3 slots)
        drawSlotGrid(context, leftPos + 12, topPos + 139, 9, 3);

        // 5. Rejilla de la Barra Rápida / Hotbar (9x1 slots)
        drawSlotGrid(context, leftPos + 12, topPos + 197, 9, 1);

        // 6. Carril de Scrollbar
        drawScrollbarTrack(context, leftPos + 176, topPos + 38, 10, 72);
    }
}
