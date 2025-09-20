// GraphicalView.java
package com.tuempresa.proyecto.demo1;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.atomic.AtomicReference;

public class GraphicalView {

    private JFrame frame;
    private GamePanel gamePanel;
    private AtomicReference<Direccion> direccionActual;

    public GraphicalView(GameStateSnapshot inicial, AtomicReference<Direccion> direccionActual) {
        this.direccionActual = direccionActual;
        this.gamePanel = new GamePanel(inicial);

        frame = new JFrame("Snake Multijugador");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(gamePanel);
        frame.pack(); // Ajusta el tamaño de la ventana al del panel
        frame.setLocationRelativeTo(null); // Centra la ventana
        frame.addKeyListener(createKeyListener());
        frame.setFocusable(true);
        frame.setVisible(true);
    }

    public void repaint() {
        gamePanel.repaint();
    }

    // Called from EDT with a DTO snapshot to safely update what the panel renders
    public void actualizarEstado(GameStateSnapshot snapshot) {
        this.gamePanel.setEstado(snapshot);
    }

    public JFrame getFrame() { return frame; }

    private KeyListener createKeyListener() {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                Direccion ultimaDireccion = direccionActual.get();
                Direccion nuevaDireccion = ultimaDireccion;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        if (ultimaDireccion != Direccion.ABAJO) nuevaDireccion = Direccion.ARRIBA;
                        break;
                    case KeyEvent.VK_S:
                        if (ultimaDireccion != Direccion.ARRIBA) nuevaDireccion = Direccion.ABAJO;
                        break;
                    case KeyEvent.VK_A:
                        if (ultimaDireccion != Direccion.DERECHA) nuevaDireccion = Direccion.IZQUIERDA;
                        break;
                    case KeyEvent.VK_D:
                        if (ultimaDireccion != Direccion.IZQUIERDA) nuevaDireccion = Direccion.DERECHA;
                        break;
                }
                direccionActual.set(nuevaDireccion);
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        };
    }
}