package com.tuempresa.proyecto.demo1.game;

import com.tuempresa.proyecto.demo1.model.BotDifficulty;
import com.tuempresa.proyecto.demo1.model.Coordenada;
import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.net.Packet;
import com.tuempresa.proyecto.demo1.net.dto.FrutaSnapshot;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.net.dto.SnakeSnapshot;
import com.tuempresa.proyecto.demo1.util.Logger;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
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
    private BotDifficulty difficulty;
    private final Random random = new Random();

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
        int pick = random.nextInt(BotDifficulty.values().length);
        this.difficulty = BotDifficulty.values()[pick];
        this.botId = "Bot-" + difficulty.name().substring(0, 3) + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);


        Logger.info("Bot " + botId + " conectando a " + GameConfig.DEFAULT_HOST + ":" + GameConfig.DEFAULT_PORT);
        socket = new Socket(GameConfig.DEFAULT_HOST, GameConfig.DEFAULT_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Enviar nuestro ID autogenerado al servidor
        out.writeObject(botId);
        out.flush();

        // Ya no esperamos que el servidor nos devuelva un ID.
        Logger.info("Bot conectado. ID: " + botId + " con dificultad " + this.difficulty);

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
                Object receivedObject = in.readObject();
                if (receivedObject instanceof Packet) {
                    byte[] snapshotBytes = ((Packet) receivedObject).data;
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(snapshotBytes);
                         ObjectInputStream ois = new ObjectInputStream(bais)) {
                        GameStateSnapshot snapshot = (GameStateSnapshot) ois.readObject();
                        decideNextMove(snapshot);
                    } catch (java.io.IOException | ClassNotFoundException e) {
                        Logger.warn("Error deserializing game state packet for bot " + botId, e);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.warn("Bot " + botId + " perdió la conexión con el servidor.", e);
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

        switch (difficulty) {
            case FACIL:
                moveEasy(snapshot, mySnake);
                break;
            case INTERMEDIO:
                moveIntermediate(snapshot, mySnake);
                break;
            case MAESTRO:
                moveMaster(snapshot, mySnake);
                break;
        }
    }

    private void moveEasy(GameStateSnapshot snapshot, SnakeSnapshot mySnake) {
        Direccion currentDirection = direccionActual.get();
        Coordenada head = mySnake.cuerpo.get(0);

        // Con una pequeña probabilidad, intentar un giro aleatorio para un comportamiento menos predecible.
        if (random.nextInt(100) < 10) { // 10% de probabilidad de un giro "aleatorio"
            Direccion randomTurn = random.nextBoolean() ? getLeftTurn(currentDirection) : getRightTurn(currentDirection);
            Coordenada nextCoord = getNextCoordenada(head, randomTurn);
            if (!isCollision(nextCoord, mySnake, snapshot)) {
                Logger.info("Bot " + botId + " (" + difficulty + ") haciendo un giro aleatorio a " + randomTurn);
                direccionActual.set(randomTurn);
                return;
            }
        }

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
                    Logger.info("Bot " + botId + " (" + difficulty + ") cambiando de " + currentDirection + " a " + nextDir + " para evitar colisión.");
                    direccionActual.set(nextDir);
                }
                return; // Encontramos una dirección segura, salimos.
            }
        }

        // Si todas las direcciones posibles llevan a una colisión, no hacemos nada (el bot morirá).
        Logger.warn("Bot " + botId + " (" + difficulty + ") está atrapado. No hay movimientos seguros.");
    }

    private void moveIntermediate(GameStateSnapshot snapshot, SnakeSnapshot mySnake) {
        // 1. Find the closest fruit
        Optional<FrutaSnapshot> closestFruit = findClosestFruit(snapshot, mySnake);

        if (closestFruit.isPresent()) {
            Direccion currentDirection = direccionActual.get();
            Coordenada head = mySnake.cuerpo.get(0);

            // 2. Decide direction towards fruit
            Direccion directionToFruit = getDirectionToTarget(head, closestFruit.get().coordenada, currentDirection);

            // 3. Check if the path is safe
            Coordenada nextCoord = getNextCoordenada(head, directionToFruit);
            if (!isCollision(nextCoord, mySnake, snapshot)) {
                if (!directionToFruit.equals(currentDirection)) {
                    Logger.info("Bot " + botId + " (" + difficulty + ") va por fruta. Cambiando de " + currentDirection + " a " + directionToFruit);
                    direccionActual.set(directionToFruit);
                }
                return;
            }
        }

        // 4. If no fruit or path to fruit is unsafe, fall back to easy move
        moveEasy(snapshot, mySnake);
    }

    private Optional<FrutaSnapshot> findClosestFruit(GameStateSnapshot snapshot, SnakeSnapshot mySnake) {
        if (snapshot.frutas.isEmpty()) {
            return Optional.empty();
        }
        Coordenada head = mySnake.cuerpo.get(0);
        return snapshot.frutas.stream()
                .min(Comparator.comparingInt(fruit -> manhattanDistance(head, fruit.coordenada)));
    }

    private Direccion getDirectionToTarget(Coordenada from, Coordenada to, Direccion current) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());

        if (dx != 0 && dy != 0) {
            if (Math.abs(to.getX() - from.getX()) > Math.abs(to.getY() - from.getY())) {
                return dx > 0 ? Direccion.DERECHA : Direccion.IZQUIERDA;
            } else {
                return dy > 0 ? Direccion.ABAJO : Direccion.ARRIBA;
            }
        } else if (dx != 0) {
            return dx > 0 ? Direccion.DERECHA : Direccion.IZQUIERDA;
        } else if (dy != 0) {
            return dy > 0 ? Direccion.ABAJO : Direccion.ARRIBA;
        }

        return current;
    }

    private int manhattanDistance(Coordenada c1, Coordenada c2) {
        return Math.abs(c1.getX() - c2.getX()) + Math.abs(c1.getY() - c2.getY());
    }

    private void moveMaster(GameStateSnapshot snapshot, SnakeSnapshot mySnake) {
        // 1. Find the closest enemy
        Optional<SnakeSnapshot> closestEnemy = findClosestEnemy(snapshot, mySnake);

        if (closestEnemy.isPresent()) {
            SnakeSnapshot enemy = closestEnemy.get();
            // 2. Predict enemy's next move (simple prediction)
            Direccion enemyDirection = getSnakeDirection(enemy);
            Coordenada enemyHead = enemy.cuerpo.get(0);
            Coordenada predictedEnemyNextPos = getNextCoordenada(enemyHead, enemyDirection);

            // 3. Find an attack position
            // Try to get in front of the enemy
            Direccion directionToAttack = getDirectionToTarget(mySnake.cuerpo.get(0), predictedEnemyNextPos, direccionActual.get());

            // 4. Check if attack is safe
            Coordenada myNextPos = getNextCoordenada(mySnake.cuerpo.get(0), directionToAttack);
            if (!isCollision(myNextPos, mySnake, snapshot)) {
                if (!directionToAttack.equals(direccionActual.get())) {
                    Logger.info("Bot " + botId + " (" + difficulty + ") va a atacar a " + enemy.idJugador + ". Cambiando a " + directionToAttack);
                    direccionActual.set(directionToAttack);
                }
                return;
            }
        }

        // 5. If no enemy or attack is not safe, behave as intermediate
        moveIntermediate(snapshot, mySnake);
    }

    private Optional<SnakeSnapshot> findClosestEnemy(GameStateSnapshot snapshot, SnakeSnapshot mySnake) {
        Coordenada myHead = mySnake.cuerpo.get(0);
        return snapshot.snakes.stream()
                .filter(s -> !s.idJugador.equals(mySnake.idJugador)) // Filter out myself
                .min(Comparator.comparingInt(enemy -> manhattanDistance(myHead, enemy.cuerpo.get(0))));
    }

    private Direccion getSnakeDirection(SnakeSnapshot snake) {
        if (snake.cuerpo.size() < 2) {
            return Direccion.DERECHA; // No info, assume right
        }
        Coordenada head = snake.cuerpo.get(0);
        Coordenada neck = snake.cuerpo.get(1);
        int dx = head.getX() - neck.getX();
        int dy = head.getY() - neck.getY();

        if (dx == 1) return Direccion.DERECHA;
        if (dx == -1) return Direccion.IZQUIERDA;
        if (dy == 1) return Direccion.ABAJO;
        if (dy == -1) return Direccion.ARRIBA;

        return Direccion.DERECHA; // Should not happen
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
        // 1. Comprobar colisión con las paredes (lógica del servidor)
        if (coord.getX() < 0 || coord.getX() >= snapshot.width ||
            coord.getY() < 0 || coord.getY() >= snapshot.height) {
            return true;
        }

        // 2. Comprobar colisión con todas las serpientes (lógica del servidor)
        for (SnakeSnapshot otherSnake : snapshot.snakes) {
            // Comprobar si la coordenada 'coord' está en el cuerpo de 'otherSnake'
            if (!otherSnake.cuerpo.contains(coord)) {
                continue; // No hay colisión con esta serpiente, pasar a la siguiente
            }

            // Si la colisión es con nosotros mismos
            if (otherSnake.idJugador.equals(mySnake.idJugador)) {
                // La lógica del servidor comprueba todo el cuerpo para evitar giros de 180 grados.
                // Si 'coord' está en nuestro cuerpo, es una colisión.
                return true;
            }

            // Si la colisión es con otra serpiente
            // Es una colisión, pero necesitamos ver si es con la cola que está a punto de moverse.
            Coordenada tail = otherSnake.cuerpo.get(otherSnake.cuerpo.size() - 1);

            // Si la colisión es con la cola Y la serpiente no está creciendo Y no es de un solo segmento,
            // entonces NO es una colisión real.
            boolean isTailCollision = coord.equals(tail);
            boolean isGrowing = otherSnake.segmentosPorCrecer > 0;
            boolean isMultiSegment = otherSnake.cuerpo.size() > 1;

            if (isTailCollision && !isGrowing && isMultiSegment) {
                // No es una colisión fatal, la cola se moverá.
                continue;
            }

            // Si no es el caso especial de la cola, es una colisión real.
            return true;
        }

        return false; // No se encontró ninguna colisión
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
