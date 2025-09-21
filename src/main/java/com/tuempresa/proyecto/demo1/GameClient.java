package com.tuempresa.proyecto.demo1;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class GameClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

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

    public void start() throws IOException {
        Logger.info("Iniciando cliente...");
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
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


        // Bucle principal para recibir el estado del juego y actualizar la vista
        try {
            while (true) {
                GameStateSnapshot snapshot = (GameStateSnapshot) in.readObject();
                SwingUtilities.invokeLater(() -> {
                    if (view != null) {
                        view.actualizarEstado(snapshot);
                        view.repaint();
                    }
                });
            }
        } catch (ClassNotFoundException | IOException e) {
            Logger.warn("Conexión perdida con el servidor.");
            // e.printStackTrace(); // Optional: might be noisy if client just closes
        } finally {
            Logger.info("Cliente desconectado.");
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                // ignore
            }
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
        return new GameStateSnapshot(30, 20,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                true,
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
}
