package com.tuempresa.proyecto.demo1.ui;

import com.tuempresa.proyecto.demo1.model.GameState;

public final class BoardRenderer {
    public static byte[][] toByteBoard(GameState estado) {
        byte[][] tablero = new byte[estado.getTablero().length][estado.getTablero()[0].length];
        // llenar y mapear frutas/serpientes a bytes (usar Constantes)
        return tablero;
    }
}
