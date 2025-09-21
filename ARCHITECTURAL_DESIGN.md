# Architectural Design Document

This document outlines the proposed architectural changes to the Java Snake game. The goal is to refactor the existing prototype into a robust, performant, and production-ready application that can serve as a foundation for a scalable, client-server multiplayer game.

## Part 1: Code Brittleness and Maintainability

### Analysis
The current codebase suffers from two main issues:
1.  **Hardcoded "Magic Numbers":** Values like the board dimensions (20, 10) are hardcoded directly in the source code, making it difficult to configure and maintain the game.
2.  **Incorrect Data Structures:** The `Coordenada` class, which is a fundamental data structure, is not implemented correctly for use in high-performance collections. It lacks proper `equals()` and `hashCode()` implementations, which can lead to incorrect behavior in collections like `HashSet` and `HashMap`.

### Proposed Solution
1.  **Centralized Configuration:** I will create a `Constants` class to centralize all configuration values. This will allow for easy modification of game parameters such as board dimensions, snake speed, and other game-related constants.
2.  **Proper `Coordenada` Implementation:** I will override the `equals()` and `hashCode()` methods in the `Coordenada` class. This will ensure that instances of `Coordenada` can be correctly used in hash-based collections, which is crucial for efficient collision detection and other game logic.

## Part 2: Server Observability and Performance Blind Spots

### Analysis
The server currently runs "dark," with no visibility into its internal state, critical events, or performance bottlenecks. This makes it impossible to debug issues post-factum or analyze the game's performance under load.

### Proposed Solution
I will implement a unified `Logger` class to provide a simple and effective observability solution. This class will:
-   Provide static methods for logging at different levels (e.g., `info`, `warn`, `error`).
-   Write log messages to both the console and a log file (`server.log` and `client.log`).
-   Be integrated into critical parts of the application to log events such as player connections/disconnections, game state changes, and game loop tick durations.

## Part 3: Concurrency and GUI Instability

### Analysis
The current threading model allows the game loop thread and the Swing Event Dispatch Thread (EDT) to access the same mutable `GameState` object. This creates a high risk of `ConcurrentModificationException` errors and graphical glitches, as the game state can be modified while it is being rendered.

### Proposed Solution
I will implement the **Snapshot Pattern** to decouple the game logic state from the rendering state.
-   The game loop will create an immutable snapshot of the game state at the end of each tick.
-   The rendering thread will use this stable, immutable snapshot for rendering.
This approach completely eliminates the risk of concurrency issues by ensuring that the rendering thread always operates on a consistent view of the data, without the need for complex locking mechanisms. I will create a `GameStateSnapshot` class to hold a deep copy of all the data required for rendering.

## Part 4: Network Inefficiency and Architectural Coupling

### Analysis
The current data model is not designed for network transmission. It is heavyweight and tightly couples the game logic to the view (e.g., by using `java.awt.Color`). This makes it difficult and inefficient to implement a client-server architecture.

### Proposed Solution
I will design and implement a set of **Data Transfer Objects (DTOs)** that are suitable for a low-latency network environment.
-   These DTOs will be lightweight, plain Java objects representing the game state in a serializable format.
-   I will create DTOs for the snake, fruit, and the overall game state (`SnakeDTO`, `FrutaDTO`, `GameStateDTO`).
-   These DTOs will contain only the data necessary for the client to render the game, removing any server-side logic or heavyweight objects.
This will create a clean separation between the server's authoritative state and the data required by the client, which is essential for a scalable and maintainable client-server architecture.
