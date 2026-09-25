package com.example.simplepipes.red;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.List;

/**
 * Representa el resultado de una búsqueda de ruta exitosa en la red logística.
 * SIN CAMBIOS - esta clase no tiene dependencias de NeoForge.
 */
public class ResultadoRuta {
    public final BlockPos destination;
    public final List<BlockPos> path;
    public final Direction insertSide;
    public final int maxSpace;

    public ResultadoRuta(BlockPos destination, List<BlockPos> path, Direction insertSide, int maxSpace) {
        this.destination = destination;
        this.path = path;
        this.insertSide = insertSide;
        this.maxSpace = maxSpace;
    }
}
