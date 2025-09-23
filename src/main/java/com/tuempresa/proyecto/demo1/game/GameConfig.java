package com.tuempresa.proyecto.demo1.game;

import java.awt.Color;
import java.awt.Font;
import com.tuempresa.proyecto.demo1.model.Coordenada;

public final class GameConfig {

    // Game configuration
    public static final int ANCHO_TABLERO = 30;
    public static final int ALTO_TABLERO = 20;
    public static final int MILIS_POR_TICK = 150; // 150ms por tick

    // Network configuration
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 12350;
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

    // Performance Metrics
    public static final boolean ENABLE_PERFORMANCE_METRICS = true;
    public static final long SERVER_TICK_WARNING_THRESHOLD_MS = MILIS_POR_TICK;

    // Game Element Codes (from former Constants.java)
    public static final byte VACIO = 0;
    public static final byte FRUTA_BASE = 2;
    public static final byte SNAKE_HEAD = 10;
    public static final byte SNAKE_BODY = 11;

    // Fruit Configuration
    public static final int FRUTAS_MINIMAS = 1;
    public static final int FRUTAS_POR_JUGADOR_DIVISOR = 2; // Generar 1 fruta extra por cada 2 jugadores.
    public static final int PROBABILIDAD_FRUTA_NORMAL = 70; // 70% de probabilidad para la fruta de 1 punto
    public static final int PROBABILIDAD_FRUTA_BUENA = 20;  // 20% de probabilidad para la fruta de 2 puntos
    // El 10% restante es para la fruta excelente (3 puntos)

    private GameConfig() {
        // Private constructor to prevent instantiation
    }
}
