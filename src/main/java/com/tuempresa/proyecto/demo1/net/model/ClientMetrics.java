package com.tuempresa.proyecto.demo1.net.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds all server-side tracked metrics for a single connected client.
 * This is not a DTO, but a live object managed by the server.
 */
public class ClientMetrics {
    private final String playerId;
    private final InetAddress ipAddress;
    private final long connectionTimestamp;
    private volatile long lastPingRttMs = -1; // -1 indicates no ping yet
    private volatile String status = "Connecting"; // e.g., Connecting, Alive, Dead

    public ClientMetrics(String playerId, InetAddress ipAddress) {
        this.playerId = playerId;
        this.ipAddress = ipAddress;
        this.connectionTimestamp = System.currentTimeMillis();
    }

    // Getters
    public String getPlayerId() {
        return playerId;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public long getConnectionTimestamp() {
        return connectionTimestamp;
    }

    public long getLastPingRttMs() {
        return lastPingRttMs;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setLastPingRttMs(long lastPingRttMs) {
        this.lastPingRttMs = lastPingRttMs;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
