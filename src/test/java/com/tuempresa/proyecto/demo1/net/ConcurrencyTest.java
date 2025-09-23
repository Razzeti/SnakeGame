package com.tuempresa.proyecto.demo1.net;

import org.junit.jupiter.api.AfterEach;
import com.tuempresa.proyecto.demo1.model.Direccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcurrencyTest {

    private GameServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws InterruptedException {
        server = new GameServer();
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        // Give the server a moment to start
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stop();
        serverThread.interrupt();
        // Give the server a moment to shut down
        Thread.sleep(500);
    }

    @Test
    void testMassConnection() throws InterruptedException {
        int numberOfClients = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        AtomicInteger successfulConnections = new AtomicInteger(0);

        for (int i = 0; i < numberOfClients; i++) {
            executor.submit(() -> {
                GameClient client = new GameClient(true);
                try {
                    // The client's start() method blocks, so we run it in a separate thread
                    Thread clientThread = new Thread(() -> {
                        try {
                            client.start();
                        } catch (IOException e) {
                            // This is expected when the client disconnects
                        }
                    });
                    clientThread.start();

                    // Wait for the client to get a player ID
                    client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);
                    successfulConnections.incrementAndGet();

                    // Keep the connection alive for a short time
                    Thread.sleep(1000);
                    client.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(numberOfClients, successfulConnections.get(), "All clients should have connected successfully.");
    }

    @Test
    void testHighChurn() throws InterruptedException {
        int numberOfClients = 50;
        int testDurationSeconds = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        final boolean[] running = {true};
        AtomicInteger connectionCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfClients; i++) {
            executor.submit(() -> {
                while (running[0]) {
                    GameClient client = new GameClient(true);
                    try {
                        Thread clientThread = new Thread(() -> {
                            try {
                                client.start();
                            } catch (IOException e) {
                                // Expected on disconnect
                            }
                        });
                        clientThread.start();

                        client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);
                        connectionCount.incrementAndGet();

                        // Stay connected for a random time
                        Thread.sleep((long) (Math.random() * 1000));

                        client.disconnect();
                        clientThread.join(1000);

                    } catch (Exception e) {
                        // Ignore exceptions during churn
                    }
                }
            });
        }

        Thread.sleep(testDurationSeconds * 1000);
        running[0] = false;
        executor.shutdownNow();

        System.out.println("Total connections in " + testDurationSeconds + " seconds: " + connectionCount.get());
        // No assertion here, the test passes if the server doesn't crash and there are no obvious errors in the logs.
    }

    @Test
    void testHighActivity() throws InterruptedException, IOException {
        int numberOfClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        final boolean[] gameStarted = {false};

        for (int i = 0; i < numberOfClients; i++) {
            executor.submit(() -> {
                GameClient client = new GameClient(true);
                try {
                    Thread clientThread = new Thread(() -> {
                        try {
                            client.start();
                        } catch (IOException e) {
                            // Expected on disconnect
                        }
                    });
                    clientThread.start();

                    client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);
                    latch.countDown();

                    // Wait until the game starts
                    while (!gameStarted[0]) {
                        Thread.sleep(100);
                    }

                    // Send random directions
                    while (gameStarted[0]) {
                        client.setDirection(Direccion.values()[(int) (Math.random() * Direccion.values().length)]);
                        Thread.sleep(50);
                    }

                    client.disconnect();
                    clientThread.join(1000);

                } catch (Exception e) {
                    // Ignore
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);

        // Start the game with an admin client
        try (Socket adminSocket = new Socket("localhost", 12346);
             ObjectOutputStream out = new ObjectOutputStream(adminSocket.getOutputStream())) {
            out.writeObject("START_GAME");
            out.flush();
        }

        gameStarted[0] = true;

        Thread.sleep(10 * 1000); // Run for 10 seconds
        gameStarted[0] = false; // Stop the clients

        executor.shutdownNow();
    }

    @Test
    void testMaxGameState() throws InterruptedException, IOException {
        int numberOfClients = 4; // Max players based on STARTING_POSITIONS
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        final boolean[] gameStarted = {true};

        for (int i = 0; i < numberOfClients; i++) {
            final int clientIndex = i;
            executor.submit(() -> {
                GameClient client = new GameClient(true);
                try {
                    Thread clientThread = new Thread(() -> {
                        try {
                            client.start();
                        } catch (IOException e) {
                            // Expected on disconnect
                        }
                    });
                    clientThread.start();

                    client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);

                    // Move in a box pattern to avoid collisions
                    while (gameStarted[0]) {
                        // This logic is simple and may not be perfect, but it's enough to keep snakes alive for a while
                        long time = System.currentTimeMillis() / 1000;
                        if (time % 4 == 0) {
                            client.setDirection(Direccion.ABAJO);
                        } else if (time % 4 == 1) {
                            client.setDirection(Direccion.IZQUIERDA);
                        } else if (time % 4 == 2) {
                            client.setDirection(Direccion.ARRIBA);
                        } else {
                            client.setDirection(Direccion.DERECHA);
                        }
                        Thread.sleep(200);
                    }

                    client.disconnect();
                    clientThread.join(1000);

                } catch (Exception e) {
                    // Ignore
                }
            });
        }

        // Start the game with an admin client
        try (Socket adminSocket = new Socket("localhost", 12346);
             ObjectOutputStream out = new ObjectOutputStream(adminSocket.getOutputStream())) {
            out.writeObject("START_GAME");
            out.flush();
        }

        Thread.sleep(30 * 1000); // Run for 30 seconds
        gameStarted[0] = false; // Stop the clients

        executor.shutdownNow();
    }
}
