# Snake Game

This project is a classic Snake game implemented in Java with multiplayer support.

## Project Structure

The project is a standard Maven project with the following structure:

*   `src/main/java/com/tuempresa/proyecto/demo1/`: Contains the main source code for the game.
    *   `Game.java`: The main entry point for the application. It can launch the game in single-player, server, or client mode.
    *   `GameLogic.java`: Handles the core game mechanics, including snake movement, collision detection, and fruit generation.
    *   `GameServer.java`: Implements the server functionality, managing client connections and the main game loop.
    *   `GameClient.java`: Implements the client functionality, connecting to the server and handling user input.
    *   `GameState.java`: Represents the state of the game, including the snakes, fruits, and board dimensions.
    *   `GraphicalView.java`: Provides the graphical user interface for the game using Java Swing.
*   `pom.xml`: The Maven Project Object Model file, which defines project dependencies and build settings.

## Network Integration

The game uses a client-server architecture for multiplayer gameplay.

*   **Protocol:** Communication is based on TCP sockets, with the server listening on port `12345`.
*   **Data Transfer:** The client and server exchange serialized Java objects. The server sends `GameStateSnapshot` objects to clients, and clients send `Direccion` objects to the server.
*   **Threading:** The server is multi-threaded, with a new thread for each client. The main game loop runs on a `ScheduledExecutorService`.

## Game Logic and Algorithmic Complexity

The core game logic is in the `GameLogic` class.

*   **Collision Detection:** The `comprobarColisiones` method checks for collisions between snakes, walls, and fruits. The current implementation has a time complexity of **O(S^2 * L)**, where `S` is the number of snakes and `L` is the average length of a snake. This is because it iterates through all snakes and checks for collisions with all other snakes.
*   **Potential Improvement:** For a large number of players, this could become a performance bottleneck. A possible optimization is to use a spatial partitioning data structure (e.g., a quadtree or a grid-based hash) to reduce the number of collision checks.

## How to Run

You can run the game using Maven.

### Single-Player Mode

```bash
mvn exec:java
```

### Server Mode

```bash
mvn exec:java -Dexec.args="server"
```

### Client Mode

Make sure the server is running, then execute the following command in a new terminal:

```bash
mvn exec:java -Dexec.args="client"
```
