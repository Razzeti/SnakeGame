package com.tuempresa.proyecto.demo1.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordenadaTest {

    @Test
    @DisplayName("Dos coordenadas con los mismos valores de X e Y deben ser iguales")
    void testEquals_SameValues_ShouldBeEqual() {
        Coordenada c1 = new Coordenada(10, 20);
        Coordenada c2 = new Coordenada(10, 20);
        assertTrue(c1.equals(c2), "Las coordenadas con los mismos valores deben ser iguales.");
    }

    @Test
    @DisplayName("Dos coordenadas con diferentes valores de X no deben ser iguales")
    void testEquals_DifferentX_ShouldNotBeEqual() {
        Coordenada c1 = new Coordenada(10, 20);
        Coordenada c2 = new Coordenada(15, 20);
        assertFalse(c1.equals(c2), "Las coordenadas con diferentes valores de X no deben ser iguales.");
    }

    @Test
    @DisplayName("Dos coordenadas con diferentes valores de Y no deben ser iguales")
    void testEquals_DifferentY_ShouldNotBeEqual() {
        Coordenada c1 = new Coordenada(10, 20);
        Coordenada c2 = new Coordenada(10, 25);
        assertFalse(c1.equals(c2), "Las coordenadas con diferentes valores de Y no deben ser iguales.");
    }

    @Test
    @DisplayName("Una coordenada no debe ser igual a un objeto de otro tipo")
    @SuppressWarnings("unlikely-arg-type")
    void testEquals_DifferentType_ShouldNotBeEqual() {
        Coordenada c1 = new Coordenada(10, 20);
        String s1 = "No soy una coordenada";
        assertFalse(c1.equals(s1), "Una coordenada no debe ser igual a un objeto de otro tipo.");
    }

    @Test
    @DisplayName("Una coordenada debe ser igual a sí misma")
    void testEquals_SameInstance_ShouldBeEqual() {
        Coordenada c1 = new Coordenada(10, 20);
        assertTrue(c1.equals(c1), "Una coordenada debe ser igual a sí misma.");
    }

    @Test
    @DisplayName("El hashcode de dos coordenadas iguales debe ser el mismo")
    void testHashCode_EqualObjects_ShouldHaveSameHashCode() {
        Coordenada c1 = new Coordenada(10, 20);
        Coordenada c2 = new Coordenada(10, 20);
        assertEquals(c1.hashCode(), c2.hashCode(), "El hashcode de dos coordenadas iguales debe ser el mismo.");
    }

    @Test
    @DisplayName("Es probable que el hashcode de dos coordenadas diferentes sea distinto")
    void testHashCode_DifferentObjects_ShouldHaveDifferentHashCode() {
        Coordenada c1 = new Coordenada(10, 20);
        Coordenada c2 = new Coordenada(15, 25);
        assertNotEquals(c1.hashCode(), c2.hashCode(), "Es muy probable que el hashcode de dos coordenadas diferentes sea distinto.");
    }
}
