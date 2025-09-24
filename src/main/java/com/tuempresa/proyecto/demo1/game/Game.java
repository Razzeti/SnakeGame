package com.tuempresa.proyecto.demo1.game;

import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.model.GameState;
import com.tuempresa.proyecto.demo1.model.Snake;
import com.tuempresa.proyecto.demo1.net.AdminClient;
import com.tuempresa.proyecto.demo1.net.GameClient;
import com.tuempresa.proyecto.demo1.net.GameServer;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.ui.GraphicalView;
import com.tuempresa.proyecto.demo1.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Game {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Game::createAndShowMainMenu);
    }

    private static void createAndShowMainMenu() {
        JFrame frame = new JFrame("Snake Game - Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 250);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton singlePlayerButton = new JButton("Single Player");
        singlePlayerButton.addActionListener(e -> {
            frame.dispose();
            Logger.info("Iniciando en modo un jugador...");
            startSinglePlayerGame();
        });

        JButton hostGameButton = new JButton("Host Game");
        hostGameButton.addActionListener(e -> {
            frame.dispose();
            Logger.setLogFile(GameConfig.LOG_FILE_SERVER);
            Logger.info("Iniciando en modo servidor...");
            new GameServer().start();
        });

        JButton joinGameButton = new JButton("Join Game");
        joinGameButton.addActionListener(e -> {
            frame.dispose();
            showJoinGameDialog();
        });

        JButton addBotButton = new JButton("Add Bot");
        addBotButton.addActionListener(e -> {
            frame.dispose();
            showBotConnectionDialog();
        });

        JButton adminClientButton = new JButton("Join as Admin");
        adminClientButton.addActionListener(e -> {
            frame.dispose();
            showAdminLoginDialog();
        });

        panel.add(singlePlayerButton);
        panel.add(hostGameButton);
        panel.add(joinGameButton);
        panel.add(addBotButton);
        panel.add(adminClientButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void showAdminLoginDialog() {
        JTextField hostField = new JTextField(GameConfig.DEFAULT_HOST);
        JTextField portField = new JTextField(String.valueOf(GameConfig.DEFAULT_PORT));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Server IP:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Admin Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());

            Logger.setLogFile(GameConfig.LOG_FILE_ADMIN);
            Logger.info("Iniciando en modo admin...");

            new Thread(() -> {
                new AdminClient().start(host, port);
            }).start();
        }
    }

    private static void showBotConnectionDialog() {
        JTextField hostField = new JTextField(GameConfig.DEFAULT_HOST);
        JTextField portField = new JTextField(String.valueOf(GameConfig.DEFAULT_PORT));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Server IP:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Bot",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());

            Logger.info("Añadiendo un bot al juego...");
            new Thread(() -> {
                try {
                    Bot bot = new Bot();
                    bot.start(host, port);
                } catch (Exception ex) {
                    Logger.error("Falló al iniciar el bot: " + ex.getMessage(), ex);
                }
            }).start();
        }
    }

    private static void showJoinGameDialog() {
        JTextField hostField = new JTextField(GameConfig.DEFAULT_HOST);
        JTextField portField = new JTextField(String.valueOf(GameConfig.DEFAULT_PORT));
        JTextField nameField = new JTextField("Player" + (int)(Math.random() * 1000));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Server IP:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Player Name:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Join Game",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());
            String playerName = nameField.getText();
            String playerId = playerName + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);


            Logger.setLogFile(GameConfig.LOG_FILE_CLIENT);
            Logger.info("Iniciando en modo cliente con ID: " + playerId);

            new Thread(() -> {
                try {
                    GameClient client = new GameClient();
                    client.start(host, port, playerId); // Ahora se envía el ID
                } catch (IOException ex) {
                    Logger.error("No se pudo conectar al servidor: " + ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Could not connect to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    private static void startSinglePlayerGame() {
        // --- CONFIGURACIÓN INICIAL ---
        GameState estado = new GameState(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO);
        // For single player, the game starts immediately.
        estado.setGamePhase(com.tuempresa.proyecto.demo1.model.GamePhase.IN_PROGRESS);

        Snake jugador1 = new Snake("JugadorA", GameConfig.POSICION_INICIAL_JUGADOR_1);
        estado.getSerpientes().add(jugador1);

        java.util.concurrent.ConcurrentHashMap<String, Direccion> acciones = new java.util.concurrent.ConcurrentHashMap<>();
        AtomicReference<Direccion> direccionActual = new AtomicReference<>(Direccion.DERECHA);
        GameLogic gameLogic = new GameLogic();
        gameLogic.generarFruta(estado);

        // --- CREACIÓN DE LA INTERFAZ GRÁFICA ---
        SwingUtilities.invokeLater(() -> {
            GameStateSnapshot initialSnapshot = estado.snapshot().toSnapshotDto();
            GraphicalView view = new GraphicalView(initialSnapshot, direccionActual);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GameLoop");
                t.setDaemon(true);
                return t;
            });

            Runnable tick = () -> {
                if (estado.getGamePhase() == com.tuempresa.proyecto.demo1.model.GamePhase.IN_PROGRESS) {
                    acciones.put("JugadorA", direccionActual.get());
                    gameLogic.actualizar(estado, acciones);
                    if (estado.getSerpientes().isEmpty()) {
                        estado.setGamePhase(com.tuempresa.proyecto.demo1.model.GamePhase.GAME_ENDED);
                        Logger.info("Juego en modo un jugador terminado.");
                    }
                }

                GameStateSnapshot snapshot = estado.snapshot().toSnapshotDto();
                SwingUtilities.invokeLater(() -> {
                    view.actualizarEstado(snapshot);
                    view.repaint();
                });
            };

            scheduler.scheduleAtFixedRate(tick, 0, GameConfig.MILIS_POR_TICK, TimeUnit.MILLISECONDS);

            view.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    scheduler.shutdownNow();
                    Logger.info("Ventana de juego cerrada, finalizando loop de un jugador.");
                }
            });
        });
    }
}