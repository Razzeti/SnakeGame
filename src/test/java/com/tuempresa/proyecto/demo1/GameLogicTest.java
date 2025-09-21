package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GameLogicTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testLoggerInfo() {
        String message = "Test message";
        Logger.info(message);
        assertTrue(outContent.toString().contains("INFO"));
        assertTrue(outContent.toString().contains(message));
    }

    @Test
    public void testGenerarFrutaPerformance() {
        GameState estado = new GameState(50, 50);
        Snake snake = new Snake("TestSnake", new Coordenada(0, 0));
        // Make the snake very long to make the board crowded
        for (int i = 1; i < 2000; i++) {
            snake.getCuerpo().add(new Coordenada(i / 50, i % 50));
        }
        estado.getSerpientes().add(snake);

        // Warm-up
        for (int i = 0; i < 10; i++) {
            GameLogic.generarFruta(estado);
            estado.getFrutas().clear();
        }

        List<Long> executionTimes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long startTime = System.nanoTime();
            GameLogic.generarFruta(estado);
            long endTime = System.nanoTime();
            executionTimes.add(endTime - startTime);
            estado.getFrutas().clear();
        }

        long averageTime = executionTimes.stream().mapToLong(Long::longValue).sum() / executionTimes.size();
        System.out.println("Average time for generarFruta: " + TimeUnit.NANOSECONDS.toMillis(averageTime) + " ms");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(averageTime) < 100, "generarFruta should be fast even on a crowded board");
    }

    @Test
    public void testSnakeSelfCollision() {
        GameState estado = new GameState(20, 20);
        Snake snake = new Snake("TestSnake", new Coordenada(5, 5));
        snake.getCuerpo().add(new Coordenada(5, 6));
        snake.getCuerpo().add(new Coordenada(6, 6));
        snake.getCuerpo().add(new Coordenada(6, 5));
        snake.setSegmentosPorCrecer(0);
        estado.getSerpientes().add(snake);

        GameLogic gameLogic = new GameLogic();
        java.util.concurrent.ConcurrentHashMap<String, Direccion> acciones = new java.util.concurrent.ConcurrentHashMap<>();
        acciones.put("TestSnake", Direccion.DERECHA);

        gameLogic.actualizar(estado, acciones);
        assertEquals(0, estado.getSerpientes().size());
    }
}
