package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ClientServerIntegrationTest {

    @Test
    public void testClientConnectsToServer() {
        // Iniciar servidor en un hilo aparte
        GameServer server = new GameServer();
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();

        // Darle un respiro al servidor para que inicie los sockets
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Iniciar cliente en otro hilo
        GameClient client = new GameClient(true); // true for test mode
        Thread clientThread = new Thread(() -> {
            try {
                client.start();
            } catch (IOException e) {
                // El futuro se completará excepcionalmente, fallando la prueba.
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();

        try {
            // Esperar a que el cliente confirme la recepción del ID de jugador
            String playerId = client.getPlayerIdFuture().get(5, TimeUnit.SECONDS);

            // Realizar aserciones
            assertNotNull(playerId, "El ID del jugador no debería ser nulo");
            assertTrue(client.isConnected(), "El cliente debería estar conectado");
            assertTrue(playerId.startsWith("Jugador_"), "El ID del jugador debería empezar con 'Jugador_'");

        } catch (Exception e) {
            fail("El cliente no pudo conectarse y recibir un ID de jugador en el tiempo esperado.", e);
        } finally {
            // Limpieza
            serverThread.interrupt();
            clientThread.interrupt();
        }
    }
}
