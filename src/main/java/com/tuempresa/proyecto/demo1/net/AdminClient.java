package com.tuempresa.proyecto.demo1.net;

import com.tuempresa.proyecto.demo1.game.GameConfig;
import com.tuempresa.proyecto.demo1.net.dto.AdminDataSnapshot;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.ui.GamePanel;
import com.tuempresa.proyecto.demo1.util.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class AdminClient {

    private JFrame frame;
    private GamePanel gamePanel;
    private JTable playerDashboard;
    private DefaultTableModel dashboardModel;
    private JButton kickPlayerButton;
    private JButton startGameButton;
    private JButton resetGameButton;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public static void main(String[] args) {
        Logger.setLogFile(GameConfig.LOG_FILE_ADMIN);
        SwingUtilities.invokeLater(() -> {
            try {
                new AdminClient().start();
            } catch (IOException e) {
                Logger.error("Failed to start Admin Client", e);
                JOptionPane.showMessageDialog(null, "Could not connect to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void start() throws IOException {
        // Setup GUI
        frame = new JFrame("Game Admin Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Game Spectator View (Center)
        // We need an initial snapshot to create the panel
        GameStateSnapshot initialSnapshot = createEmptySnapshot();
        gamePanel = new GamePanel(initialSnapshot);
        frame.add(gamePanel, BorderLayout.CENTER);

        // Player Dashboard (East)
        dashboardModel = new DefaultTableModel(new String[]{"ID", "IP", "Ping (ms)", "Score", "Status", "Uptime (s)"}, 0);
        playerDashboard = new JTable(dashboardModel);
        JScrollPane dashboardScrollPane = new JScrollPane(playerDashboard);
        dashboardScrollPane.setPreferredSize(new Dimension(450, 0));
        frame.add(dashboardScrollPane, BorderLayout.EAST);

        // Control Panel (South)
        JPanel controlPanel = new JPanel();
        startGameButton = new JButton("Start Game");
        resetGameButton = new JButton("Reset Game");
        kickPlayerButton = new JButton("Kick Player");
        kickPlayerButton.setEnabled(false); // Disabled until a player is selected

        controlPanel.add(startGameButton);
        controlPanel.add(resetGameButton);
        controlPanel.add(kickPlayerButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);

        // Connect to server and start listening
        connectAndListen();
    }

    private GameStateSnapshot createEmptySnapshot() {
        return new GameStateSnapshot(GameConfig.ANCHO_TABLERO, GameConfig.ALTO_TABLERO,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                com.tuempresa.proyecto.demo1.model.GamePhase.WAITING_FOR_PLAYERS);
    }

    private void connectAndListen() {
        try {
            socket = new Socket(GameConfig.DEFAULT_HOST, GameConfig.DEFAULT_PORT + 1);
            // To avoid deadlock, the client should initialize streams in the reverse order of the server.
            // Server does: out -> in. Client must do: in -> out.
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Flush the stream header immediately.
        } catch (IOException e) {
            Logger.error("Admin client connection failed", e);
            JOptionPane.showMessageDialog(frame, "Could not connect to the admin port: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Add action listeners
        startGameButton.addActionListener(e -> sendCommand("START_GAME"));
        resetGameButton.addActionListener(e -> sendCommand("RESET_GAME"));
        playerDashboard.getSelectionModel().addListSelectionListener(e -> {
            kickPlayerButton.setEnabled(playerDashboard.getSelectedRow() != -1);
        });
        kickPlayerButton.addActionListener(e -> {
            int selectedRow = playerDashboard.getSelectedRow();
            if (selectedRow != -1) {
                String playerId = (String) dashboardModel.getValueAt(selectedRow, 0);
                sendCommand("KICK_PLAYER " + playerId);
            }
        });


        // Thread to listen for continuous updates from the server
        Thread listenerThread = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    Object serverObject = in.readObject();
                    if (serverObject instanceof GameStateSnapshot) {
                        SwingUtilities.invokeLater(() -> {
                            gamePanel.setEstado((GameStateSnapshot) serverObject);
                            gamePanel.repaint();
                        });
                    } else if (serverObject instanceof AdminDataSnapshot) {
                        SwingUtilities.invokeLater(() -> updateDashboard((AdminDataSnapshot) serverObject));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (!socket.isClosed()) {
                    Logger.error("Lost connection to server", e);
                    JOptionPane.showMessageDialog(frame, "Lost connection to the server.", "Connection Lost", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void sendCommand(String command) {
        if (out == null) {
            Logger.error("Cannot send command, output stream is not initialized.");
            return;
        }
        Logger.info("Sending command to server: " + command);
        try {
            out.writeObject(command);
            out.flush();
        } catch (IOException e) {
            Logger.error("Failed to send command '" + command + "'", e);
            JOptionPane.showMessageDialog(frame, "Error sending command: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
            // Consider closing the connection here if the error is fatal
        }
    }

    private void updateDashboard(AdminDataSnapshot data) {
        dashboardModel.setRowCount(0); // Clear existing data
        for (com.tuempresa.proyecto.demo1.net.dto.PlayerData player : data.getPlayers()) {
            dashboardModel.addRow(new Object[]{
                    player.getPlayerId(),
                    player.getIpAddress(),
                    player.getPingMs(),
                    player.getScore(),
                    player.getStatus(),
                    player.getConnectionDurationSeconds()
            });
        }
    }
}
