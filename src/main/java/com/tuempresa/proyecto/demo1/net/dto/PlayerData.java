package com.tuempresa.proyecto.demo1.net.dto;

import java.io.Serializable;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final String ipAddress;
    private final long connectionDurationSeconds;
    private final long pingMs;
    private final String status;
    private final int score;

    public PlayerData(String playerId, String ipAddress, long connectionDurationSeconds, long pingMs, String status, int score) {
        this.playerId = playerId;
        this.ipAddress = ipAddress;
        this.connectionDurationSeconds = connectionDurationSeconds;
        this.pingMs = pingMs;
        this.status = status;
        this.score = score;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getConnectionDurationSeconds() {
        return connectionDurationSeconds;
    }

    public long getPingMs() {
        return pingMs;
    }

    public String getStatus() {
        return status;
    }

    public int getScore() {
        return score;
    }
}
