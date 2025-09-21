package com.tuempresa.proyecto.demo1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

/**
 * La clase Bot simula ser un cliente del juego con una lógica automática.
 * Se conecta al servidor, recibe el estado del juego y toma decisiones
 * para mantenerse con vida sin intervención humana.
 */
public class Bot {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String botId;
    private final AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);

    public static void main(String[] args) {
        // Lógica para iniciar un bot. Se puede modificar para iniciar múltiples bots.
        Logger.info("Iniciando un bot...");
        Bot bot = new Bot();
        try {
            bot.start();
        } catch (IOException | ClassNotFoundException e) {
            Logger.error("El bot falló al iniciar o se desconectó: " + e.getMessage(), e);
        }
    }

    public void start() throws IOException, ClassNotFoundException {
        Logger.info("Bot conectando a " + GameConfig.DEFAULT_HOST + ":" + GameConfig.DEFAULT_PORT);
        socket = new Socket(GameConfig.DEFAULT_HOST, GameConfig.DEFAULT_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        botId = (String) in.readObject();
        Logger.info("Bot conectado. ID: " + botId);

        // Hilo para enviar la dirección al servidor
        Thread inputSenderThread = new Thread(this::sendInputLoop);
        inputSenderThread.setDaemon(true);
        inputSenderThread.start();

        // Bucle principal para recibir el estado del juego
        receiveGameStateLoop();
    }

    private void sendInputLoop() {
        Direccion lastSentDirection = null;
        try {
            while (socket != null && !socket.isClosed()) {
                Direccion currentDirection = direccionActual.get();
                if (!currentDirection.equals(lastSentDirection)) {
                    out.writeObject(currentDirection);
                    out.flush();
                    lastSentDirection = currentDirection;
                    Logger.info("Bot " + botId + " cambió dirección a " + currentDirection);
                }
                // Pausa para no saturar la red
                Thread.sleep(50);
            }
        } catch (IOException | InterruptedException e) {
            Logger.warn("El hilo de envío de input del bot " + botId + " fue interrumpido.");
        }
    }

    private void receiveGameStateLoop() {
        try {
            while (socket != null && !socket.isClosed()) {
                GameStateSnapshot snapshot = (GameStateSnapshot) in.readObject();
                decideNextMove(snapshot);
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.warn("Bot " + botId + " perdió la conexión con el servidor.");
        } finally {
            closeConnection();
        }
    }

    private void decideNextMove(GameStateSnapshot snapshot) {
        SnakeSnapshot mySnake = snapshot.snakes.stream()
                .filter(s -> s.idJugador.equals(botId))
                .findFirst()
                .orElse(null);

        if (mySnake == null || mySnake.cuerpo.isEmpty()) {
            Logger.info("Bot " + botId + " no se encontró en el juego (probablemente murió).");
            return;
        }

        Direccion currentDirection = direccionActual.get();
        Coordenada head = mySnake.cuerpo.get(0);

        // Estrategia de evasión mejorada: probar la dirección actual, luego la derecha, luego la izquierda.
        Direccion[] possibleDirections = {
                currentDirection,
                getRightTurn(currentDirection),
                getLeftTurn(currentDirection)
        };

        for (Direccion nextDir : possibleDirections) {
            Coordenada nextCoord = getNextCoordenada(head, nextDir);
            if (!isCollision(nextCoord, mySnake, snapshot)) {
                if (!nextDir.equals(currentDirection)) {
                    Logger.info("Bot " + botId + " cambiando de " + currentDirection + " a " + nextDir + " para evitar colisión.");
                    direccionActual.set(nextDir);
                }
                return; // Encontramos una dirección segura, salimos.
            }
        }

        // Si todas las direcciones posibles llevan a una colisión, no hacemos nada (el bot morirá).
        Logger.warn("Bot " + botId + " está atrapado. No hay movimientos seguros.");
    }

    private Coordenada getNextCoordenada(Coordenada head, Direccion direction) {
        switch (direction) {
            case ARRIBA: return new Coordenada(head.getX(), head.getY() - 1);
            case ABAJO: return new Coordenada(head.getX(), head.getY() + 1);
            case IZQUIERDA: return new Coordenada(head.getX() - 1, head.getY());
            case DERECHA: return new Coordenada(head.getX() + 1, head.getY());
            default: return head;
        }
    }

    private boolean isCollision(Coordenada coord, SnakeSnapshot mySnake, GameStateSnapshot snapshot) {
        // 1. Comprobar colisión con las paredes
        if (coord.getX() < 0 || coord.getX() >= GameConfig.ANCHO_TABLERO ||
            coord.getY() < 0 || coord.getY() >= GameConfig.ALTO_TABLERO) {
            return true;
        }

        // 2. Comprobar colisión con CUALQUIER serpiente en el tablero
        for (SnakeSnapshot snake : snapshot.snakes) {
            // Si la serpiente es la nuestra, no chocamos con la punta de la cola, porque se moverá.
            if (snake.idJugador.equals(mySnake.idJugador)) {
                if (snake.cuerpo.size() > 2 && snake.cuerpo.subList(0, snake.cuerpo.size() - 1).contains(coord)) {
                    return true;
                }
            } else {
                // Para otras serpientes, cualquier parte de su cuerpo es una colisión.
                if (snake.cuerpo.contains(coord)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Direccion getLeftTurn(Direccion current) {
        switch (current) {
            case ARRIBA: return Direccion.IZQUIERDA;
            case DERECHA: return Direccion.ARRIBA;
            case ABAJO: return Direccion.DERECHA;
            case IZQUIERDA: return Direccion.ABAJO;
            default: return Direccion.IZQUIERDA; // Fallback
        }
    }

    private Direccion getRightTurn(Direccion current) {
        switch (current) {
            case ARRIBA: return Direccion.DERECHA;
            case DERECHA: return Direccion.ABAJO;
            case ABAJO: return Direccion.IZQUIERDA;
            case IZQUIERDA: return Direccion.ARRIBA;
            default: return Direccion.DERECHA; // Fallback
        }
    }

    private void closeConnection() {
        Logger.info("Bot " + botId + " desconectado.");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error al cerrar el socket del bot " + botId, e);
        }
    }
}
