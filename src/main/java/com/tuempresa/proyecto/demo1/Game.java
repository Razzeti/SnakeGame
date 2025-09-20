package com.tuempresa.proyecto.demo1;

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
                try {
                    new GameServer().start();
                } catch (IOException e) {
                    System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (args[0].equalsIgnoreCase("client")) {
                try {
                    new GameClient().start();
                } catch (IOException e) {
                    System.err.println("No se pudo conectar al servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            startSinglePlayerGame();
        }
    }

    private static void startSinglePlayerGame() {
        // --- CONFIGURACIÓN INICIAL ---
        final int ANCHO_TABLERO = 30;
        final int ALTO_TABLERO = 20;
        final int TICK_RATE_MS = 150;

        // --- INICIALIZACIÓN DE OBJETOS ---
        GameState estado = new GameState(ANCHO_TABLERO, ALTO_TABLERO);
        Snake jugador1 = new Snake("JugadorA", new Coordenada(10, 10));
        estado.getSerpientes().add(jugador1);
        GameLogic.generarFruta(estado);

        java.util.concurrent.ConcurrentHashMap<String, Direccion> acciones = new java.util.concurrent.ConcurrentHashMap<>();

        AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);

        GameLogic gameLogic = new GameLogic();

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
                    }
                }

                GameStateSnapshot snapshot = estado.snapshot().toSnapshotDto();
                SwingUtilities.invokeLater(() -> {
                    view.actualizarEstado(snapshot);
                    view.repaint();
                });
            };

            scheduler.scheduleAtFixedRate(tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);

            view.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    scheduler.shutdownNow();
                }
            });
        });
    }
}