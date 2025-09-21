package com.tuempresa.proyecto.demo1;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public enum LogLevel {
        INFO, DEBUG, WARN, ERROR
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static void log(LogLevel level, String message) {
        String timestamp = dateFormat.format(new Date());
        System.out.println(String.format("[%s] [%s] %s", timestamp, level, message));
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message);
        throwable.printStackTrace(System.err);
    }
}
