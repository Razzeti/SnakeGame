// GamePanel.java
package com.tuempresa.proyecto.demo1.ui;

import com.tuempresa.proyecto.demo1.game.GameConfig;
import com.tuempresa.proyecto.demo1.model.Coordenada;
import com.tuempresa.proyecto.demo1.model.GameState;
import com.tuempresa.proyecto.demo1.model.Snake;
import com.tuempresa.proyecto.demo1.net.dto.FrutaSnapshot;
import com.tuempresa.proyecto.demo1.net.dto.GameStateSnapshot;
import com.tuempresa.proyecto.demo1.net.dto.SnakeSnapshot;
import com.tuempresa.proyecto.demo1.util.Logger;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

public class GamePanel extends JPanel {

    // --- Optimizaciones de Renderizado ---
    // Cachear objetos de UI para evitar crearlos en cada fotograma en paintComponent()
    private static final Paint SNAKE_BODY_PAINT = new java.awt.GradientPaint(
            0, 0, Color.GREEN,
            GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, Color.GREEN.darker()
    );
    private static final Color SNAKE_HEAD_COLOR = Color.ORANGE;
    // --- Fin de Optimizaciones ---

    // Se eliminan las constantes locales, ahora se usa GameConfig
    private GameState estado; // kept for compatibility but not used for rendering
    private GameStateSnapshot snapshot;

    public GamePanel(GameStateSnapshot inicial) {
        this.snapshot = inicial;
        int panelWidth = (inicial != null ? inicial.width : GameConfig.ANCHO_TABLERO) * GameConfig.DEFAULT_TILE_SIZE;
        int panelHeight = (inicial != null ? inicial.height : GameConfig.ALTO_TABLERO) * GameConfig.DEFAULT_TILE_SIZE;
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(GameConfig.COLOR_FONDO);
    }

    // For backward compatibility (not used by new loop)
    public void setEstado(GameState estado) { this.estado = estado; }

    // Update snapshot DTO for rendering (EDT only)
    public void setEstado(GameStateSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void paintComponent(Graphics g) {
        long startTime = 0;
        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            startTime = System.nanoTime();
        }

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // limpiar una vez
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Render from DTO snapshot (if available)
        if (snapshot != null) {
            // Draw fruits
            for (FrutaSnapshot fruta : snapshot.frutas) {
                // Usa el color individual de cada fruta del snapshot
                g2d.setColor(new java.awt.Color(fruta.colorRgb));
                Coordenada pos = fruta.coordenada;
                g2d.fillOval(pos.x * GameConfig.DEFAULT_TILE_SIZE, pos.y * GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE);
            }

            // Draw snakes
            for (SnakeSnapshot serpiente : snapshot.snakes) {
                // body
                g2d.setPaint(SNAKE_BODY_PAINT);
                for (int i = 1; i < serpiente.cuerpo.size(); i++) {
                    Coordenada parteCuerpo = serpiente.cuerpo.get(i);
                    g2d.fillRoundRect(parteCuerpo.x * GameConfig.DEFAULT_TILE_SIZE, parteCuerpo.y * GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, 10, 10);
                }
                // head
                g2d.setColor(SNAKE_HEAD_COLOR);
                Coordenada cabeza = serpiente.cuerpo.get(0);
                g2d.fillRoundRect(cabeza.x * GameConfig.DEFAULT_TILE_SIZE, cabeza.y * GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, GameConfig.DEFAULT_TILE_SIZE, 20, 20);
            }
        }

        // Dibujar puntajes y estado de juego
        g2d.setColor(GameConfig.COLOR_TEXTO);
        g2d.setFont(GameConfig.FUENTE_TEXTO);
        int yOffset = 20;

        if (snapshot != null) {
            // Dibuja los puntajes de cada serpiente
            for (SnakeSnapshot serpiente : snapshot.snakes) {
                g2d.drawString(serpiente.idJugador + ": " + serpiente.puntaje, 10, yOffset);
                yOffset += 20;
            }

            // Muestra un mensaje dependiendo de la fase del juego
            String gameStatusMessage = "";
            if (snapshot.gamePhase == com.tuempresa.proyecto.demo1.model.GamePhase.WAITING_FOR_PLAYERS) {
                gameStatusMessage = "WAITING FOR PLAYERS...";
            } else if (snapshot.gamePhase == com.tuempresa.proyecto.demo1.model.GamePhase.GAME_ENDED) {
                gameStatusMessage = "GAME OVER";
            }

            if (!gameStatusMessage.isEmpty()) {
                g2d.setColor(GameConfig.COLOR_TEXTO);
                g2d.setFont(GameConfig.FUENTE_GRANDE_TEXTO);
                // Centrar el texto
                java.awt.FontMetrics fm = g2d.getFontMetrics();
                int stringWidth = fm.stringWidth(gameStatusMessage);
                g2d.drawString(gameStatusMessage, (getWidth() - stringWidth) / 2, getHeight() / 2);
            }
        }

        if (GameConfig.ENABLE_PERFORMANCE_METRICS) {
            long endTime = System.nanoTime();
            long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            Logger.info(String.format("[METRIC] Client frame render time: %d ms", durationMs));
        }
    }
}