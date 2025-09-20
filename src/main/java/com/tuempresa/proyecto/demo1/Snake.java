// Snake.java (versión modificada)
package com.tuempresa.proyecto.demo1;

import java.util.LinkedList;

public class Snake implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private LinkedList<Coordenada> cuerpo;
    private java.util.HashSet<Long> ocupadas;
    private int puntaje;
    private String idJugador;
    private int segmentosPorCrecer; // <-- NUEVA VARIABLE

    public Snake(String idJugador, Coordenada posicionInicial) {
        this.idJugador = idJugador;
        this.puntaje = 0;
        this.cuerpo = new LinkedList<>();
        this.cuerpo.addFirst(posicionInicial);
        this.ocupadas = new java.util.HashSet<>();
        this.ocupadas.add(key(posicionInicial));
        this.segmentosPorCrecer = 0;
    }

    // Copy constructor for deep copy / snapshots
    public Snake(Snake other) {
        this.idJugador = other.idJugador;
        this.puntaje = other.puntaje;
        this.segmentosPorCrecer = other.segmentosPorCrecer;
        this.cuerpo = new LinkedList<>();
        this.ocupadas = new java.util.HashSet<>();
        for (Coordenada c : other.cuerpo) {
            Coordenada copy = new Coordenada(c);
            this.cuerpo.add(copy);
            this.ocupadas.add(key(copy));
        }
    }

    // ... (los getters y setters de siempre) ...
    // GETTERS
    private static long key(Coordenada c) {
        return (((long)c.x) << 32) | (c.y & 0xffffffffL);
    }
    public boolean ocupa(Coordenada c) { return ocupadas.contains(key(c)); }
    public LinkedList<Coordenada> getCuerpo() { return cuerpo; }
    public int getPuntaje() { return puntaje; }
    public String getIdJugador() { return idJugador; }
    public Coordenada getHead() { return this.cuerpo.getFirst(); }
    public int getSegmentosPorCrecer() { return segmentosPorCrecer; } // <-- NUEVO GETTER

    // SETTERS
    public void setCuerpo(LinkedList<Coordenada> cuerpo) { this.cuerpo = cuerpo; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }
    public void setIdJugador(String idJugador) { this.idJugador = idJugador; }
    public void setSegmentosPorCrecer(int segmentos) { this.segmentosPorCrecer = segmentos; } // <-- NUEVO SETTER
}