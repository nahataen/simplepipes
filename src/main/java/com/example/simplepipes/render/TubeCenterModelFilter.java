package com.example.simplepipes.render;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Clase independiente altamente optimizada (0-garbage, O(1) performance)
 * para filtrar la visibilidad de las 6 caras de tube_center.json (NORTH, SOUTH,
 * EAST, WEST, UP, DOWN) según las conexiones activas con otros bloques.
 *
 * Lógica:
 * - Si la tubería está conectada a otro bloque en la dirección dada (propiedad = true),
 *   la cara correspondiente se OCULTA.
 * - Si NO está conectada en esa dirección (propiedad = false), la cara se MUESTRA.
 */
public final class TubeCenterModelFilter {

    private TubeCenterModelFilter() {} // Evitar instanciación innecesaria

    // Arreglo estático indexado directamente por ordinal de Direction (O(1), 0 asignaciones de memoria)
    private static final BooleanProperty[] DIRECTION_PROPERTIES = new BooleanProperty[6];

    static {
        DIRECTION_PROPERTIES[Direction.DOWN.ordinal()]  = BlockStateProperties.DOWN;
        DIRECTION_PROPERTIES[Direction.UP.ordinal()]    = BlockStateProperties.UP;
        DIRECTION_PROPERTIES[Direction.NORTH.ordinal()] = BlockStateProperties.NORTH;
        DIRECTION_PROPERTIES[Direction.SOUTH.ordinal()] = BlockStateProperties.SOUTH;
        DIRECTION_PROPERTIES[Direction.WEST.ordinal()]  = BlockStateProperties.WEST;
        DIRECTION_PROPERTIES[Direction.EAST.ordinal()]  = BlockStateProperties.EAST;
    }

    /**
     * Determina de forma ultrarrápida si la cara en una dirección dada debe mostrarse.
     *
     * @param state El BlockState actual de la tubería.
     * @param direction La dirección de la cara (DOWN, UP, NORTH, SOUTH, WEST, EAST).
     * @return true si la cara debe ser visible (no conectada), false si debe ocultarse (conectada).
     */
    public static boolean shouldShowFace(BlockState state, Direction direction) {
        if (state == null || direction == null) {
            return true;
        }
        BooleanProperty prop = DIRECTION_PROPERTIES[direction.ordinal()];
        return !state.hasProperty(prop) || !state.getValue(prop);
    }

    /**
     * Métodos directos para cada dirección específica (para conveniencia y máxima velocidad):
     */
    public static boolean shouldShowNorth(BlockState state) {
        return shouldShowFace(state, Direction.NORTH);
    }

    public static boolean shouldShowSouth(BlockState state) {
        return shouldShowFace(state, Direction.SOUTH);
    }

    public static boolean shouldShowEast(BlockState state) {
        return shouldShowFace(state, Direction.EAST);
    }

    public static boolean shouldShowWest(BlockState state) {
        return shouldShowFace(state, Direction.WEST);
    }

    public static boolean shouldShowUp(BlockState state) {
        return shouldShowFace(state, Direction.UP);
    }

    public static boolean shouldShowDown(BlockState state) {
        return shouldShowFace(state, Direction.DOWN);
    }

    /**
     * Devuelve una máscara de bits (6 bits) representando las caras visibles.
     * Bit set = cara visible.
     */
    public static int getVisibleFacesMask(BlockState state) {
        if (state == null) return 0b111111;
        int mask = 0;
        for (int i = 0; i < 6; i++) {
            BooleanProperty prop = DIRECTION_PROPERTIES[i];
            if (!state.hasProperty(prop) || !state.getValue(prop)) {
                mask |= (1 << i);
            }
        }
        return mask;
            }
}
