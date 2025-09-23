package com.tuempresa.proyecto.demo1.net.dto;

import com.tuempresa.proyecto.demo1.model.GamePhase;

import java.io.Serializable;
import java.util.List;

public class AdminDataSnapshot implements Serializable {
    private static final long serialVersionUID = 2L; // Version updated

    private final List<PlayerData> players;
    private final GamePhase gamePhase;
    private final GameStateSnapshot gameStateSnapshot; // For spectator view

    public AdminDataSnapshot(List<PlayerData> players, GamePhase gamePhase, GameStateSnapshot gameStateSnapshot) {
        this.players = players;
        this.gamePhase = gamePhase;
        this.gameStateSnapshot = gameStateSnapshot;
    }

    public List<PlayerData> getPlayers() {
        return players;
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public GameStateSnapshot getGameStateSnapshot() {
        return gameStateSnapshot;
    }
}
