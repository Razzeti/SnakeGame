package com.tuempresa.proyecto.demo1.game;

import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.model.GameState;
import com.tuempresa.proyecto.demo1.model.Snake;
import com.tuempresa.proyecto.demo1.net.GameClient;
import com.tuempresa.proyecto.demo1.net.GameServer;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.ui.GraphicalView;
import com.tuempresa.proyecto.demo1.util.Logger;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Game {

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("server")) {
                Logger.setLogFile(GameConfig.LOG_FILE_SERVER);
                Logger.info("Iniciando en modo servidor...");
                new GameServer().start();
            } else if (args[0].equalsIgnoreCase("client")) {
                Logger.setLogFile(GameConfig.LOG_FILE_CLIENT);
                Logger.info("Iniciando en modo cliente...");
                try {
                    new GameClient().start();
                } catch (IOException e) {
                    Logger.error("No se pudo conectar al servidor: " + e.getMessage(), e);
                }
            }
        } else {
            Logger.info("Iniciando en modo un jugador...");
            startSinglePlayerGame();
        }
    }

    private static void startSinglePlayerGame() {
        // --- CONFIGURACIÓN INICIAL ---
        // Ahora se usan los valores de GameConfig
        // --- INICIALIZACIÓN DE OBJETOS ---
        GameState estado = new GameState(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO);
        Snake jugador1 = new Snake("JugadorA", GameConfig.POSICION_INICIAL_JUGADOR_1);
        estado.getSerpientes().add(jugador1);

        java.util.concurrent.ConcurrentHashMap<String, Direccion> acciones = new java.util.concurrent.ConcurrentHashMap<>();

        AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);

        GameLogic gameLogic = new GameLogic();
        gameLogic.generarFruta(estado);

        // --- CREACIÓN DE LA INTERFAZ GRÁFICA ---
        SwingUtilities.invokeLater(() -> {
            GameStateSnapshot initialSnapshot = estado.snapshot().toSnapshotDto();
            GraphicalView view = new GraphicalView(initialSnapshot, direccionActual);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GameLoop");
                t.setDaemon(true);
                return t;
            });

            Runnable tick = () -> {
                if (estado.isJuegoActivo()) {
                    acciones.put("JugadorA", direccionActual.get());
                    gameLogic.actualizar(estado, acciones);
                    if (estado.getSerpientes().isEmpty()) {
                        estado.setJuegoActivo(false);
                        Logger.info("Juego en modo un jugador terminado.");
                    }
                }

                GameStateSnapshot snapshot = estado.snapshot().toSnapshotDto();
                SwingUtilities.invokeLater(() -> {
                    view.actualizarEstado(snapshot);
                    view.repaint();
                });
            };

            scheduler.scheduleAtFixedRate(tick, 0, GameConfig.MILIS_POR_TICK, TimeUnit.MILLISECONDS);

            view.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    scheduler.shutdownNow();
                    Logger.info("Ventana de juego cerrada, finalizando loop de un jugador.");
                }
            });
        });
    }
}