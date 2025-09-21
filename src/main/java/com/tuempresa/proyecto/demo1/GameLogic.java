package com.tuempresa.proyecto.demo1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameLogic {

    public void actualizar(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
        if (!estado.isJuegoActivo()) {
            return;
        }

        long startTime = System.nanoTime();
        // 1. Determinar los próximos movimientos y comprobar colisiones
        List<Snake> serpientesAeliminar = new ArrayList<>();
        Set<Coordenada> futurasCabezas = new HashSet<>();
        for (Snake s : estado.getSerpientes()) {
            Coordenada cabezaActual = s.getHead();
            Direccion dir = accionesDeJugadores.getOrDefault(s.getIdJugador(), Direccion.NINGUNA);
            Coordenada nuevaCabeza;

            switch (dir) {
                case ARRIBA:    nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y - 1); break;
                case ABAJO:     nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y + 1); break;
                case IZQUIERDA: nuevaCabeza = new Coordenada(cabezaActual.x - 1, cabezaActual.y); break;
                case DERECHA:   nuevaCabeza = new Coordenada(cabezaActual.x + 1, cabezaActual.y); break;
                default: continue; // Si no hay dirección, no hay nada que hacer
            }

            if (esColision(estado, s, nuevaCabeza) || !futurasCabezas.add(nuevaCabeza)) {
                serpientesAeliminar.add(s);
            }
        }

        // Eliminar las serpientes que colisionaron
        for (Snake s : serpientesAeliminar) {
            estado.getSerpientes().remove(s);
            Logger.info(String.format("Jugador %s eliminado.", s.getIdJugador()));
        }

        // 2. Mover las serpientes que sobrevivieron
        for (Snake s : estado.getSerpientes()) {
             Direccion dir = accionesDeJugadores.getOrDefault(s.getIdJugador(), Direccion.NINGUNA);
             Coordenada cabezaActual = s.getHead();
             Coordenada nuevaCabeza;
             switch (dir) {
                case ARRIBA:    nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y - 1); break;
                case ABAJO:     nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y + 1); break;
                case IZQUIERDA: nuevaCabeza = new Coordenada(cabezaActual.x - 1, cabezaActual.y); break;
                case DERECHA:   nuevaCabeza = new Coordenada(cabezaActual.x + 1, cabezaActual.y); break;
                default: continue;
            }

            s.getCuerpo().addFirst(nuevaCabeza);
            s.addOcupada(nuevaCabeza);

            // Gestionar crecimiento y colisión con fruta
            boolean comioFruta = false;
            java.util.Iterator<Fruta> fit = estado.getFrutas().iterator();
            while (fit.hasNext()) {
                Fruta f = fit.next();
                if (f.getCoordenada().equals(nuevaCabeza)) {
                    s.setPuntaje(s.getPuntaje() + f.getValor());
                    s.setSegmentosPorCrecer(s.getSegmentosPorCrecer() + f.getValor());
                    fit.remove();
                    comioFruta = true;
                    Logger.debug(String.format("Jugador %s comió una fruta de valor %d.", s.getIdJugador(), f.getValor()));
                    break; // Solo se puede comer una fruta por tick
                }
            }

            if (!comioFruta) {
                if (s.getSegmentosPorCrecer() > 0) {
                    s.setSegmentosPorCrecer(s.getSegmentosPorCrecer() - 1);
                } else {
                    Coordenada cola = s.getCuerpo().removeLast();
                    s.removeOcupada(cola);
                }
            }
        }

        long endTime = System.nanoTime();
        Logger.debug(String.format("Lógica de juego tomó %.3f ms", (endTime - startTime) / 1_000_000.0));

        // 3. Generar nueva fruta si es necesario
        if (estado.getFrutas().isEmpty()) {
            generarFruta(estado);
        }

        // 4. Actualizar el tablero para la vista
        actualizarTablero(estado);
    }

    private boolean esColision(GameState estado, Snake serpiente, Coordenada nuevaCabeza) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;

        // Colisión con el borde
        if (nuevaCabeza.getX() < 0 || nuevaCabeza.getX() >= ancho || nuevaCabeza.getY() < 0 || nuevaCabeza.getY() >= alto) {
            return true;
        }

        // Colisión consigo misma
        for (int i = 0; i < serpiente.getCuerpo().size(); i++) {
            if (nuevaCabeza.equals(serpiente.getCuerpo().get(i))) {
                return true;
            }
        }

        // Colisión con otras serpientes
        for (Snake otra : estado.getSerpientes()) {
            if (serpiente == otra) continue;
            if (otra.ocupa(nuevaCabeza)) {
                return true;
            }
        }
        return false;
    }

    private void actualizarTablero(GameState estado) {
        byte[][] tablero = estado.getTablero();
        for(int i=0; i<tablero.length; i++) {
            for(int j=0; j<tablero[i].length; j++) {
                tablero[i][j] = GameConfig.VACIO;
            }
        }

        for (Fruta fruta : estado.getFrutas()) {
            Coordenada pos = fruta.getCoordenada();
            tablero[pos.y][pos.x] = (byte)(GameConfig.FRUTA_BASE + fruta.getValor() - 1);
        }

        for (Snake serpiente : estado.getSerpientes()) {
            Coordenada cabeza = serpiente.getHead();
            tablero[cabeza.y][cabeza.x] = GameConfig.SNAKE_HEAD;
            for (int i = 1; i < serpiente.getCuerpo().size(); i++) {
                Coordenada parteCuerpo = serpiente.getCuerpo().get(i);
                tablero[parteCuerpo.y][parteCuerpo.x] = GameConfig.SNAKE_BODY;
            }
        }
    }

    public static void generarFruta(GameState estado) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;
        Random random = new Random();

        Set<Coordenada> celdasOcupadas = new HashSet<>();
        for (Snake s : estado.getSerpientes()) {
            celdasOcupadas.addAll(s.getCuerpo());
        }
        for (Fruta f : estado.getFrutas()) {
            celdasOcupadas.add(f.getCoordenada());
        }

        List<Coordenada> celdasVacias = new ArrayList<>();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                Coordenada c = new Coordenada(x, y);
                if (!celdasOcupadas.contains(c)) {
                    celdasVacias.add(c);
                }
            }
        }

        if (celdasVacias.isEmpty()) {
            Logger.warn("No hay espacio para generar una nueva fruta.");
            return;
        }

        Coordenada nuevaPosicion = celdasVacias.get(random.nextInt(celdasVacias.size()));

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
        Logger.debug(String.format("Nueva fruta generada en (%d, %d) con valor %d.", nuevaPosicion.x, nuevaPosicion.y, nuevaFruta.getValor()));
    }
}
