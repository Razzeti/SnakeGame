package com.tuempresa.proyecto.demo1;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public enum LogLevel {
        INFO,
        ERROR,
        WARN
    }

    private static final String LOG_FILE = "sunday_game.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter writer;

    static {
        try {
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("Could not initialize logger: " + e.getMessage());
            writer = new PrintWriter(System.out);
        }
    }

    public static void log(LogLevel level, String message) {
        String timestamp = dtf.format(LocalDateTime.now());
        String logMessage = "[" + timestamp + "] [" + level + "] " + message;
        System.out.println(logMessage);
        writer.println(logMessage);
    }

    public static void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
