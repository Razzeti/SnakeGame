# Architectural Design Document

This document outlines the current architectural design of the Java Snake game. The architecture is designed to be robust, performant, and maintainable, supporting a client-server multiplayer experience.

## 1. Core Architecture: Client-Server Model

The game is built on a classic client-server model to support multiplayer gameplay.

*   **`GameServer`**: The authoritative central server that manages the game state, player connections, and the main game loop. It is responsible for all game logic execution.
*   **`GameClient`**: A lightweight client that connects to the server. Its primary responsibilities are to send user input (direction changes) to the server and to render the game state it receives from the server.
*   **Network Protocol**: Communication is handled over TCP sockets. The server listens for client connections on a configurable port (default `12345`) and for administrator connections on a separate port (default `12346`).

## 2. State Management and Concurrency

To ensure stability and prevent concurrency issues between the game logic thread and the rendering thread, the application employs the **Snapshot Pattern**.

*   **`GameState`**: A mutable class that holds the complete, authoritative state of the game, including all snakes, fruits, and the current game phase. This object is managed exclusively by the server's main game loop.
*   **`GameStateSnapshot`**: An immutable Data Transfer Object (DTO) that represents a read-only view of the game state at a specific moment in time.
*   **Data Flow**:
    1.  The `GameServer` updates the `GameState` on each tick.
    2.  After the update, the server creates an immutable `GameStateSnapshot` from the `GameState`.
    3.  This snapshot is serialized and broadcast to all connected `GameClient` instances.
    4.  The client's rendering thread (the Swing Event Dispatch Thread) uses this snapshot to draw the game.

This pattern decouples the game logic from the rendering logic, eliminating `ConcurrentModificationException` errors and ensuring that the GUI always renders a consistent state.

## 3. Data Transfer Objects (DTOs)

To ensure efficient and clean network communication, the project uses lightweight DTOs to transfer data between the server and clients.

*   **`GameStateSnapshot`**: The primary DTO, which encapsulates the entire state needed for rendering. It contains lists of `SnakeDTO` and `FrutaDTO` objects.
*   **`SnakeDTO` and `FrutaDTO`**: Lightweight, serializable representations of the game objects. They contain only the data necessary for the client to render them (e.g., coordinates, colors), without any of the server-side logic or heavyweight objects like `java.awt.Color`.

This approach minimizes bandwidth usage and creates a clean separation of concerns between the server's internal data model and the network-facing data contract.

## 4. Centralized Configuration

To enhance maintainability and simplify configuration, all "magic numbers" and tunable parameters have been centralized.

*   **`GameConfig.java`**: A final class containing static constants for all major game parameters. This includes:
    *   Board dimensions (`ANCHO_TABLERO`, `ALTO_TABLERO`)
    *   Network ports (`DEFAULT_PORT`, `ADMIN_PORT`)
    *   Game speed (`TICK_DELAY`)
    *   Colors and other rendering-related values

This centralization allows for easy adjustments to the game's behavior without modifying the core logic.

## 5. Game Flow and State Machine

The game's flow is managed by a robust state machine, implemented using the `GamePhase` enum.

*   **`GamePhase` Enum**: Defines the possible states of the game:
    *   `WAITING_FOR_PLAYERS`: The initial state when the server is waiting for clients to connect.
    *   `IN_PROGRESS`: The state when the game is actively being played.
    *   `GAME_ENDED`: The state after the game has finished.
*   **State Transitions**: The `GameServer` controls the transitions between these phases based on player actions and administrator commands. For example, the game only moves from `WAITING_FOR_PLAYERS` to `IN_PROGRESS` after an administrator issues the `START_GAME` command.

## 6. Administrator Interface

For game management and observability, a simple text-based administrator interface is available on a separate network port.

*   **Functionality**: Allows an administrator to connect via a tool like `netcat` or `telnet` to issue commands.
*   **Commands**:
    *   `START_GAME`: Starts the game.
    *   `RESET_GAME`: Resets the game to the waiting phase.
    *   `LIST_PLAYERS`: Lists connected players.
    *   `SHUTDOWN`: Stops the server.

This out-of-band management channel provides essential control over the live game environment.
