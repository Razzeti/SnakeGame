package com.tuempresa.proyecto.demo1.net.dto;

import com.tuempresa.proyecto.demo1.model.Coordenada;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class SnakeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String idJugador;
    public final int puntaje;
    public final List<Coordenada> cuerpo;
    public final int segmentosPorCrecer;
    public final int colorRgb;

    public SnakeSnapshot(String idJugador, int puntaje, List<Coordenada> cuerpo, int segmentosPorCrecer, int colorRgb) {
        this.idJugador = idJugador;
        this.puntaje = puntaje;
        this.cuerpo = Collections.unmodifiableList(cuerpo);
        this.segmentosPorCrecer = segmentosPorCrecer;
        this.colorRgb = colorRgb;
    }
}
