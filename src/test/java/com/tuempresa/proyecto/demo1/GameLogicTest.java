package com.tuempresa.proyecto.demo1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {

    private GameLogic gameLogic;
    private GameState gameState;
    private ConcurrentHashMap<String, Direccion> acciones;

    @BeforeEach
    void setUp() {
        gameLogic = new GameLogic();
        gameState = new GameState(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO);
        acciones = new ConcurrentHashMap<>();
    }

    @Test
    @DisplayName("La serpiente debe moverse correctamente a la derecha")
    void testActualizar_MoverDerecha() {
        Snake snake = new Snake("Player1", new Coordenada(10, 10));
        gameState.getSerpientes().add(snake);
        acciones.put("Player1", Direccion.DERECHA);

        Coordenada cabezaOriginal = new Coordenada(snake.getHead());
        gameLogic.actualizar(gameState, acciones);

        assertEquals(cabezaOriginal.x + 1, snake.getHead().x, "La cabeza de la serpiente no se movió a la derecha.");
        assertEquals(cabezaOriginal.y, snake.getHead().y, "La cabeza de la serpiente no debe cambiar su coordenada Y.");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10, IZQUIERDA",  // Colisión con borde izquierdo
            "29, 10, DERECHA", // Colisión con borde derecho (ANCHO_TABLERO - 1)
            "10, 0, ARRIBA",    // Colisión con borde superior
            "10, 19, ABAJO"     // Colisión con borde inferior (ALTO_TABLERO - 1)
    })
    @DisplayName("La serpiente debe ser eliminada al colisionar con los bordes")
    void testActualizar_ColisionBordes(int startX, int startY, Direccion direccion) {
        Snake snake = new Snake("Player1", new Coordenada(startX, startY));
        gameState.getSerpientes().add(snake);
        acciones.put("Player1", direccion);

        gameLogic.actualizar(gameState, acciones);

        assertTrue(gameState.getSerpientes().isEmpty(), "La serpiente debería haber sido eliminada tras la colisión.");
    }

    @Test
    @DisplayName("La serpiente debe crecer después de comer una fruta")
    void testActualizar_ComerFruta() {
        Snake snake = new Snake("Player1", new Coordenada(10, 10));
        gameState.getSerpientes().add(snake);
        // Colocamos una fruta justo delante de la serpiente
        Coordenada posFrutaOriginal = new Coordenada(11, 10);
        Fruta fruta = new Fruta(posFrutaOriginal, 1, 0);
        gameState.getFrutas().add(fruta);
        acciones.put("Player1", Direccion.DERECHA);

        int puntajeInicial = snake.getPuntaje();

        gameLogic.actualizar(gameState, acciones);

        assertTrue(snake.getPuntaje() > puntajeInicial, "El puntaje debería aumentar después de comer.");
        // La serpiente come, por lo que su cola no se mueve, resultando en crecimiento.
        // El segmento a crecer se manejará en el siguiente tick.
        assertEquals(2, snake.getCuerpo().size(), "El cuerpo de la serpiente debería haber crecido a 2 segmentos.");

        // Se come una fruta, pero se genera otra, así que el total sigue siendo 1.
        assertEquals(1, gameState.getFrutas().size(), "Debería haber solo una fruta en el tablero.");
        // Y la nueva fruta no debe estar en la misma posición que la anterior.
        assertNotEquals(posFrutaOriginal, gameState.getFrutas().get(0).getCoordenada(), "La nueva fruta no debería estar en la misma posición que la fruta comida.");
    }

    @Test
    @DisplayName("Una serpiente debe ser eliminada al colisionar con otra")
    void testActualizar_ColisionEntreSerpientes() {
        Snake snake1 = new Snake("Player1", new Coordenada(10, 10));
        Snake snake2 = new Snake("Player2", new Coordenada(12, 10));
        gameState.getSerpientes().add(snake1);
        gameState.getSerpientes().add(snake2);

        acciones.put("Player1", Direccion.DERECHA); // snake1 se moverá a (11, 10)
        acciones.put("Player2", Direccion.IZQUIERDA); // snake2 se moverá a (11, 10)

        gameLogic.actualizar(gameState, acciones);

        // Ambas serpientes intentan ocupar la misma casilla (11,10) al mismo tiempo.
        // El resultado exacto (quién sobrevive) puede depender del orden de procesamiento,
        // pero al menos una debe ser eliminada. En la implementación actual, ambas son eliminadas.
        assertTrue(gameState.getSerpientes().size() < 2, "Al menos una serpiente debería ser eliminada.");
    }

    @Test
    @DisplayName("El snapshot del estado no debe ser afectado por cambios posteriores en el estado original")
    void testInmutabilidadSnapshot() {
        Snake snake = new Snake("Player1", new Coordenada(5, 5));
        gameState.getSerpientes().add(snake);

        // Crear un snapshot del estado inicial
        GameStateSnapshot snapshotInicial = gameState.toSnapshotDto();

        // Modificar el estado original
        acciones.put("Player1", Direccion.DERECHA);
        gameLogic.actualizar(gameState, acciones);

        // Verificar que el snapshot no ha cambiado
        Coordenada cabezaEnSnapshot = snapshotInicial.snakes.get(0).cuerpo.get(0);
        assertEquals(5, cabezaEnSnapshot.x, "La coordenada X del snapshot no debe cambiar.");
        assertEquals(5, cabezaEnSnapshot.y, "La coordenada Y del snapshot no debe cambiar.");
    }
}
