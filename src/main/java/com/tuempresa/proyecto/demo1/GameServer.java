package com.tuempresa.proyecto.demo1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
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

    private static final int PLAYER_PORT = 12345;
    private static final int ADMIN_PORT = 12346;
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

    public void start() {
        Logger.info("Iniciando servidor...");
        ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
        gameLoop.scheduleAtFixedRate(this::tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);

        // Iniciar hilo para escuchar conexiones de administradores
        Thread adminListenerThread = new Thread(this::listenForAdmins);
        adminListenerThread.start();

        // El hilo principal se encarga de escuchar a los jugadores. Esto bloquea.
        listenForPlayers();
    }

    private void listenForPlayers() {
        try (ServerSocket serverSocket = new ServerSocket(PLAYER_PORT)) {
            Logger.info("Servidor escuchando jugadores en el puerto " + PLAYER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Logger.info("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            Logger.error("No se pudo iniciar el listener de jugadores en el puerto " + PLAYER_PORT, e);
        }
    }

    private void listenForAdmins() {
        try (ServerSocket serverSocket = new ServerSocket(ADMIN_PORT)) {
            Logger.info("Servidor escuchando administradores en el puerto " + ADMIN_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Logger.info("Nuevo administrador conectado: " + clientSocket.getInetAddress());
                new Thread(new AdminClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            Logger.error("No se pudo iniciar el listener de administradores en el puerto " + ADMIN_PORT, e);
        }
    }

    private void tick() {
        // La lógica del juego solo se procesa si el juego está EN PROGRESO.
        if (gameState.getGamePhase() == GamePhase.IN_PROGRESS) {
            gameLogic.actualizar(gameState, accionesDeJugadores);

            // Si no quedan serpientes, el juego termina.
            if (gameState.getSerpientes().isEmpty()) {
                Logger.info("Juego terminado. Todas las serpientes eliminadas.");
                gameState.setGamePhase(GamePhase.GAME_ENDED);
                gameState.setJuegoActivo(false); // Se mantiene por consistencia, podría eliminarse a futuro.
            }
        }
        // El estado se transmite siempre, para que los clientes (y admins) vean el estado actual.
        broadcastGameState();
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
                    Logger.warn("Error al enviar estado al cliente. Eliminando cliente.");
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
                    Logger.info("Jugador " + playerId + " se ha unido al juego.");
                }


                while (true) {
                    try {
                        Direccion dir = (Direccion) in.readObject();
                        accionesDeJugadores.put(playerId, dir);
                    } catch (ClassNotFoundException e) {
                        Logger.error("Error al leer la dirección del cliente " + playerId, e);
                        break;
                    }
                }
            } catch (IOException e) {
                Logger.warn("Conexión perdida con el cliente: " + clientSocket.getInetAddress());
            } finally {
                if (out != null) {
                    clientOutputStreams.remove(out);
                }
                if (playerId != null) {
                    synchronized (gameState) {
                        gameState.getSerpientes().removeIf(s -> s.getIdJugador().equals(playerId));
                        accionesDeJugadores.remove(playerId);
                        Logger.info("Jugador " + playerId + " ha sido eliminado del juego.");
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

    private synchronized String handleAdminCommand(String command) {
        if (command == null) {
            return "Error: Comando nulo.";
        }
        Logger.debug("Procesando comando de admin: " + command);
        switch (command.toUpperCase()) {
            case "START_GAME":
                if (gameState.getGamePhase() == GamePhase.IN_PROGRESS) {
                    Logger.warn("Intento de iniciar un juego que ya está en progreso.");
                    return "Error: El juego ya está en progreso.";
                }
                Logger.info("El juego ha sido iniciado por un administrador.");
                gameState.setGamePhase(GamePhase.IN_PROGRESS);
                gameState.setJuegoActivo(true);
                return "Juego iniciado.";

            case "RESET_GAME":
                Logger.info("El juego ha sido reseteado por un administrador.");
                gameState.setGamePhase(GamePhase.WAITING_FOR_PLAYERS);
                gameState.setJuegoActivo(false); // Por consistencia
                gameState.getFrutas().clear();
                GameLogic.generarFruta(gameState);

                // Resetear estado de las serpientes existentes
                int i = 0;
                for (Snake snake : gameState.getSerpientes()) {
                    snake.reset(new Coordenada(10 + i * 2, 10));
                    accionesDeJugadores.put(snake.getIdJugador(), Direccion.DERECHA);
                    i++;
                }
                return "Juego reseteado. Esperando jugadores.";

            case "LIST_PLAYERS":
                if (gameState.getSerpientes().isEmpty()) {
                    return "No hay jugadores conectados.";
                }
                StringBuilder playerList = new StringBuilder("Jugadores conectados:\n");
                for (Snake snake : gameState.getSerpientes()) {
                    playerList.append(String.format("- %s (Puntaje: %d)\n", snake.getIdJugador(), snake.getPuntaje()));
                }
                return playerList.toString();

            case "SHUTDOWN":
                Logger.warn("Comando de apagado recibido. El servidor se cerrará.");
                // Esta es una forma abrupta. Una implementación más robusta
                // cerraría los sockets y los hilos de forma ordenada.
                System.exit(0);
                return "Servidor apagándose..."; // Esto probablemente no se envíe.

            default:
                return "Error: Comando desconocido '" + command + "'.";
        }
    }

    private class AdminClientHandler implements Runnable {
        private Socket clientSocket;

        public AdminClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                out.println("Conexión de administración establecida. Bienvenido.");

                String command;
                while ((command = in.readLine()) != null) {
                    String response = handleAdminCommand(command);
                    out.println(response);
                }

            } catch (IOException e) {
                Logger.warn("Conexión perdida con el cliente de admin: " + clientSocket.getInetAddress());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignorar
                }
            }
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
