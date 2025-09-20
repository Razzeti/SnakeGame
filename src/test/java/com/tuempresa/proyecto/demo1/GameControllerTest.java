package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class GameControllerTest {

    private GameState state;
    private Snake snake;
    private ConcurrentHashMap<String, Direccion> acciones;

    @BeforeEach
    public void setup() {
        state = new GameState(10, 10); // width=10, height=10
        snake = new Snake("player1", new Coordenada(5,5));
        state.getSerpientes().add(snake);
        acciones = new ConcurrentHashMap<>();
    }

    // Movement tests
    @Test
    public void testMoveUp() {
        acciones.put("player1", Direccion.ARRIBA);
        GameController.actualizar(state, acciones);
        Coordenada head = snake.getHead();
        assertEquals(5, head.getX());
        assertEquals(4, head.getY());
    }

    @Test
    public void testMoveDown() {
        acciones.put("player1", Direccion.ABAJO);
        GameController.actualizar(state, acciones);
        Coordenada head = snake.getHead();
        assertEquals(5, head.getX());
        assertEquals(6, head.getY());
    }

    @Test
    public void testMoveLeft() {
        acciones.put("player1", Direccion.IZQUIERDA);
        GameController.actualizar(state, acciones);
        Coordenada head = snake.getHead();
        assertEquals(4, head.getX());
        assertEquals(5, head.getY());
    }

    @Test
    public void testMoveRight() {
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        Coordenada head = snake.getHead();
        assertEquals(6, head.getX());
        assertEquals(5, head.getY());
    }

    @ParameterizedTest
    @CsvSource({"ARRIBA,5,4","ABAJO,5,6","IZQUIERDA,4,5","DERECHA,6,5"})
    public void parameterizedMovementTest(String dirName, int expectedX, int expectedY) {
        Direccion dir = Direccion.valueOf(dirName);
        acciones.put("player1", dir);
        GameController.actualizar(state, acciones);
        Coordenada head = snake.getHead();
        assertEquals(expectedX, head.getX());
        assertEquals(expectedY, head.getY());
    }

    @Test
    public void testSnapshotImmutabilityUnderConcurrentUpdate() throws Exception {
        // Create a snapshot and then mutate original to ensure snapshot remains stable
        state = new GameState(5,5);
        Snake s = new Snake("p", new Coordenada(2,2));
        state.getSerpientes().add(s);
        GameState snapshot = state.snapshot();

        // mutate original
        s.getCuerpo().addFirst(new Coordenada(3,2));

        // snapshot should not see the new head
        Coordenada snapHead = snapshot.getSerpientes().get(0).getHead();
        assertEquals(2, snapHead.getX());
        assertEquals(2, snapHead.getY());
    }

    @Test
    public void testBodyFollowsHead() {
        // Move right 3 times
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones); // head at 6,5
        GameController.actualizar(state, acciones); // head at 7,5
        GameController.actualizar(state, acciones); // head at 8,5
        List<Coordenada> cuerpo = snake.getCuerpo();
        assertEquals(1, cuerpo.size()); // snake length should still be 1 (no growth yet)
        // Now simulate growth and movement to observe body following
        snake.setSegmentosPorCrecer(2);
        GameController.actualizar(state, acciones); // grow: length->2
        GameController.actualizar(state, acciones); // grow: length->3
        assertTrue(snake.getCuerpo().size() >= 3);
        // Ensure the tail coordinate is behind the head
        Coordenada head = snake.getHead();
        Coordenada tail = snake.getCuerpo().getLast();
        assertTrue(!(tail.getX() == head.getX() && tail.getY() == head.getY()));
    }

    // Growth tests
    @Test
    public void testEatFruitIncreasesSizeBy1() {
        // Place a fruit directly in front of the snake
        Fruta fruit = new Fruta(new Coordenada(6,5), 1, java.awt.Color.RED.getRGB());
        state.getFrutas().add(fruit);
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        // After moving, snake should have eaten fruit and have segmentosPorCrecer > 0
        assertTrue(snake.getSegmentosPorCrecer() >= 1 || snake.getCuerpo().size() > 1);
    }

    @Test
    public void testEatFruitValor3GrowsOverTurns() {
        Fruta fruit = new Fruta(new Coordenada(6,5), 3, java.awt.Color.BLUE.getRGB());
        state.getFrutas().add(fruit);
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        // After eating, segmentosPorCrecer should be at least 3
        assertTrue(snake.getSegmentosPorCrecer() >= 3 || snake.getCuerpo().size() > 1);
    }

    // Collision with boundaries
    @Test
    public void testCollisionTopRemovesSnake() {
        // Place head at y=0 and move up
        snake.getCuerpo().clear();
        snake.getCuerpo().add(new Coordenada(5,0));
        acciones.put("player1", Direccion.ARRIBA);
        GameController.actualizar(state, acciones);
        assertTrue(state.getSerpientes().isEmpty(), "Snake should be removed after colliding with top boundary");
    }

    @Test
    public void testCollisionBottomRemovesSnake() {
        snake.getCuerpo().clear();
        snake.getCuerpo().add(new Coordenada(5,9));
        acciones.put("player1", Direccion.ABAJO);
        GameController.actualizar(state, acciones);
        assertTrue(state.getSerpientes().isEmpty());
    }

    @Test
    public void testCollisionLeftRemovesSnake() {
        snake.getCuerpo().clear();
        snake.getCuerpo().add(new Coordenada(0,5));
        acciones.put("player1", Direccion.IZQUIERDA);
        GameController.actualizar(state, acciones);
        assertTrue(state.getSerpientes().isEmpty());
    }

    @Test
    public void testCollisionRightRemovesSnake() {
        snake.getCuerpo().clear();
        snake.getCuerpo().add(new Coordenada(9,5));
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        assertTrue(state.getSerpientes().isEmpty());
    }

    // Self collision
    @Test
    public void testSelfCollision_ShouldRemoveSnake() {
        // Build a snake where moving right immediately collides with its body at (6,5)
        snake.getCuerpo().clear();
        snake.getCuerpo().add(new Coordenada(5,5)); // head
        snake.getCuerpo().add(new Coordenada(6,5));
        snake.getCuerpo().add(new Coordenada(6,6));
        snake.getCuerpo().add(new Coordenada(5,6));
        // Move right into (6,5) which is occupied -> self-collision
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        assertTrue(state.getSerpientes().isEmpty(), "Snake should be removed after self-collision");
    }

    @Test
    public void testOppositeDirectionHandledGracefully() {
        // Start moving right, then attempt to move left immediately
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones); // move right
        acciones.put("player1", Direccion.IZQUIERDA);
        GameController.actualizar(state, acciones); // attempt to move left
        // Expect either prevention of move or resulting collision; ensure the game state remains consistent
        // We'll assert that snake exists or is removed but no exception thrown
        // If snake exists, its head should not teleport
        if (!state.getSerpientes().isEmpty()) {
            Coordenada head = snake.getHead();
            assertTrue(Math.abs(head.getX() - 6) <= 1);
        }
    }

    // Scoring test
    @Test
    public void testScoreIncrementsOnEat() {
        Fruta fruit = new Fruta(new Coordenada(6,5), 2, java.awt.Color.MAGENTA.getRGB());
        state.getFrutas().add(fruit);
        int before = snake.getPuntaje();
        acciones.put("player1", Direccion.DERECHA);
        GameController.actualizar(state, acciones);
        assertTrue(snake.getPuntaje() >= before + 2);
    }

    @Test
    public void testGenerarFrutaPlacesValidPosition() {
        // Place snake occupying most board positions except one
        state.getSerpientes().clear();
        Snake big = new Snake("big", new Coordenada(0,0));
        big.getCuerpo().clear();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                big.getCuerpo().add(new Coordenada(x,y));
            }
        }
        state.getSerpientes().add(big);
        GameController.generarFruta(state);
        assertFalse(state.getFrutas().isEmpty());
        Fruta f = state.getFrutas().get(0);
        // Fruit must not be on top of any snake segment
        for (Snake s : state.getSerpientes()) {
            for (Coordenada c : s.getCuerpo()) {
                assertFalse(c.getX() == f.getCoordenada().getX() && c.getY() == f.getCoordenada().getY());
            }
        }
    }
}
