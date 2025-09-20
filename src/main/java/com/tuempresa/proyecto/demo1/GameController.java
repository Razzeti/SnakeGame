// GameController.java (VERSIÓN COMPLETA Y CORREGIDA)
package com.tuempresa.proyecto.demo1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameController {

    private static Random random = new Random();

    /**
     * El método principal del controlador, llamado en cada ciclo del Game Loop.
     * Orquesta el movimiento, las colisiones y la generación de frutas.
     */
    public static void actualizar(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
        if (!estado.isJuegoActivo()) {
            return;
        }

        // --- ESTOS SON LOS COMANDOS QUE FALTABAN ---
        moverSerpientes(estado, accionesDeJugadores);
        comprobarColisiones(estado);

        if (estado.getFrutas().isEmpty()) {
            generarFruta(estado);
        }

        // La actualización del tablero de bytes no es estrictamente necesaria para la GUI,
        // pero es buena práctica mantenerlo sincronizado.
        actualizarTablero(estado);
    }

    /**
     * Mueve cada serpiente y maneja la lógica de crecimiento.
     */
    private static void moverSerpientes(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
        for (Snake serpiente : estado.getSerpientes()) {
            Direccion dir = accionesDeJugadores.getOrDefault(serpiente.getIdJugador(), Direccion.NINGUNA);

            if (dir == Direccion.NINGUNA) continue;

            Coordenada cabezaActual = serpiente.getHead();
            Coordenada nuevaCabeza = new Coordenada(cabezaActual.getX(), cabezaActual.getY());

            switch (dir) {
                case ARRIBA:    nuevaCabeza.y--; break;
                case ABAJO:     nuevaCabeza.y++; break;
                case IZQUIERDA: nuevaCabeza.x--; break;
                case DERECHA:   nuevaCabeza.x++; break;
                default: break; // covers NINGUNA and any future values
            }

            serpiente.getCuerpo().addFirst(nuevaCabeza);

            if (serpiente.getSegmentosPorCrecer() > 0) {
                serpiente.setSegmentosPorCrecer(serpiente.getSegmentosPorCrecer() - 1);
            } else {
                serpiente.getCuerpo().removeLast();
            }
        }
    }

    /**
     * Revisa todas las posibles colisiones y actualiza el estado.
     */
     private static void comprobarColisiones(GameState estado) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;

        java.util.Iterator<Snake> iter = estado.getSerpientes().iterator();
        while (iter.hasNext()) {
            Snake s = iter.next();
            Coordenada head = s.getHead();

            // Boundary check
            if (head.getX() < 0 || head.getX() >= ancho || head.getY() < 0 || head.getY() >= alto) {
                iter.remove();
                continue;
            }

            // Check fruit collision (eat fruit)
            java.util.Iterator<Fruta> fit = estado.getFrutas().iterator();
            while (fit.hasNext()) {
                Fruta f = fit.next();
                Coordenada fp = f.getCoordenada();
                if (fp.getX() == head.getX() && fp.getY() == head.getY()) {
                    // Eat the fruit
                    s.setPuntaje(s.getPuntaje() + f.getValor());
                    s.setSegmentosPorCrecer(s.getSegmentosPorCrecer() + f.getValor());
                    fit.remove();
                    break;
                }
            }

            // Check collisions with any snake body (including self)
            boolean collided = false;
            for (Snake other : estado.getSerpientes()) {
                java.util.List<Coordenada> cuerpo = other.getCuerpo();
                for (int i = 0; i < cuerpo.size(); i++) {
                    Coordenada c = cuerpo.get(i);
                    // skip comparing head with itself at index 0
                    if (other == s && i == 0) continue;
                    if (c.getX() == head.getX() && c.getY() == head.getY()) {
                        collided = true;
                        break;
                    }
                }
                if (collided) break;
            }

            if (collided) {
                iter.remove();
            }
        }
    }

    /**
     * Añade una nueva fruta en una posición aleatoria y vacía del tablero.
     */
    public static void generarFruta(GameState estado) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;

        Coordenada nuevaPosicion;
        // Bucle para asegurar que la fruta no aparezca encima de una serpiente
        while (true) {
            int x = random.nextInt(ancho);
            int y = random.nextInt(alto);
            boolean esPosicionValida = true;

            // Comprobar que no hay ninguna parte de ninguna serpiente en la nueva posición
            for (Snake s : estado.getSerpientes()) {
                for (Coordenada c : s.getCuerpo()) {
                    if (c.x == x && c.y == y) {
                        esPosicionValida = false;
                        break;
                    }
                }
                if (!esPosicionValida) break;
            }

            if (esPosicionValida) {
                nuevaPosicion = new Coordenada(x, y);
                break;
            }
        }

        int tipoFruta = random.nextInt(100);
        Fruta nuevaFruta;

        if (tipoFruta < 70) {
            nuevaFruta = new Fruta(nuevaPosicion, 1, Color.RED.getRGB());
        } else if (tipoFruta < 90) {
            nuevaFruta = new Fruta(nuevaPosicion, 2, Color.BLUE.getRGB());
        } else {
            nuevaFruta = new Fruta(nuevaPosicion, 3, Color.MAGENTA.getRGB());
        }

        estado.getFrutas().add(nuevaFruta);
    }

    /**
     * Limpia y redibuja el tablero de bytes.
     */
    private static void actualizarTablero(GameState estado) {
        byte[][] tablero = estado.getTablero();
        for(int i=0; i<tablero.length; i++) {
            for(int j=0; j<tablero[i].length; j++) {
                // ANTES: tablero[i][j] = 0;
                tablero[i][j] = Constants.VACIO;
            }
        }

        for (Fruta fruta : estado.getFrutas()) {
            Coordenada pos = fruta.getCoordenada();
            // Lógica de mapeo sugerida: FRUTA_BASE + valor - 1
            // ANTES: tablero[pos.y][pos.x] = (byte)(fruta.getValor() + 1);
            tablero[pos.y][pos.x] = (byte)(Constants.FRUTA_BASE + fruta.getValor() - 1);
        }

        for (Snake serpiente : estado.getSerpientes()) {
            Coordenada cabeza = serpiente.getHead();
            // ANTES: tablero[cabeza.y][cabeza.x] = 10;
            tablero[cabeza.y][cabeza.x] = Constants.SNAKE_HEAD;
            for (int i = 1; i < serpiente.getCuerpo().size(); i++) {
                Coordenada parteCuerpo = serpiente.getCuerpo().get(i);
                // ANTES: tablero[parteCuerpo.y][parteCuerpo.x] = 11;
                tablero[parteCuerpo.y][parteCuerpo.x] = Constants.SNAKE_BODY;
            }
        }
    }
}