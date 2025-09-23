package com.tuempresa.proyecto.demo1.net.dto;

import java.io.Serializable;
import java.util.List;

public class AdminDataSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<PlayerData> players;

    public AdminDataSnapshot(List<PlayerData> players) {
        this.players = players;
    }

    public List<PlayerData> getPlayers() {
        return players;
    }
}
