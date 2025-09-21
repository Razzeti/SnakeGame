package com.tuempresa.proyecto.demo1;

public class Fruta implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Coordenada coordenada;
    private int valor;
    private int colorRgb;

    public Fruta(Coordenada coordenada, int valor, int colorRgb) {
        this.coordenada = coordenada;
        this.valor = valor;
        this.colorRgb = colorRgb;
    }

    // Copy constructor for deep copy / snapshots
    public Fruta(Fruta other) {
        this.coordenada = new Coordenada(other.coordenada);
        this.valor = other.valor;
        this.colorRgb = other.colorRgb;
    }

    // Getters
    public Coordenada getCoordenada() {
        return coordenada;
    }

    // Returns an AWT Color for rendering, constructed on demand
    public java.awt.Color getColor() {
        return new java.awt.Color(colorRgb);
    }

    // Returns the primitive rgb for serialization/networking
    public int getColorRgb() {
        return colorRgb;
    }

    public int getValor() {
        return valor;
    }

}