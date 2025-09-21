package com.tuempresa.proyecto.demo1;

import java.awt.Color;
import java.awt.Font;

public final class GameConfig {

    // Game configuration
    public static final int ANCHO_TABLERO = 30;
    public static final int ALTO_TABLERO = 20;
    public static final int MILIS_POR_TICK = 150; // 150ms por tick

    // Network configuration
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 12345;
    public static final int NETWORK_INPUT_BUFFER_SIZE = 1; // Un solo comando de dirección a la vez
    public static final int NETWORK_GAME_STATE_BUFFER_SIZE = 2; // Doble buffer para el estado del juego

    // Rendering configuration
    public static final int DEFAULT_TILE_SIZE = 25;
    public static final Color COLOR_FONDO = new Color(40, 40, 40);
    public static final Color COLOR_FRUTA = new Color(255, 60, 60);
    public static final Color COLOR_TEXTO = Color.WHITE;
    public static final Font FUENTE_TEXTO = new Font("Monospaced", Font.BOLD, 18);
    public static final Font FUENTE_GRANDE_TEXTO = new Font("Monospaced", Font.BOLD, 72);

    // Snake configuration
    public static final Coordenada POSICION_INICIAL_JUGADOR_1 = new Coordenada(10, 10);
    public static final Coordenada POSICION_INICIAL_JUGADOR_2 = new Coordenada(20, 10);

    // Logging configuration
    public static final String LOG_FILE_SERVER = "server.log";
    public static final String LOG_FILE_CLIENT = "client.log";
    public static final String LOG_FILE_ADMIN = "admin.log";
    public static final boolean LOG_TO_CONSOLE = true;

    private GameConfig() {
        // Private constructor to prevent instantiation
    }
}
