package com.tuempresa.proyecto.demo1.model;

public class Coordenada implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final int x;
    public final int y;

    public Coordenada(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Copy constructor for deep copy / snapshots
    public Coordenada(Coordenada other) {
        this.x = other.x;
        this.y = other.y;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // Método extra útil para debugging (opcional pero recomendado)
    @Override
    public String toString() {
        return "Coordenada{" + "x=" + x + ", y=" + y + '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordenada)) return false;
        Coordenada c = (Coordenada) o;
        return x == c.x && y == c.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}