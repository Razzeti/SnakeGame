package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClientServerIntegrationTest {

    private GameServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        server = new GameServer();
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        // Dar un pequeño margen para que el servidor inicie
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        server.stop();
        try {
            serverThread.join(1000); // Espera a que el hilo del servidor muera
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Test
    @DisplayName("Un cliente debe poder conectarse y recibir un ID de jugador")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testClientConnectsAndReceivesPlayerId() throws Exception {
        GameClient client = new GameClient(true); // true para modo test sin GUI

        // El start se ejecuta en otro hilo para no bloquear el test
        CompletableFuture<Void> clientStartFuture = CompletableFuture.runAsync(() -> {
            try {
                client.start();
            } catch (IOException e) {
                fail("El cliente no pudo iniciar: " + e.getMessage());
            }
        });

        // Esperar a que el cliente obtenga su ID
        String playerId = client.getPlayerIdFuture().get();

        assertNotNull(playerId, "El ID de jugador no debería ser nulo.");
        assertTrue(playerId.startsWith("Jugador_"), "El ID de jugador debe tener el formato esperado.");

        // Detener el cliente para terminar el test limpiamente
        clientStartFuture.cancel(true);
    }

    @Test
    @DisplayName("Dos clientes deben conectarse y recibir IDs de jugador diferentes")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testTwoClientsConnectAndGetDifferentIds() throws Exception {
        GameClient client1 = new GameClient(true);
        GameClient client2 = new GameClient(true);

        CompletableFuture<Void> client1Start = CompletableFuture.runAsync(() -> {
            try { client1.start(); } catch (IOException e) { fail("Cliente 1 falló"); }
        });
        CompletableFuture<Void> client2Start = CompletableFuture.runAsync(() -> {
            try { client2.start(); } catch (IOException e) { fail("Cliente 2 falló"); }
        });

        // Esperar a que ambos clientes obtengan su ID
        String player1Id = client1.getPlayerIdFuture().get();
        String player2Id = client2.getPlayerIdFuture().get();

        assertNotNull(player1Id);
        assertNotNull(player2Id);
        assertNotEquals(player1Id, player2Id, "Los dos clientes deben tener IDs diferentes.");

        client1Start.cancel(true);
        client2Start.cancel(true);
    }
}
