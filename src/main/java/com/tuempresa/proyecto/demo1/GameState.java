// GameState.java (versión modificada)
package com.tuempresa.proyecto.demo1;

import java.util.ArrayList;

public class GameState {

    private byte[][] tablero;
    private ArrayList<Snake> serpientes;
    private ArrayList<Fruta> frutas; // <-- CAMBIO DE Coordenada A Fruta
    private boolean juegoActivo;

    // Deep copy / snapshot constructor
    public GameState(GameState other) {
        // copy tablero
        byte[][] otherTablero = other.tablero;
        this.tablero = new byte[otherTablero.length][otherTablero[0].length];
        for (int i = 0; i < otherTablero.length; i++) {
            System.arraycopy(otherTablero[i], 0, this.tablero[i], 0, otherTablero[i].length);
        }

        // copy serpientes
        this.serpientes = new ArrayList<>();
        for (Snake s : other.serpientes) {
            this.serpientes.add(new Snake(s));
        }

        // copy frutas
        this.frutas = new ArrayList<>();
        for (Fruta f : other.frutas) {
            this.frutas.add(new Fruta(f));
        }

        this.juegoActivo = other.juegoActivo;
    }

    public GameState(int ancho, int alto) {
        this.tablero = new byte[alto][ancho];
        this.serpientes = new ArrayList<>();
        this.frutas = new ArrayList<>(); // Se inicializa la nueva lista
        this.juegoActivo = true;
    }

    // Create a deep copy snapshot
    public GameState snapshot() {
        return new GameState(this);
    }

    // Convert to lightweight DTO snapshot for networking/UI
    public GameStateSnapshot toSnapshotDto() {
        int height = tablero.length;
        int width = tablero[0].length;
        java.util.List<SnakeSnapshot> snakeDtos = new java.util.ArrayList<>();
        for (Snake s : this.serpientes) {
            java.util.List<Coordenada> body = new java.util.ArrayList<>();
            for (Coordenada c : s.getCuerpo()) body.add(new Coordenada(c));
            snakeDtos.add(new SnakeSnapshot(s.getIdJugador(), s.getPuntaje(), body));
        }

        java.util.List<FrutaSnapshot> frutaDtos = new java.util.ArrayList<>();
        for (Fruta f : this.frutas) {
            frutaDtos.add(new FrutaSnapshot(new Coordenada(f.getCoordenada()), f.getValor(), f.getColorRgb()));
        }

        return new GameStateSnapshot(width, height, snakeDtos, frutaDtos, this.juegoActivo);
    }

    // --- Getters ---
    public byte[][] getTablero() { return tablero; }
    public ArrayList<Snake> getSerpientes() { return serpientes; }
    public ArrayList<Fruta> getFrutas() { return frutas; } // <-- CAMBIO
    public boolean isJuegoActivo() { return juegoActivo; }

    // --- Setters ---
    public void setTablero(byte[][] tablero) { this.tablero = tablero; }
    public void setSerpientes(ArrayList<Snake> serpientes) { this.serpientes = serpientes; }
    public void setFrutas(ArrayList<Fruta> frutas) { this.frutas = frutas; } // <-- CAMBIO
    public void setJuegoActivo(boolean juegoActivo) { this.juegoActivo = juegoActivo; }
}