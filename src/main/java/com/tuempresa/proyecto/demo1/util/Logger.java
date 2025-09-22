package com.tuempresa.proyecto.demo1.util;

import com.tuempresa.proyecto.demo1.game.GameConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public enum LogLevel {
        INFO, DEBUG, WARN, ERROR
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static PrintWriter fileWriter;
    private static String logFileName;

    // Static block to initialize the logger
    static {
        // Default log file, can be changed by calling setLogFile
        setLogFile("application.log");
    }

    public static synchronized void setLogFile(String fileName) {
        if (fileName.equals(logFileName)) {
            return; // Avoid re-opening the same file
        }

        if (fileWriter != null) {
            fileWriter.close();
        }

        try {
            fileWriter = new PrintWriter(new FileWriter(fileName, true), true);
            logFileName = fileName;
            info("Logging to file: " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to set log file: " + fileName);
            e.printStackTrace();
        }
    }

    private static synchronized void log(LogLevel level, String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        if (GameConfig.LOG_TO_CONSOLE) {
            System.out.println(logMessage);
        }

        if (fileWriter != null) {
            fileWriter.println(logMessage);
        }
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

    public static synchronized void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message);
        if (throwable != null) {
            if (GameConfig.LOG_TO_CONSOLE) {
                throwable.printStackTrace(System.err);
            }
            if (fileWriter != null) {
                throwable.printStackTrace(fileWriter);
            }
        }
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public static synchronized void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message);
        if (throwable != null) {
            if (GameConfig.LOG_TO_CONSOLE) {
                throwable.printStackTrace(System.err);
            }
            if (fileWriter != null) {
                throwable.printStackTrace(fileWriter);
            }
        }
    }

    // Close the file writer when the application exits
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            close();
        }));
    }

    public static synchronized void close() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }

    public static synchronized void reset() {
        close();
        logFileName = null;
    }
}
