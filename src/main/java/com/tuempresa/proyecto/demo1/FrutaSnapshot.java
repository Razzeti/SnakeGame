package com.tuempresa.proyecto.demo1;

import java.io.Serializable;

public class FrutaSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    public final Coordenada coordenada;
    public final int valor;
    public final int colorRgb;

    public FrutaSnapshot(Coordenada coordenada, int valor, int colorRgb) {
        this.coordenada = new Coordenada(coordenada);
        this.valor = valor;
        this.colorRgb = colorRgb;
    }
}
