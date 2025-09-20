package com.tuempresa.proyecto.demo1;

public final class BoardRenderer {
    public static byte[][] toByteBoard(GameState estado) {
        byte[][] tablero = new byte[estado.getTablero().length][estado.getTablero()[0].length];
        // llenar y mapear frutas/serpientes a bytes (usar Constantes)
        return tablero;
    }
}
