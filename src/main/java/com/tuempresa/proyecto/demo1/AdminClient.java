package com.tuempresa.proyecto.demo1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AdminClient {

    private static final String SERVER_IP = "127.0.0.1";
    // El puerto de admin debe ser diferente al de los jugadores
    private static final int ADMIN_PORT = 12346;

    public static void main(String[] args) {
        System.out.println("Iniciando cliente de administración...");
        try (Socket socket = new Socket(SERVER_IP, ADMIN_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner consoleInput = new Scanner(System.in)) {

            System.out.println("Conectado al puerto de administración del servidor.");

            // Hilo para escuchar respuestas del servidor de forma asíncrona
            Thread serverListener = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println("Respuesta del Servidor: " + serverResponse);
                    }
                } catch (IOException e) {
                    // Si el socket se cierra desde el hilo principal, esto es esperado.
                    System.out.println("Conexión con el servidor cerrada.");
                }
            });
            serverListener.setDaemon(true);
            serverListener.start();

            System.out.println("Escriba un comando y presione Enter. Escriba 'exit' para salir.");
            System.out.println("Comandos disponibles: START_GAME, RESET_GAME, LIST_PLAYERS, SHUTDOWN");

            while (consoleInput.hasNextLine()) {
                String command = consoleInput.nextLine();
                out.println(command); // Enviar comando al servidor

                if ("exit".equalsIgnoreCase(command) || "SHUTDOWN".equalsIgnoreCase(command)) {
                    break; // Salir del bucle si el usuario escribe 'exit' o 'SHUTDOWN'
                }
            }

        } catch (IOException e) {
            System.err.println("Error de conexión con el servidor de administración: " + e.getMessage());
        } finally {
            System.out.println("Cliente de administración desconectado.");
        }
    }
}
