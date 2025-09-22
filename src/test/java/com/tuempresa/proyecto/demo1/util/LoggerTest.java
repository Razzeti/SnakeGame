package com.tuempresa.proyecto.demo1.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private static final String TEST_LOG_FILE = "test_logger.log";

    @BeforeEach
    public void setUpStreams() throws IOException {
        System.setOut(new PrintStream(outContent));
        Logger.reset(); // Resetea el estado del logger
        // Asegurarse de que el archivo de log exista y esté vacío
        Files.deleteIfExists(Paths.get(TEST_LOG_FILE));
        Logger.setLogFile(TEST_LOG_FILE);
    }

    @AfterEach
    public void restoreStreams() throws IOException {
        System.setOut(originalOut);
        Logger.close(); // Cierra el archivo para asegurar que todo se ha escrito
        Files.deleteIfExists(Paths.get(TEST_LOG_FILE));
    }

    @Test
    @DisplayName("El logger debe escribir un mensaje de INFO en la consola")
    void testInfo_ConsoleOutput() {
        String message = "Este es un mensaje de info";
        Logger.info(message);
        assertTrue(outContent.toString().contains("INFO") && outContent.toString().contains(message));
    }

    @Test
    @DisplayName("El logger debe escribir un mensaje de WARN en la consola")
    void testWarn_ConsoleOutput() {
        String message = "Este es un mensaje de advertencia";
        Logger.warn(message);
        assertTrue(outContent.toString().contains("WARN") && outContent.toString().contains(message));
    }

    @Test
    @DisplayName("El logger debe escribir un mensaje de ERROR en la consola")
    void testError_ConsoleOutput() {
        String message = "Este es un mensaje de error";
        Logger.error(message);
        assertTrue(outContent.toString().contains("ERROR") && outContent.toString().contains(message));
    }

    @Test
    @DisplayName("El logger debe escribir en el archivo de log")
    void testFileLogging() throws IOException {
        String message = "Este mensaje debe ir al archivo";
        Logger.info(message);

        // Asegurarse de que el logger ha tenido tiempo de escribir
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<String> lines = Files.readAllLines(Paths.get(TEST_LOG_FILE));
        assertTrue(lines.stream().anyMatch(line -> line.contains("INFO") && line.contains(message)));
    }

    @Test
    @DisplayName("El logger debe registrar una excepción en el archivo de log")
    void testExceptionLogging() throws IOException {
        String message = "Error con excepcion";
        Exception ex = new RuntimeException("Test Exception");
        Logger.error(message, ex);

        // Asegurarse de que el logger ha tenido tiempo de escribir
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<String> lines = Files.readAllLines(Paths.get(TEST_LOG_FILE));
        boolean messageFound = lines.stream().anyMatch(line -> line.contains("ERROR") && line.contains(message));
        boolean exceptionFound = lines.stream().anyMatch(line -> line.contains(ex.getClass().getName()));

        assertTrue(messageFound, "El mensaje de error no fue encontrado en el log.");
        assertTrue(exceptionFound, "La traza de la excepción no fue encontrada en el log.");
    }
}
