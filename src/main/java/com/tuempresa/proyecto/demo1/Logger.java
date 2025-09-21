package com.tuempresa.proyecto.demo1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public enum LogLevel {
        INFO,
        ERROR,
        WARN
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(LogLevel level, String message) {
        String timestamp = dtf.format(LocalDateTime.now());
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }
}
