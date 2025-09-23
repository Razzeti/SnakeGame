package com.tuempresa.proyecto.demo1.net.dto;

import com.tuempresa.proyecto.demo1.model.GamePhase;

public class GameStateSnapshot implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final int width, height;
    public final java.util.List<SnakeSnapshot> snakes;
    public final java.util.List<FrutaSnapshot> frutas;
    public final GamePhase gamePhase;
    public GameStateSnapshot(int width, int height, java.util.List<SnakeSnapshot> snakes, java.util.List<FrutaSnapshot> frutas, GamePhase gamePhase) {
        this.width = width;
        this.height = height;
        this.snakes = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(snakes));
        this.frutas = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(frutas));
        this.gamePhase = gamePhase;
    }
}