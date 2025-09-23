package com.tuempresa.proyecto.demo1.model;

/**
 * Enum que representa las posibles causas de eliminación de una serpiente en el juego.
 */
public enum CausaMuerte {
    /**
     * La serpiente ha chocado contra una de las paredes del tablero.
     */
    COLISION_PARED,

    /**
     * La serpiente ha chocado contra su propio cuerpo.
     */
    COLISION_CUERPO,

    /**
     * La serpiente ha chocado contra el cuerpo de otra serpiente.
     */
    COLISION_OTRO_JUGADOR,

    /**
     * La serpiente ha chocado de frente contra la cabeza de otra serpiente.
     */
    COLISION_CABEZA,

    /**
     * No se ha producido ninguna colisión.
     */
    NINGUNA
}
