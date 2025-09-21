package com.tuempresa.proyecto.demo1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClientMetrics {
    private final ConcurrentHashMap<String, Long> bytesSent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastPingTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastBandwidthResetTime = new ConcurrentHashMap<>();

    public void recordBytesSent(String playerId, long bytes) {
        bytesSent.merge(playerId, bytes, Long::sum);
    }

    public double getBandwidthKBps(String playerId) {
        long totalBytes = bytesSent.getOrDefault(playerId, 0L);
        long now = System.nanoTime();
        long lastResetTime = lastBandwidthResetTime.getOrDefault(playerId, now);
        long timeDeltaNs = now - lastResetTime;

        if (timeDeltaNs == 0) {
            return 0.0;
        }

        double timeDeltaS = (double) timeDeltaNs / TimeUnit.SECONDS.toNanos(1);
        return (double) totalBytes / 1024 / timeDeltaS;
    }

    public void resetBandwidth(String playerId) {
        bytesSent.put(playerId, 0L);
        lastBandwidthResetTime.put(playerId, System.nanoTime());
    }

    public void recordPing(String playerId) {
        lastPingTime.put(playerId, System.nanoTime());
    }

    public long getLatencyMs(String playerId, long pongTimestamp) {
        long start = lastPingTime.getOrDefault(playerId, pongTimestamp);
        return (pongTimestamp - start) / 1_000_000;
    }
}
