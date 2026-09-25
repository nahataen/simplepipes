package com.example.simplepipes.gui;

import com.example.simplepipes.red.ExplorerChestPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla (GUI Client) para el Cofre Explorador.
 *
 * Render chain en AbstractContainerScreen 26.1.2:
 *   extractRenderState()
 *     → extractContents()                    ← main method
 *         → super.extractRenderState()       ← background, widgets (EditBox), etc
 *         → translate(leftPos, topPos)
 *         → extractLabels()
 *         → extractSlotHighlightBack()
 *         → extractSlots()                   ← player inventory items
 *         → extractSlotHighlightFront()
 *     → extractCarriedItem()
 *     → extractSnapbackItem()
 *     → extractTooltip()
 *
 * Estrategia: NO sobreescribir extractContents. En su lugar,
 * sobreescribir extractRenderState para añadir nuestros ítems de red
 * DESPUÉS de que extractContents complete todo su trabajo (fondo, widgets, slots).
 */
public class ExplorerChestScreen extends AbstractContainerScreen<ExplorerChestMenu> {

    private EditBox searchBox;
    private static final List<ExplorerChestPayloads.ItemEntry> NETWORK_ITEMS = new ArrayList<>();

    private int scrollOffset = 0;
    private static final int COLUMNS = 9;
    private static final int ROWS    = 4;
    private static final int VISIBLE_SLOTS = COLUMNS * ROWS;

    // Coordenadas de la rejilla de la red (relativas a leftPos/topPos)
    private static final int GRID_OFFSET_X = 13;
    private static final int GRID_OFFSET_Y = 38;

    public ExplorerChestScreen(ExplorerChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 195, 222);
    }

    public static void updateNetworkItems(List<ExplorerChestPayloads.ItemEntry> items) {
        NETWORK_ITEMS.clear();
        NETWORK_ITEMS.addAll(items);
    }

    public void updateNetworkPosAndItems(net.minecraft.core.BlockPos pos, List<ExplorerChestPayloads.ItemEntry> items) {
        if (pos != null && !pos.equals(net.minecraft.core.BlockPos.ZERO)) {
            this.menu.setExplorerPos(pos);
        }
        NETWORK_ITEMS.clear();
        NETWORK_ITEMS.addAll(items);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inicialización
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        int searchX = this.leftPos + 13;
        int searchY = this.topPos + 18;

        this.searchBox = new EditBox(this.font, searchX, searchY, 168, 14, Component.literal(""));
        this.searchBox.setHint(Component.literal("Buscar ítem..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setCanLoseFocus(true);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Eventos de teclado – redirigir al searchBox si está enfocado
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(event)) return true;
            int key = event.key();
            if (key != 256) { // 256 = Escape key
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (this.searchBox != null && this.searchBox.charTyped(event)) return true;
        return super.charTyped(event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER – extractContents: punto correcto de inyección del fondo
    //
    // ¿Por qué aquí y no en extractMenuBackground?
    //   AbstractContainerScreen.isInGameUi() retorna true.
    //   Screen.extractBackground() ve isInGameUi()==true y salta directamente
    //   a extractTransparentBackground() (el gradiente oscuro sobre el mundo).
    //   NUNCA llama a extractMenuBackground(). Por lo tanto, cualquier override
    //   de extractMenuBackground es código muerto para pantallas in-game.
    //
    //   extractContents() es el método donde AbstractContainerScreen dibuja
    //   todo el contenido: primero llama super.extractRenderState() (widgets),
    //   luego translate(leftPos,topPos), extractLabels, extractSlots, etc.
    //   Insertamos nuestro fondo ANTES de super para que quede debajo de todo.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 1. Dibujar el fondo procedural del panel (ANTES de widgets/labels/slots)
        ExplorerChestGuiRenderer.renderFullGuiBackground(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        // 2. Dejar que AbstractContainerScreen haga el resto:
        //    super.extractRenderState() → widgets (EditBox)
        //    translate → extractLabels → extractSlots → etc.
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER – extractRenderState: se ejecuta DESPUÉS de extractContents
    // Aquí añadimos los ítems de la red encima de todo lo demás
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Primero, dejar que AbstractContainerScreen haga todo su trabajo:
        // extractContents (que llama a Screen.extractRenderState → background + widgets),
        // extractLabels, extractSlots (player inv), extractCarriedItem, etc.
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Ahora dibujar nuestros ítems de la red encima
        renderNetworkItems(g, mouseX, mouseY);
    }

    private void renderNetworkItems(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<ExplorerChestPayloads.ItemEntry> filtered = getFilteredItems();
        int startX = this.leftPos + GRID_OFFSET_X;
        int startY = this.topPos  + GRID_OFFSET_Y;
        int index  = scrollOffset * COLUMNS;

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int slotIdx = index + i;
            if (slotIdx >= filtered.size()) break;

            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * 18;
            int y = startY + row * 18;

            ExplorerChestPayloads.ItemEntry entry = filtered.get(slotIdx);
            ItemStack stack = entry.stack();

            // Hover highlight
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                g.fill(x, y, x + 16, y + 16, 0x55FFFFFF);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(stack.getHoverName());
                tooltip.add(Component.literal("§7En red: §6" + entry.count()));
                tooltip.add(Component.literal("§e[Clic Izq]§7 Stack  §e[Clic Der]§7 1 ítem"));
                tooltip.add(Component.literal("§e[Shift+Clic]§7 Al inventario"));
                g.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }

            g.item(stack, x, y);

            String countStr = formatCount(entry.count());
            g.text(this.font, countStr, x + 16 - this.font.width(countStr), y + 9, 0xFFFFFF, true);
        }

        // Scrollbar thumb
        int maxRows = (int) Math.ceil((double) filtered.size() / COLUMNS);
        if (maxRows > ROWS) {
            int trackH = 72;
            int thumbH = Math.max(12, (int) ((float) ROWS / maxRows * trackH));
            int maxScroll = maxRows - ROWS;
            int thumbY = this.topPos + 38 + (int) ((float) scrollOffset / maxScroll * (trackH - thumbH));
            int thumbX = this.leftPos + 176;

            g.fill(thumbX, thumbY, thumbX + 9, thumbY + thumbH, 0xFFD4AF37);
            g.fill(thumbX + 1, thumbY + 1, thumbX + 8, thumbY + thumbH - 1, 0xFF8C6919);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Labels – se llaman desde extractContents con translate ya aplicado
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, Component.literal("◈ Cofre Explorador"), 13, 5, 0xFFD700, true);
        g.text(this.font, Component.literal("Inventario"), 13, 127, 0xBBBBBB, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mouse – coordenadas ya escaladas por MC
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean synthetic) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        int gridMinX = this.leftPos + 12;
        int gridMaxX = this.leftPos + 175;
        int gridMinY = this.topPos + 37;
        int gridMaxY = this.topPos + 110;

        // Si se hace clic dentro del panel/rejilla de la red
        if (mx >= gridMinX && mx < gridMaxX && my >= gridMinY && my < gridMaxY) {
            ItemStack carried = this.menu.getCarried();

            // CASO 1: El jugador tiene un ítem en el cursor -> DEPOSITAR en la red
            if (!carried.isEmpty()) {
                int amount = (button == 1) ? 1 : carried.getCount();
                ClientPlayNetworking.send(new ExplorerChestPayloads.DepositCarriedItemPayload(
                        this.menu.getExplorerPos(),
                        amount
                ));
                return true;
            }

            // CASO 2: El cursor está vacío -> EXTRAER ítem de la red
            int startX = this.leftPos + GRID_OFFSET_X;
            int startY = this.topPos  + GRID_OFFSET_Y;
            List<ExplorerChestPayloads.ItemEntry> filtered = getFilteredItems();
            int index = scrollOffset * COLUMNS;

            for (int i = 0; i < VISIBLE_SLOTS; i++) {
                int slotIdx = index + i;
                if (slotIdx >= filtered.size()) break;

                int col = i % COLUMNS;
                int row = i / COLUMNS;
                int x = startX + col * 18;
                int y = startY + row * 18;

                if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                    ExplorerChestPayloads.ItemEntry entry = filtered.get(slotIdx);
                    boolean hasShift = (event.modifiers() & 1) != 0;
                    int amount = (button == 1) ? 1 : 64;

                    ClientPlayNetworking.send(new ExplorerChestPayloads.ExtractItemPayload(
                            this.menu.getExplorerPos(),
                            entry.stack(),
                            amount,
                            hasShift
                    ));
                    return true;
                }
            }
        }

        if (this.searchBox != null && !this.searchBox.isMouseOver(mx, my)) {
            this.searchBox.setFocused(false);
        }

        return super.mouseClicked(event, synthetic);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        List<ExplorerChestPayloads.ItemEntry> filtered = getFilteredItems();
        int maxRows = (int) Math.ceil((double) filtered.size() / COLUMNS);

        if (vAmount < 0 && (scrollOffset + ROWS) < maxRows) {
            scrollOffset++;
            return true;
        } else if (vAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<ExplorerChestPayloads.ItemEntry> getFilteredItems() {
        String q = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase() : "";
        if (q.isEmpty()) return NETWORK_ITEMS;
        List<ExplorerChestPayloads.ItemEntry> result = new ArrayList<>();
        for (ExplorerChestPayloads.ItemEntry e : NETWORK_ITEMS)
            if (e.stack().getHoverName().getString().toLowerCase().contains(q))
                result.add(e);
        return result;
    }

    private void drawOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private static String formatCount(long count) {
        if (count < 1_000)     return String.valueOf(count);
        if (count < 1_000_000) return String.format("%.1fk", count / 1_000.0);
        return String.format("%.1fM", count / 1_000_000.0);
    }
}
