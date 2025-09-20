// Game.java (versión correcta para Swing)
package com.tuempresa.proyecto.demo1;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Game {

    public static void main(String[] args) {
        // --- CONFIGURACIÓN INICIAL ---
        final int ANCHO_TABLERO = 30;
        final int ALTO_TABLERO = 20;
        final int TICK_RATE_MS = 150;

        // --- INICIALIZACIÓN DE OBJETOS ---
        GameState estado = new GameState(ANCHO_TABLERO, ALTO_TABLERO);
        Snake jugador1 = new Snake("JugadorA", new Coordenada(10, 10));
        estado.getSerpientes().add(jugador1);
        GameController.generarFruta(estado);

        java.util.concurrent.ConcurrentHashMap<String, Direccion> acciones = new java.util.concurrent.ConcurrentHashMap<>();

        AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);

        // --- CREACIÓN DE LA INTERFAZ GRÁFICA ---
        // Se usa invokeLater para asegurar que la GUI se cree en el hilo correcto (EDT).
        SwingUtilities.invokeLater(() -> {
            // Create initial snapshot DTO for safe rendering
            GameStateSnapshot initialSnapshot = estado.snapshot().toSnapshotDto();
            GraphicalView view = new GraphicalView(initialSnapshot, direccionActual);

            // Use a ScheduledExecutorService for the game loop
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GameLoop");
                t.setDaemon(true);
                return t;
            });

            Runnable tick = () -> {
                if (estado.isJuegoActivo()) {
                    acciones.put("JugadorA", direccionActual.get());
                    GameController.actualizar(estado, acciones);
                    if (estado.getSerpientes().isEmpty()) {
                        estado.setJuegoActivo(false);
                    }
                }

                // Create immutable DTO snapshot and update UI on EDT
                GameStateSnapshot snapshot = estado.snapshot().toSnapshotDto();
                SwingUtilities.invokeLater(() -> {
                    view.actualizarEstado(snapshot);
                    view.repaint();
                });
            };

            scheduler.scheduleAtFixedRate(tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);

            // Ensure scheduler shutdown when window closes
            view.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    scheduler.shutdownNow();
                }
            });
        });
    }
}