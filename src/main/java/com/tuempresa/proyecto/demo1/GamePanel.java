// GamePanel.java
package com.tuempresa.proyecto.demo1;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GamePanel extends JPanel {

    private final java.awt.Font scoreFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 16);
    private final java.awt.Font gameOverFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 40);
    private final java.awt.Color scoreColor = java.awt.Color.WHITE;
    private final java.awt.Color gameOverColor = java.awt.Color.ORANGE;
    private GameState estado; // kept for compatibility but not used for rendering
    private GameStateSnapshot snapshot;
    // ANTES: private final int TILE_SIZE = 20; // <--- SE ELIMINA ESTA LÍNEA

    public GamePanel(GameStateSnapshot inicial) {
        this.snapshot = inicial;
        int panelWidth = inicial.width * Constants.DEFAULT_TILE_SIZE;
        int panelHeight = inicial.height * Constants.DEFAULT_TILE_SIZE;
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(Color.BLACK);
    }

    // For backward compatibility (not used by new loop)
    public void setEstado(GameState estado) { this.estado = estado; }

    // Update snapshot DTO for rendering (EDT only)
    public void setEstado(GameStateSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // limpiar una vez
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Render from DTO snapshot (if available)
        if (snapshot != null) {
            // Draw fruits
            for (FrutaSnapshot fruta : snapshot.frutas) {
                g2d.setColor(new java.awt.Color(fruta.colorRgb));
                Coordenada pos = fruta.coordenada;
                g2d.fillOval(pos.x * Constants.DEFAULT_TILE_SIZE, pos.y * Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE);
            }

            // Draw snakes
            for (SnakeSnapshot serpiente : snapshot.snakes) {
                // body
                g2d.setPaint(new java.awt.GradientPaint(0, 0, Color.GREEN, Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, Color.GREEN.darker()));
                for (int i = 1; i < serpiente.cuerpo.size(); i++) {
                    Coordenada parteCuerpo = serpiente.cuerpo.get(i);
                    g2d.fillRoundRect(parteCuerpo.x * Constants.DEFAULT_TILE_SIZE, parteCuerpo.y * Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, 10, 10);
                }
                // head
                g2d.setColor(Color.ORANGE);
                Coordenada cabeza = serpiente.cuerpo.get(0);
                g2d.fillRoundRect(cabeza.x * Constants.DEFAULT_TILE_SIZE, cabeza.y * Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, Constants.DEFAULT_TILE_SIZE, 20, 20);
            }
        }

        // Dibujar puntajes y estado de juego usando snapshot si está disponible
        g2d.setColor(scoreColor);
        g2d.setFont(scoreFont);
        int yOffset = 20;
        if (snapshot != null) {
            for (SnakeSnapshot serpiente : snapshot.snakes) {
                g2d.drawString(serpiente.idJugador + ": " + serpiente.puntaje, 10, yOffset);
                yOffset += 20;
            }
            if (!snapshot.juegoActivo) {
                g2d.setColor(gameOverColor);
                g2d.setFont(gameOverFont);
                g2d.drawString("GAME OVER", getWidth() / 4, getHeight() / 2);
            }
        } else if (estado != null) {
            for (Snake serpiente : estado.getSerpientes()) {
                g2d.drawString(serpiente.getIdJugador() + ": " + serpiente.getPuntaje(), 10, yOffset);
                yOffset += 20;
            }
            if (!estado.isJuegoActivo()) {
                g2d.setColor(gameOverColor);
                g2d.setFont(gameOverFont);
                g2d.drawString("GAME OVER", getWidth() / 4, getHeight() / 2);
            }
        }
    }
}