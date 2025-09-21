package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PerformanceMetricsTest {

    private static final String TEST_SERVER_LOG = "test_server.log";
    private static final String TEST_CLIENT_LOG = "test_client.log";

    private GameServer server;
    private GameClient client;
    private Thread serverThread;
    private Thread clientThread;
    private ScheduledExecutorService testPingScheduler;

    @BeforeEach
    void setUp() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_SERVER_LOG));
        Files.deleteIfExists(Paths.get(TEST_CLIENT_LOG));
        Logger.reset();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }
        if (clientThread != null && clientThread.isAlive()) {
            clientThread.interrupt();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
        if (testPingScheduler != null && !testPingScheduler.isShutdown()) {
            testPingScheduler.shutdownNow();
        }
        // Wait for threads to die
        if (serverThread != null) serverThread.join(1000);
        if (clientThread != null) clientThread.join(1000);
        Logger.reset();
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void whenMetricsEnabled_thenLogsContainAllMetricData() throws Exception {
        assumeTrue(GameConfig.ENABLE_PERFORMANCE_METRICS, "This test requires ENABLE_PERFORMANCE_METRICS to be true in GameConfig.java");

        // --- Server Setup ---
        Logger.setLogFile(TEST_SERVER_LOG);
        server = new GameServer();
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        Thread.sleep(500); // Give server time to start

        // --- Client Setup ---
        Logger.setLogFile(TEST_CLIENT_LOG);
        testPingScheduler = Executors.newSingleThreadScheduledExecutor();
        client = new GameClient(true, testPingScheduler); // Test mode with custom scheduler

        clientThread = new Thread(() -> {
            try {
                client.start();
            } catch (IOException e) {
                // This is expected when the client is interrupted
            }
        });
        clientThread.start();
        testPingScheduler.scheduleAtFixedRate(() -> client.sendPing(), 1, 1, TimeUnit.SECONDS);

        // Wait for client to connect and get a player ID
        client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);

        // --- Simulate Rendering ---
        // We need a graphics context to call paintComponent, a BufferedImage works well
        GameStateSnapshot emptySnapshot = new GameStateSnapshot(1,1, Collections.emptyList(), Collections.emptyList(), true, GamePhase.IN_PROGRESS);
        GamePanel gamePanel = new GamePanel(emptySnapshot);
        // Simulate the panel having a size, otherwise getWidth/getHeight is 0
        gamePanel.setSize(new java.awt.Dimension(100, 100));
        BufferedImage image = new BufferedImage(gamePanel.getWidth(), gamePanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        // Let the game run to generate logs
        for (int i = 0; i < 5; i++) {
            gamePanel.paintComponent(g); // Manually trigger render logging
            Thread.sleep(250);
        }

        // --- Verification ---
        List<String> serverLog = Files.readAllLines(Paths.get(TEST_SERVER_LOG), StandardCharsets.UTF_8);
        List<String> clientLog = Files.readAllLines(Paths.get(TEST_CLIENT_LOG), StandardCharsets.UTF_8);

        // Filter for metric lines only to make assertions more robust
        List<String> serverMetrics = serverLog.stream().filter(s -> s.contains("[METRIC]")).collect(Collectors.toList());
        List<String> clientMetrics = clientLog.stream().filter(s -> s.contains("[METRIC]")).collect(Collectors.toList());

        assertTrue(serverMetrics.stream().anyMatch(line -> line.contains("Server tick duration")), "Server log should contain tick duration metric.");
        assertTrue(serverMetrics.stream().anyMatch(line -> line.contains("Packet size")), "Server log should contain packet size metric.");
        assertTrue(clientMetrics.stream().anyMatch(line -> line.contains("Client frame render time")), "Client log should contain render time metric.");
        assertTrue(clientMetrics.stream().anyMatch(line -> line.contains("Network Latency (RTT)")), "Client log should contain RTT metric.");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void whenMetricsDisabled_thenLogsContainNoMetricData() throws Exception {
        assumeTrue(!GameConfig.ENABLE_PERFORMANCE_METRICS, "This test requires ENABLE_PERFORMANCE_METRICS to be false in GameConfig.java");

        // --- Server Setup ---
        Logger.setLogFile(TEST_SERVER_LOG);
        server = new GameServer();
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        Thread.sleep(500);

        // --- Client Setup ---
        Logger.setLogFile(TEST_CLIENT_LOG);
        client = new GameClient(true); // Test mode
        clientThread = new Thread(() -> {
            try {
                client.start();
            } catch (IOException e) {
                // Expected
            }
        });
        clientThread.start();
        client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);

        Thread.sleep(1000); // Let it run a bit

        // --- Verification ---
        List<String> serverLog = Files.readAllLines(Paths.get(TEST_SERVER_LOG), StandardCharsets.UTF_8);
        List<String> clientLog = Files.readAllLines(Paths.get(TEST_CLIENT_LOG), StandardCharsets.UTF_8);

        long serverMetricCount = serverLog.stream().filter(line -> line.contains("[METRIC]")).count();
        long clientMetricCount = clientLog.stream().filter(line -> line.contains("[METRIC]")).count();

        assertTrue(serverMetricCount == 0, "Server log should not contain any metrics when disabled.");
        assertTrue(clientMetricCount == 0, "Client log should not contain any metrics when disabled.");
    }
}
