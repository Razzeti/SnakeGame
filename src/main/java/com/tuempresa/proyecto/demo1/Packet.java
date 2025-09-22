package com.tuempresa.proyecto.demo1;

import java.io.Serializable;

/**
 * A wrapper for sending pre-serialized data. This avoids the high CPU cost
 * of re-serializing complex objects for every client on every tick and reduces
 * network latency issues.
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 2L; // Changed version UID

    public final byte[] data;

    public Packet(byte[] data) {
        this.data = data;
    }
}
