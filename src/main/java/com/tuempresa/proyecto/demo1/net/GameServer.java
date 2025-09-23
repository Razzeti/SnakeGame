package com.tuempresa.proyecto.demo1.net;

import com.tuempresa.proyecto.demo1.game.GameConfig;
import com.tuempresa.proyecto.demo1.game.GameLogic;
import com.tuempresa.proyecto.demo1.model.Coordenada;
import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.model.GamePhase;
import com.tuempresa.proyecto.demo1.model.GameState;
import com.tuempresa.proyecto.demo1.model.Snake;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {

    // Ahora se usan los valores de GameConfig
    private GameState gameState;
    private GameLogic gameLogic;
    private ConcurrentHashMap<String, Direccion> accionesDeJugadores;
    private Set<ObjectOutputStream> clientOutputStreams = Collections.synchronizedSet(new HashSet<>());
    private ScheduledExecutorService gameLoop;
    private ServerSocket playerServerSocket;
    private ServerSocket adminServerSocket;
    private Thread playerListenerThread;
    private Thread adminListenerThread;
    private final Set<AdminClientHandler> adminClientHandlers = Collections.synchronizedSet(new HashSet<>());


    private ConcurrentHashMap<String, com.tuempresa.proyecto.demo1.net.model.ClientMetrics> clientMetrics = new ConcurrentHashMap<>();
    // Lista de posiciones de inicio para los jugadores.
    private static final List<Coordenada> STARTING_POSITIONS = Arrays.asList(
            new Coordenada(10, 5),  // Jugador 1
            new Coordenada(20, 5),  // Jugador 2
            new Coordenada(10, 15), // Jugador 3
            new Coordenada(20, 15)  // Jugador 4
            // Se pueden añadir más posiciones si se espera soportar más jugadores.
    );


    public GameServer() {
        gameState = new GameState(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO);
        gameLogic = new GameLogic();
        accionesDeJugadores = new ConcurrentHashMap<>();
        gameLogic.generarFruta(gameState);
    }

    public void start() {
        Logger.info("Iniciando servidor...");
        gameLoop = Executors.newSingleThreadScheduledExecutor();
        gameLoop.scheduleAtFixedRate(this::tick, 0, GameConfig.MILIS_POR_TICK, TimeUnit.MILLISECONDS);

        // Iniciar hilo para escuchar conexiones de administradores
        adminListenerThread = new Thread(this::listenForAdmins);
        adminListenerThread.start();

        // El hilo principal se encarga de escuchar a los jugadores.
        playerListenerThread = new Thread(this::listenForPlayers);
        playerListenerThread.start();
    }

    private void listenForPlayers() {
        try {
            playerServerSocket = new ServerSocket(GameConfig.DEFAULT_PORT);
            Logger.info("Servidor escuchando jugadores en el puerto " + GameConfig.DEFAULT_PORT);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = playerServerSocket.accept();
                Logger.info("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Logger.error("No se pudo iniciar el listener de jugadores en el puerto " + GameConfig.DEFAULT_PORT, e);
            }
        }
    }

    private void listenForAdmins() {
        int adminPort = GameConfig.DEFAULT_PORT + 1;
        try {
            adminServerSocket = new ServerSocket(adminPort);
            Logger.info("Servidor escuchando administradores en el puerto " + adminPort);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = adminServerSocket.accept();
                Logger.info("Nuevo administrador conectado: " + clientSocket.getInetAddress());
                AdminClientHandler handler = new AdminClientHandler(clientSocket);
                adminClientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Logger.error("No se pudo iniciar el listener de administradores en el puerto " + adminPort, e);
            }
        }
    }

    private void tick() {
        long startTime = 0;
        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            startTime = System.nanoTime();
        }

        if (gameState.getGamePhase() == GamePhase.IN_PROGRESS) {
            gameLogic.actualizar(gameState, accionesDeJugadores);
            if (gameState.getSerpientes().isEmpty()) {
                Logger.info("Juego terminado. Todas las serpientes eliminadas.");
                gameState.setGamePhase(GamePhase.GAME_ENDED);
                gameState.setJuegoActivo(false);
            }
        }
        broadcastGameState();
        broadcastAdminData();

        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            long endTime = System.nanoTime();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            String logMessage = String.format("[METRIC] Server tick duration: %d ms", durationMs);
            if (durationMs > GameConfig.SERVER_TICK_WARNING_THRESHOLD_MS) {
                Logger.warn(logMessage + " - EXCEEDED THRESHOLD");
            } else {
                Logger.info(logMessage);
            }
        }
    }

    private void broadcastGameState() {
        GameStateSnapshot snapshot = gameState.toSnapshotDto();

        // Broadcast to players
        // OPTIMIZATION: Serialize the complex snapshot object into a byte array ONCE.
        byte[] snapshotBytes;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(snapshot);
            snapshotBytes = baos.toByteArray();
        } catch (IOException e) {
            Logger.error("Error serializing game state snapshot", e);
            return; // Cannot broadcast if serialization fails.
        }

        // OPTIMIZATION: Wrap the byte array in a simple Packet object.
        Packet packet = new Packet(snapshotBytes);

        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            Logger.info(String.format("[METRIC] Packet size: %d bytes", snapshotBytes.length));
        }

        // OPTIMIZATION: Now, send the much simpler 'Packet' object to all clients.
        // The serialization cost of this object is trivial, and the reset() call is cheap.
        synchronized (clientOutputStreams) {
            clientOutputStreams.removeIf(out -> {
                try {
                    out.writeObject(packet);
                    out.reset();
                    return false;
                } catch (IOException e) {
                    Logger.warn("Error al enviar estado al cliente. Eliminando cliente.");
                    return true;
                }
            });
        }

        // Broadcast to admins
        synchronized (adminClientHandlers) {
            adminClientHandlers.removeIf(handler -> {
                try {
                    handler.sendGameState(snapshot);
                    return false;
                } catch (Exception e) {
                    Logger.warn("Error sending game state to admin, removing handler.", e);
                    return true;
                }
            });
        }
    }

    private void broadcastAdminData() {
        java.util.List<com.tuempresa.proyecto.demo1.net.dto.PlayerData> playerDataList = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();

        // We need to iterate over the snakes to get the score, but metrics are separate.
        // It's better to build a map of scores first.
        java.util.Map<String, Integer> playerScores = new java.util.HashMap<>();
        synchronized (gameState) {
            for (Snake snake : gameState.getSerpientes()) {
                playerScores.put(snake.getIdJugador(), snake.getPuntaje());
            }
        }


        for (com.tuempresa.proyecto.demo1.net.model.ClientMetrics metrics : clientMetrics.values()) {
            long duration = (now - metrics.getConnectionTimestamp()) / 1000;
            int score = playerScores.getOrDefault(metrics.getPlayerId(), 0);
            String status = metrics.getStatus();

            // If the game has ended, the snake might be gone, but the player is still "connected".
            // We can refine their status here.
            if (gameState.getGamePhase() == GamePhase.GAME_ENDED && status.equals("Alive")) {
                status = "Game Over";
            }

            playerDataList.add(new com.tuempresa.proyecto.demo1.net.dto.PlayerData(
                    metrics.getPlayerId(),
                    metrics.getIpAddress().getHostAddress(),
                    duration,
                    metrics.getLastPingRttMs(),
                    status,
                    score
            ));
        }

        com.tuempresa.proyecto.demo1.net.dto.AdminDataSnapshot adminSnapshot = new com.tuempresa.proyecto.demo1.net.dto.AdminDataSnapshot(playerDataList);

        synchronized (adminClientHandlers) {
            adminClientHandlers.removeIf(handler -> {
                try {
                    handler.sendAdminData(adminSnapshot);
                    return false;
                } catch (Exception e) {
                    Logger.warn("Error sending admin data to admin, removing handler.", e);
                    return true;
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
            com.tuempresa.proyecto.demo1.net.model.ClientMetrics metrics = null;
            try {
                clientSocket.setTcpNoDelay(true); // OPTIMIZATION: Disable Nagle's Algorithm
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Se debe crear el jugador y asignarle un ID ANTES de que pueda recibir updates del juego.
                synchronized (gameState) {
                    int playerIndex = gameState.getSerpientes().size();
                    playerId = "Jugador_" + (playerIndex + 1);

                    // Asignar una posición de la lista, rotando si hay más jugadores que posiciones.
                    Coordenada posInicial = STARTING_POSITIONS.get(playerIndex % STARTING_POSITIONS.size());

                    Snake newSnake = new Snake(playerId, posInicial);
                    gameState.getSerpientes().add(newSnake);
                    accionesDeJugadores.put(playerId, Direccion.DERECHA); // Dirección inicial por defecto

                    metrics = new com.tuempresa.proyecto.demo1.net.model.ClientMetrics(playerId, clientSocket.getInetAddress());
                    clientMetrics.put(playerId, metrics);
                    Logger.info("Jugador " + playerId + " se ha unido al juego en " + posInicial);
                }

                // Enviar el ID al cliente. Esto lo "desbloquea" para empezar a jugar.
                out.writeObject(playerId);
                out.flush();

                // Ahora que el cliente está listo, añadirlo a la lista de broadcast.
                clientOutputStreams.add(out);
                metrics.setStatus("Alive");


                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Object receivedObject = in.readObject();
                        if (receivedObject instanceof Direccion) {
                            accionesDeJugadores.put(playerId, (Direccion) receivedObject);
                        } else if (receivedObject instanceof String) {
                            String command = (String) receivedObject;
                            if (command.startsWith("PING;")) {
                                long pingTimestamp = Long.parseLong(command.substring(5));
                                long rtt = System.currentTimeMillis() - pingTimestamp;
                                if (clientMetrics.containsKey(playerId)) {
                                    clientMetrics.get(playerId).setLastPingRttMs(rtt);
                                }
                                String pongResponse = "PONG;" + command.substring(5);
                                out.writeObject(pongResponse);
                                out.flush();
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        Logger.error("Error al leer objeto del cliente " + playerId, e);
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
                    if (clientMetrics.containsKey(playerId)) {
                        clientMetrics.get(playerId).setStatus("Dead");
                    }
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
        String upperCaseCommand = command.toUpperCase();

        if (upperCaseCommand.startsWith("KICK_PLAYER")) {
            String playerIdToKick = command.substring("KICK_PLAYER".length()).trim();
            if (clientMetrics.containsKey(playerIdToKick)) {
                // This is a bit brutal. A cleaner way would be to send a "you have been kicked"
                // message to the client and have it shut down gracefully.
                // For now, we just remove them from the game. The ClientHandler will eventually
                // fail due to a broken pipe and clean up the socket.
                synchronized (gameState) {
                    gameState.getSerpientes().removeIf(s -> s.getIdJugador().equals(playerIdToKick));
                    accionesDeJugadores.remove(playerIdToKick);
                    clientMetrics.remove(playerIdToKick);
                }
                Logger.info("Admin ha expulsado al jugador: " + playerIdToKick);
                return "Jugador " + playerIdToKick + " ha sido expulsado.";
            } else {
                return "Error: No se encontró al jugador " + playerIdToKick;
            }
        }

        switch (upperCaseCommand) {
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
                gameLogic.generarFruta(gameState);

                // Resetear estado de las serpientes existentes
                int playerIndex = 0;
                for (Snake snake : gameState.getSerpientes()) {
                    // Asignar una posición de la lista, rotando si hay más jugadores que posiciones.
                    Coordenada posInicial = STARTING_POSITIONS.get(playerIndex % STARTING_POSITIONS.size());
                    snake.reset(posInicial);
                    accionesDeJugadores.put(snake.getIdJugador(), Direccion.DERECHA);
                    playerIndex++;
                }
                // Clear metrics for players that might have disconnected during a game over
                clientMetrics.entrySet().removeIf(entry ->
                        gameState.getSerpientes().stream().noneMatch(s -> s.getIdJugador().equals(entry.getKey()))
                );
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
                System.exit(0);
                return "Servidor apagándose...";

            default:
                return "Error: Comando desconocido '" + command + "'.";
        }
    }

    private class AdminClientHandler implements Runnable {
        private Socket clientSocket;
        private ObjectOutputStream out;

        public AdminClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                adminClientHandlers.add(this);

                // Start a separate thread for reading commands from the admin client
                new Thread(() -> {
                    try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
                        while (!clientSocket.isClosed()) {
                            Object commandObj = in.readObject();
                            if (commandObj instanceof String) {
                                handleAdminCommand((String) commandObj);
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        if (!clientSocket.isClosed()) {
                            Logger.warn("Admin client disconnected: " + clientSocket.getInetAddress());
                            adminClientHandlers.remove(this);
                        }
                    }
                }).start();

            } catch (IOException e) {
                Logger.warn("Failed to establish connection with admin client: " + clientSocket.getInetAddress(), e);
                adminClientHandlers.remove(this);
            }
        }

        public void sendGameState(GameStateSnapshot snapshot) {
            try {
                out.writeObject(snapshot);
                out.reset();
            } catch (IOException e) {
                Logger.warn("Failed to send game state to admin, removing.", e);
                // Here you would remove this handler from a list of admin handlers
            }
        }

        public void sendAdminData(com.tuempresa.proyecto.demo1.net.dto.AdminDataSnapshot snapshot) {
            try {
                out.writeObject(snapshot);
                out.reset();
            } catch (IOException e) {
                Logger.warn("Failed to send admin data to admin, removing.", e);
                // Here you would remove this handler from a list of admin handlers
            }
        }
    }

    public void stop() {
        Logger.info("Deteniendo el servidor...");
        gameLoop.shutdownNow();

        try {
            if (playerServerSocket != null && !playerServerSocket.isClosed()) {
                playerServerSocket.close();
            }
            if (adminServerSocket != null && !adminServerSocket.isClosed()) {
                adminServerSocket.close();
            }
        } catch (IOException e) {
            Logger.error("Error al cerrar los server sockets", e);
        }

        if (playerListenerThread != null) {
            playerListenerThread.interrupt();
        }
        if (adminListenerThread != null) {
            adminListenerThread.interrupt();
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
