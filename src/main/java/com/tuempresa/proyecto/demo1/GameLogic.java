package com.tuempresa.proyecto.demo1;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameLogic {

    private Random random = new Random();

    public void actualizar(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
        if (!estado.isJuegoActivo()) {
            return;
        }

        moverSerpientes(estado, accionesDeJugadores);
        comprobarColisiones(estado);

        if (estado.getFrutas().isEmpty()) {
            generarFruta(estado);
        }

        actualizarTablero(estado);
    }

    private void moverSerpientes(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
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
                default: break;
            }

            serpiente.getCuerpo().addFirst(nuevaCabeza);
            serpiente.addOcupada(nuevaCabeza);

            if (serpiente.getSegmentosPorCrecer() > 0) {
                serpiente.setSegmentosPorCrecer(serpiente.getSegmentosPorCrecer() - 1);
            } else {
                Coordenada cola = serpiente.getCuerpo().removeLast();
                serpiente.removeOcupada(cola);
            }
        }
    }

    private void comprobarColisiones(GameState estado) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;

        java.util.List<Snake> serpientesAeliminar = new java.util.ArrayList<>();

        for (Snake s1 : estado.getSerpientes()) {
            Coordenada head = s1.getHead();

            // Boundary check
            if (head.getX() < 0 || head.getX() >= ancho || head.getY() < 0 || head.getY() >= alto) {
                serpientesAeliminar.add(s1);
                continue;
            }

            // Check fruit collision
            java.util.Iterator<Fruta> fit = estado.getFrutas().iterator();
            while (fit.hasNext()) {
                Fruta f = fit.next();
                if (f.getCoordenada().equals(head)) {
                    s1.setPuntaje(s1.getPuntaje() + f.getValor());
                    s1.setSegmentosPorCrecer(s1.getSegmentosPorCrecer() + f.getValor());
                    fit.remove();
                    // No 'break' here, a snake could be on multiple fruits in the same tick (unlikely but possible)
                }
            }

            // Check for collisions with other snakes and self
            for (Snake s2 : estado.getSerpientes()) {
                if (s1 == s2) { // Self-collision check
                    // Check if head collides with any part of its body except the head
                    for (int i = 1; i < s1.getCuerpo().size(); i++) {
                        if (s1.getCuerpo().get(i).equals(head)) {
                            serpientesAeliminar.add(s1);
                            break;
                        }
                    }
                } else { // Collision with another snake
                    if (s2.ocupa(head)) {
                        serpientesAeliminar.add(s1);
                        break;
                    }
                }
            }
        }

        estado.getSerpientes().removeAll(serpientesAeliminar);
    }

    private void actualizarTablero(GameState estado) {
        byte[][] tablero = estado.getTablero();
        for(int i=0; i<tablero.length; i++) {
            for(int j=0; j<tablero[i].length; j++) {
                tablero[i][j] = Constants.VACIO;
            }
        }

        for (Fruta fruta : estado.getFrutas()) {
            Coordenada pos = fruta.getCoordenada();
            tablero[pos.y][pos.x] = (byte)(Constants.FRUTA_BASE + fruta.getValor() - 1);
        }

        for (Snake serpiente : estado.getSerpientes()) {
            Coordenada cabeza = serpiente.getHead();
            tablero[cabeza.y][cabeza.x] = Constants.SNAKE_HEAD;
            for (int i = 1; i < serpiente.getCuerpo().size(); i++) {
                Coordenada parteCuerpo = serpiente.getCuerpo().get(i);
                tablero[parteCuerpo.y][parteCuerpo.x] = Constants.SNAKE_BODY;
            }
        }
    }

    public static void generarFruta(GameState estado) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;
        Random random = new Random();

        Coordenada nuevaPosicion;
        while (true) {
            int x = random.nextInt(ancho);
            int y = random.nextInt(alto);
            boolean esPosicionValida = true;

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
}
