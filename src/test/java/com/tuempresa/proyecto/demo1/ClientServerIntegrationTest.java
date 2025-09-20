package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class ClientServerIntegrationTest {

    @Test
    public void testClientConnectsToServer() throws InterruptedException {
        Thread serverThread = new Thread(() -> {
            try {
                new GameServer().start();
            } catch (IOException e) {
                // Server thread will exit if it fails to start
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server a moment to start up
        Thread.sleep(1000);

        GameClient client = new GameClient(true); // true for test mode
        Thread clientThread = new Thread(() -> {
            try {
                client.start();
            } catch (IOException e) {
                // Client thread will exit if it fails to connect
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();

        // Give the client a moment to connect
        Thread.sleep(2000);

        assertNotNull(client.getPlayerId(), "Client should have received a player ID");
        assertTrue(client.isConnected(), "Client should be connected to the server");

        // Clean up
        serverThread.interrupt();
        clientThread.interrupt();
    }
}
