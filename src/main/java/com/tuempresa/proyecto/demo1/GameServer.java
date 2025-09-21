package com.tuempresa.proyecto.demo1;

import com.tuempresa.proyecto.demo1.Logger.LogLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {

    private static final int PORT = 12345;
    private static final int TICK_RATE_MS = 150;
    private static final int ANCHO_TABLERO = 30;
    private static final int ALTO_TABLERO = 20;

    private GameState gameState;
    private GameLogic gameLogic;
    private ConcurrentHashMap<String, Direccion> accionesDeJugadores;
    private final Map<String, ObjectOutputStream> playerStreams = new ConcurrentHashMap<>();
    private final ClientMetrics clientMetrics = new ClientMetrics();
    private final ScheduledExecutorService monitoringScheduler = Executors.newScheduledThreadPool(1);
    private static final AtomicInteger playerCounter = new AtomicInteger(0);

    public GameServer() {
        gameState = new GameState(ANCHO_TABLERO, ALTO_TABLERO);
        gameLogic = new GameLogic();
        accionesDeJugadores = new ConcurrentHashMap<>();
        GameLogic.generarFruta(gameState);
    }

    public void start() throws IOException {
        Logger.log(LogLevel.INFO, "Iniciando servidor...");
        Runtime.getRuntime().addShutdownHook(new Thread(Logger::close));
        ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
        gameLoop.scheduleAtFixedRate(this::tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);
        scheduleMonitoringTasks();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Logger.log(LogLevel.INFO, "Servidor iniciado en el puerto " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Logger.log(LogLevel.INFO, "Nuevo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }

    private void scheduleMonitoringTasks() {
        monitoringScheduler.scheduleAtFixedRate(() -> {
            for (String playerId : playerStreams.keySet()) {
                double bandwidth = clientMetrics.getBandwidthKBps(playerId);
                Logger.log(LogLevel.INFO, String.format("Bandwidth for %s: %.2f KB/s", playerId, bandwidth));
                clientMetrics.resetBandwidth(playerId);
                sendPing(playerId);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void sendPing(String playerId) {
        ObjectOutputStream out = playerStreams.get(playerId);
        if (out != null) {
            try {
                out.writeObject(new Ping());
                out.flush();
                clientMetrics.recordPing(playerId);
            } catch (IOException e) {
                Logger.log(LogLevel.WARN, "Error sending ping to " + playerId);
            }
        }
    }

    private ScheduledExecutorService restartScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean restarting = false;

    private void tick() {
        if (gameState.isJuegoActivo()) {
            gameLogic.actualizar(gameState, accionesDeJugadores);
            if (gameState.getSerpientes().isEmpty() && !restarting) {
                Logger.log(LogLevel.INFO, "El juego ha terminado. Reiniciando en 5 segundos...");
                gameState.setJuegoActivo(false);
                restarting = true;
                restartScheduler.schedule(this::restartGame, 5, TimeUnit.SECONDS);
            }
        }
        broadcastGameState();
    }

    private void restartGame() {
        synchronized (gameState) {
            Logger.log(LogLevel.INFO, "Reiniciando juego...");
            accionesDeJugadores.clear();
            gameState.getSerpientes().clear();
            gameState.getFrutas().clear();

            int i = 0;
            for (String playerId : playerStreams.keySet()) {
                Snake newSnake = new Snake(playerId, new Coordenada(10 + i * 2, 10));
                gameState.getSerpientes().add(newSnake);
                accionesDeJugadores.put(playerId, Direccion.DERECHA);
                i++;
            }

            GameLogic.generarFruta(gameState);
            gameState.setJuegoActivo(true);
            restarting = false;
        }
    }

    private void broadcastGameState() {
        GameStateSnapshot snapshot = gameState.snapshot().toSnapshotDto();
        byte[] gameStateBytes = serialize(snapshot);
        if (gameStateBytes == null) {
            Logger.log(LogLevel.ERROR, "Failed to serialize game state.");
            return;
        }

        for (Map.Entry<String, ObjectOutputStream> entry : playerStreams.entrySet()) {
            String playerId = entry.getKey();
            ObjectOutputStream out = entry.getValue();
            try {
                out.writeObject(gameStateBytes);
                out.reset();
                clientMetrics.recordBytesSent(playerId, gameStateBytes.length);
            } catch (IOException e) {
                Logger.log(LogLevel.WARN, "Error al enviar estado al cliente " + playerId + ". Eliminando cliente.");
                playerStreams.remove(playerId);
            }
        }
    }

    private byte[] serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, "Error serializing object: " + e.getMessage());
            return null;
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
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                synchronized (gameState) {
                    playerId = "Jugador_" + playerCounter.incrementAndGet();
                    playerStreams.put(playerId, out);

                    Snake newSnake = new Snake(playerId, new Coordenada(10, 10));
                    gameState.getSerpientes().add(newSnake);
                    accionesDeJugadores.put(playerId, Direccion.DERECHA);
                    out.writeObject(playerId);
                    out.flush();
                }

                while (true) {
                    try {
                        Object obj = in.readObject();
                        if (obj instanceof Direccion) {
                            accionesDeJugadores.put(playerId, (Direccion) obj);
                        } else if (obj instanceof Ping) {
                            Ping ping = (Ping) obj;
                            long latency = clientMetrics.getLatencyMs(playerId, ping.getTimestamp());
                            Logger.log(LogLevel.INFO, String.format("Ping from %s: %d ms", playerId, latency));
                        }
                    } catch (ClassNotFoundException e) {
                        Logger.log(LogLevel.ERROR, "Error al leer la dirección del cliente " + playerId + ": " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException e) {
                Logger.log(LogLevel.INFO, "Conexión perdida con el cliente: " + clientSocket.getInetAddress().getHostAddress());
            } finally {
                if (playerId != null) {
                    playerStreams.remove(playerId);
                    Logger.log(LogLevel.INFO, "Jugador '" + playerId + "' desconectado.");
                    synchronized (gameState) {
                        gameState.getSerpientes().removeIf(s -> s.getIdJugador().equals(playerId));
                        accionesDeJugadores.remove(playerId);
                    }
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Logger.log(LogLevel.ERROR, "Error al cerrar el socket del cliente: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Logger.log(LogLevel.INFO, "Iniciando GameServer desde el método main.");
            new GameServer().start();
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, "No se pudo iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
