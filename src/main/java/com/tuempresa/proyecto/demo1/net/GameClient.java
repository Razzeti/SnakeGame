package com.tuempresa.proyecto.demo1.net;

import com.tuempresa.proyecto.demo1.game.GameConfig;
import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.model.GamePhase;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.ui.GraphicalView;
import com.tuempresa.proyecto.demo1.util.Logger;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GameClient {

    private ScheduledExecutorService pingScheduler;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerId;
    private GraphicalView view;
    private AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);
    private boolean isTestMode = false;
    private CompletableFuture<String> playerIdFuture = new CompletableFuture<>();

    public GameClient() {
    }

    public GameClient(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }

    // Constructor for testing with a custom scheduler
    public GameClient(boolean isTestMode, ScheduledExecutorService pingScheduler) {
        this.isTestMode = isTestMode;
        this.pingScheduler = pingScheduler;
    }

    public void start() throws IOException {
        Logger.info("Iniciando cliente...");
        try {
            socket = new Socket(GameConfig.DEFAULT_HOST, GameConfig.DEFAULT_PORT);
            socket.setTcpNoDelay(true); // OPTIMIZATION: Disable Nagle's Algorithm to reduce latency
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            playerId = (String) in.readObject();
            Logger.info("Conectado como: " + playerId);
            playerIdFuture.complete(playerId);
        } catch (ClassNotFoundException | IOException e) {
            playerIdFuture.completeExceptionally(e);
            throw new IOException("No se pudo leer el ID de jugador del servidor.", e);
        }


        if (!isTestMode) {
            // Crear la vista en el Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                // Se necesita un estado inicial para crear la vista, aunque esté vacío.
                GameStateSnapshot initialState = createEmptySnapshot();
                view = new GraphicalView(initialState, direccionActual, this);
            });
        }

        // Hilo para enviar la dirección al servidor cada vez que cambia
        Thread inputSenderThread = new Thread(this::sendInputLoop);
        inputSenderThread.setDaemon(true);
        inputSenderThread.start();

        // Iniciar el programador de pings si las métricas están habilitadas
        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            if (pingScheduler == null) { // Only create if not injected for testing
                pingScheduler = Executors.newSingleThreadScheduledExecutor();
                pingScheduler.scheduleAtFixedRate(this::sendPing, 0, 5, TimeUnit.SECONDS);
            }
        }

        // Bucle principal para recibir el estado del juego y actualizar la vista
        try {
            while (true) {
                Object receivedObject = in.readObject();
                if (receivedObject instanceof Packet) {
                    byte[] snapshotBytes = ((Packet) receivedObject).data;
                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(snapshotBytes);
                         java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {

                        GameStateSnapshot snapshot = (GameStateSnapshot) ois.readObject();
                        SwingUtilities.invokeLater(() -> {
                            if (view != null) {
                                view.actualizarEstado(snapshot);
                                view.repaint();
                            }
                        });
                    } catch (java.io.IOException | ClassNotFoundException e) {
                        Logger.warn("Error deserializing game state packet", e);
                    }
                } else if (receivedObject instanceof String) {
                    String command = (String) receivedObject;
                    if (command.startsWith("PONG;")) {
                        long originalTimestamp = Long.parseLong(command.substring(5));
                        long rtt = System.currentTimeMillis() - originalTimestamp;
                        Logger.info(String.format("[METRIC] Network Latency (RTT): %d ms", rtt));
                    }
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            Logger.warn("Conexión perdida con el servidor.");
            // e.printStackTrace(); // Optional: might be noisy if client just closes
        } finally {
            Logger.info("Cliente desconectado.");
            if (pingScheduler != null) {
                pingScheduler.shutdownNow();
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void sendPing() {
        try {
            if (out != null) {
                String pingMessage = "PING;" + System.currentTimeMillis();
                out.writeObject(pingMessage);
                out.flush();
            }
        } catch (IOException e) {
            Logger.warn("Failed to send ping to server.", e);
        }
    }

    private void sendInputLoop() {
        Direccion lastSentDirection = null;
        try {
            while (!socket.isClosed()) {
                Direccion currentDirection = direccionActual.get();
                if (!currentDirection.equals(lastSentDirection)) {
                    out.writeObject(currentDirection);
                    out.flush();
                    lastSentDirection = currentDirection;
                }
                Thread.sleep(50); // Enviar actualizaciones de input 20 veces por segundo
            }
        } catch (IOException | InterruptedException e) {
            // El hilo terminará si hay un error o se interrumpe
        }
    }

    // Método para crear un snapshot inicial vacío
    private GameStateSnapshot createEmptySnapshot() {
        return new GameStateSnapshot(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                GamePhase.WAITING_FOR_PLAYERS); // <-- VALOR INICIAL
    }

    public static void main(String[] args) {
        try {
            new GameClient().start();
        } catch (IOException e) {
            Logger.error("No se pudo conectar al servidor", e);
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public CompletableFuture<String> getPlayerIdFuture() {
        return playerIdFuture;
    }

    public void setDirection(Direccion direction) {
        this.direccionActual.set(direction);
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
