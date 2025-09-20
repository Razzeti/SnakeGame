package com.tuempresa.proyecto.demo1;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class SnakeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String idJugador;
    public final int puntaje;
    public final List<Coordenada> cuerpo;

    public SnakeSnapshot(String idJugador, int puntaje, List<Coordenada> cuerpo) {
        this.idJugador = idJugador;
        this.puntaje = puntaje;
        this.cuerpo = Collections.unmodifiableList(cuerpo);
    }
}
