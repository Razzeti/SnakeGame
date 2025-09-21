package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameServerTest {

    @Test
    public void testBandwidthAndLatencyMonitoring() throws IOException, InterruptedException {
        // Start the server in a new thread
        Thread serverThread = new Thread(() -> {
            try {
                new GameServer().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Allow the server to start
        TimeUnit.SECONDS.sleep(2);

        // Start the client
        GameClient client = new GameClient(true); // true for test mode
        Thread clientThread = new Thread(() -> {
            try {
                client.start();
            } catch (IOException e) {
                // Expected to fail when the server shuts down
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();

        // Wait for monitoring logs
        boolean foundLogs = false;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10000) { // 10 second timeout
            java.nio.file.Path logPath = Paths.get("sunday_game.log");
            if (Files.exists(logPath)) {
                String logContent = new String(Files.readAllBytes(logPath));
                java.util.regex.Pattern bandwidthPattern = java.util.regex.Pattern.compile("Bandwidth for .*: (\\d+\\.\\d+) KB/s");
                java.util.regex.Matcher bandwidthMatcher = bandwidthPattern.matcher(logContent);
                java.util.regex.Pattern latencyPattern = java.util.regex.Pattern.compile("Ping from .*: (\\d+) ms");
                java.util.regex.Matcher latencyMatcher = latencyPattern.matcher(logContent);

                boolean positiveBandwidthFound = false;
                while(bandwidthMatcher.find()) {
                    double bandwidth = Double.parseDouble(bandwidthMatcher.group(1));
                    if (bandwidth > 0) {
                        positiveBandwidthFound = true;
                        break;
                    }
                }

                if (positiveBandwidthFound && latencyMatcher.find()) {
                    foundLogs = true;
                    break;
                }
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }

        // Stop the server and client
        serverThread.interrupt();
        clientThread.interrupt();

        assertTrue(foundLogs, "Did not find expected log messages with positive bandwidth.");
    }
}
