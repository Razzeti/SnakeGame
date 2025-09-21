package com.tuempresa.proyecto.demo1;

import java.io.Serializable;

public class Ping implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long timestamp;

    public Ping() {
        this.timestamp = System.nanoTime();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
