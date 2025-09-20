package com.tuempresa.proyecto.demo1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {

    private static final int PORT = 12345;
    private static final int TICK_RATE_MS = 150;
    private static final int ANCHO_TABLERO = 30;
    private static final int ALTO_TABLERO = 20;

    private GameState gameState;
    private GameLogic gameLogic;
    private ConcurrentHashMap<String, Direccion> accionesDeJugadores;
    private Set<ObjectOutputStream> clientOutputStreams = Collections.synchronizedSet(new HashSet<>());

    public GameServer() {
        gameState = new GameState(ANCHO_TABLERO, ALTO_TABLERO);
        gameLogic = new GameLogic();
        accionesDeJugadores = new ConcurrentHashMap<>();
        GameLogic.generarFruta(gameState);
    }

    public void start() throws IOException {
        System.out.println("Iniciando servidor...");
        ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
        gameLoop.scheduleAtFixedRate(this::tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }

    private ScheduledExecutorService restartScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean restarting = false;

    private void tick() {
        if (gameState.isJuegoActivo()) {
            gameLogic.actualizar(gameState, accionesDeJugadores);
            if (gameState.getSerpientes().isEmpty() && !restarting) {
                gameState.setJuegoActivo(false);
                restarting = true;
                restartScheduler.schedule(this::restartGame, 5, TimeUnit.SECONDS);
            }
        }
        broadcastGameState();
    }

    private void restartGame() {
        synchronized (gameState) {
            System.out.println("Reiniciando juego...");
            accionesDeJugadores.clear();
            gameState.getSerpientes().clear();
            gameState.getFrutas().clear();

            // Re-create snakes for all currently connected players
            synchronized(clientOutputStreams) {
                // This is a bit of a hack, we should probably have a proper player list
                int i = 0;
                for (ObjectOutputStream out : clientOutputStreams) {
                    String playerId = "Jugador_" + (i + 1);
                    Snake newSnake = new Snake(playerId, new Coordenada(10 + i*2, 10));
                    gameState.getSerpientes().add(newSnake);
                    accionesDeJugadores.put(playerId, Direccion.DERECHA);
                    i++;
                }
            }

            GameLogic.generarFruta(gameState);
            gameState.setJuegoActivo(true);
            restarting = false;
        }
    }

    private void broadcastGameState() {
        GameStateSnapshot snapshot = gameState.snapshot().toSnapshotDto();
        synchronized (clientOutputStreams) {
            clientOutputStreams.removeIf(out -> {
                try {
                    out.writeObject(snapshot);
                    out.reset(); // Importante para asegurar que se envía el estado actualizado
                    return false;
                } catch (IOException e) {
                    System.err.println("Error al enviar estado al cliente. Eliminando cliente.");
                    return true; // Eliminar este stream si hay un error
                }
            });
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private String playerId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                synchronized (gameState) {
                    playerId = "Jugador_" + (gameState.getSerpientes().size() + 1);
                    Snake newSnake = new Snake(playerId, new Coordenada(10, 10));
                    gameState.getSerpientes().add(newSnake);
                    accionesDeJugadores.put(playerId, Direccion.DERECHA);
                    out.writeObject(playerId);
                    out.flush();
                }


                while (true) {
                    try {
                        Direccion dir = (Direccion) in.readObject();
                        accionesDeJugadores.put(playerId, dir);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Error al leer la dirección del cliente.");
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexión perdida con el cliente: " + clientSocket.getInetAddress());
            } finally {
                if (out != null) {
                    clientOutputStreams.remove(out);
                }
                if (playerId != null) {
                    synchronized (gameState) {
                        gameState.getSerpientes().removeIf(s -> s.getIdJugador().equals(playerId));
                        accionesDeJugadores.remove(playerId);
                    }
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignorar
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            new GameServer().start();
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
