package com.tuempresa.proyecto.demo1;

import javax.swing.SwingUtilities;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    public GameClient() {
    }

    public GameClient(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }

    public void start() throws IOException {
        System.out.println("Iniciando cliente...");
        socket = new Socket(SERVER_IP, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        try {
            playerId = (String) in.readObject();
            System.out.println("Conectado como: " + playerId);
        } catch (ClassNotFoundException e) {
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
                Object obj = in.readObject();
                if (obj instanceof byte[]) {
                    GameStateSnapshot snapshot = deserialize((byte[]) obj);
                    if (snapshot != null) {
                        SwingUtilities.invokeLater(() -> {
                            if (view != null) {
                                view.actualizarEstado(snapshot);
                                view.repaint();
                            }
                        });
                    }
                } else if (obj instanceof Ping) {
                    // Received a ping request, send it back immediately
                    out.writeObject(obj);
                    out.flush();
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Conexión perdida con el servidor.");
            e.printStackTrace();
        } finally {
            socket.close();
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
                true);
    }

    private GameStateSnapshot deserialize(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (GameStateSnapshot) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error deserializing game state: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            new GameClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}
