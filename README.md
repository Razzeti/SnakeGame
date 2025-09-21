# Snake Game

This project is a classic Snake game implemented in Java with multiplayer support. It features a robust client-server architecture, a centralized configuration system, and an administrator interface for game management.

## Project Structure

The project is a standard Maven project. Key classes include:

*   `src/main/java/com/tuempresa/proyecto/demo1/`:
    *   **`Game.java`**: The main entry point for the application. Launches the game in single-player, server, or client mode.
    *   **`GameServer.java`**: Manages the server, including client connections, the main game loop, and the admin interface.
    *   **`GameClient.java`**: Implements the client, connecting to the server and handling user input.
    *   **`GameLogic.java`**: Handles the core game mechanics, including snake movement, collision detection, and fruit generation.
    *   **`GameState.java`**: Represents the complete state of the game, including snakes, fruits, and the current `GamePhase`.
    *   **`GraphicalView.java` & `GamePanel.java`**: Provide the graphical user interface for the game using Java Swing.
    *   **`GameConfig.java`**: A centralized configuration file for all game constants (e.g., board size, network ports, colors).
*   `pom.xml`: The Maven Project Object Model file, which defines project dependencies and build settings.

## New Features

*   **Game State Management**: The game now uses a `GamePhase` enum (`WAITING_FOR_PLAYERS`, `IN_PROGRESS`, `GAME_ENDED`) for more reliable state transitions.
*   **Dynamic Fruits**: The game can now generate different types of fruits with varying point values and colors.
*   **Administrator Interface**: A new text-based admin console allows for remote management of the game server.
*   **Centralized Configuration**: All major settings are now stored in `GameConfig.java` for easy modification.
*   **Data Transfer Objects (DTOs)**: The server and client now communicate using lightweight `GameStateSnapshot` objects, separating network data from the internal game state.

## Network Integration

The game uses a client-server architecture for multiplayer gameplay.

*   **Player Protocol**: Communication is based on TCP sockets. The server listens for players on port `12345` (configurable in `GameConfig.java`).
*   **Admin Protocol**: The server listens for admin connections on port `12346` (`DEFAULT_PORT + 1`).
*   **Data Transfer**: The server sends serialized `GameStateSnapshot` DTOs to clients. Clients send `Direccion` objects to the server.

## Game Logic and Algorithmic Complexity

The core game logic is in the `GameLogic` class.

*   **Collision Detection**: The `actualizar` method now handles all game logic per tick. It detects collisions between snakes, walls, and other snakes. A new mechanism correctly handles simultaneous head-on-head collisions.
*   **Complexity**: The time complexity for collision detection remains **O(S^2 * L)**, where `S` is the number of snakes and `L` is the average length of a snake, as it checks every snake against every other snake.

## How to Run

You can run the game using Maven.

### 1. Start the Server

```bash
mvn exec:java -Dexec.args="server"
```
The server will start and wait for players to connect. The game will be in the `WAITING_FOR_PLAYERS` phase.

### 2. Connect Clients

In new terminal windows, connect one or more clients:

```bash
mvn exec:java -Dexec.args="client"
```
Each client will connect to the server and a new snake will appear on the board.

### 3. Use the Admin Interface to Start the Game

The game will not start until an administrator sends the `START_GAME` command. You can do this using a tool like `netcat` or `telnet`.

```bash
# Connect to the admin port (12346 by default)
nc localhost 12346
```

Once connected, you can issue commands:
*   `START_GAME`: Starts the game.
*   `RESET_GAME`: Resets the game to the waiting phase.
*   `LIST_PLAYERS`: Lists all connected players and their scores.
*   `SHUTDOWN`: Stops the server.

After typing a command, press Enter. The server will respond with a confirmation.

### Single-Player Mode

For a quick test, you can still run in single-player mode. This mode starts the game immediately.

```bash
mvn exec:java
```
