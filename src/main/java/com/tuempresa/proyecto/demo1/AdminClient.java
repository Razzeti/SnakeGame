package com.tuempresa.proyecto.demo1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AdminClient {

    public static void main(String[] args) {
        Logger.setLogFile(GameConfig.LOG_FILE_ADMIN);
        Logger.info("Iniciando cliente de administración...");

        // El puerto de admin es el puerto principal + 1
        int adminPort = GameConfig.DEFAULT_PORT + 1;

        try (Socket socket = new Socket(GameConfig.DEFAULT_HOST, adminPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner consoleInput = new Scanner(System.in)) {

            Logger.info("Conectado al puerto de administración del servidor: " + adminPort);
            System.out.println("Conectado al puerto de administración del servidor.");

            // Hilo para escuchar respuestas del servidor de forma asíncrona
            Thread serverListener = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        Logger.info("Respuesta del Servidor: " + serverResponse);
                        System.out.println("Respuesta del Servidor: " + serverResponse);
                    }
                } catch (IOException e) {
                    // Si el socket se cierra desde el hilo principal, esto es esperado.
                    Logger.warn("Conexión con el servidor cerrada.");
                    System.out.println("Conexión con el servidor cerrada.");
                }
            });
            serverListener.setDaemon(true);
            serverListener.start();

            System.out.println("Escriba un comando y presione Enter. Escriba 'exit' para salir.");
            System.out.println("Comandos disponibles: START_GAME, RESET_GAME, LIST_PLAYERS, SHUTDOWN");

            while (consoleInput.hasNextLine()) {
                String command = consoleInput.nextLine();
                Logger.info("Enviando comando: " + command);
                out.println(command); // Enviar comando al servidor

                if ("exit".equalsIgnoreCase(command) || "SHUTDOWN".equalsIgnoreCase(command)) {
                    Logger.info("Comando de salida recibido. Cerrando cliente.");
                    break; // Salir del bucle si el usuario escribe 'exit' o 'SHUTDOWN'
                }
            }

        } catch (IOException e) {
            Logger.error("Error de conexión con el servidor de administración", e);
            System.err.println("Error de conexión con el servidor de administración: " + e.getMessage());
        } finally {
            Logger.info("Cliente de administración desconectado.");
            System.out.println("Cliente de administración desconectado.");
        }
    }
}
