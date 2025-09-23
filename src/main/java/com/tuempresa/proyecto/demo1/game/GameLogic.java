package com.tuempresa.proyecto.demo1.game;

import com.tuempresa.proyecto.demo1.model.Coordenada;
import com.tuempresa.proyecto.demo1.model.Direccion;
import com.tuempresa.proyecto.demo1.model.Fruta;
import com.tuempresa.proyecto.demo1.model.GameState;
import com.tuempresa.proyecto.demo1.model.Snake;
import com.tuempresa.proyecto.demo1.model.CausaMuerte;
import com.tuempresa.proyecto.demo1.util.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameLogic {

    public void actualizar(GameState estado, ConcurrentHashMap<String, Direccion> accionesDeJugadores) {
        Map<String, Direccion> accionesDeEsteTick = new HashMap<>(accionesDeJugadores);

        // 1. Determinar los próximos movimientos de cada serpiente
        Map<Snake, Coordenada> futurosMovimientos = new HashMap<>();
        for (Snake s : estado.getSerpientes()) {
            Direccion dir = accionesDeEsteTick.getOrDefault(s.getIdJugador(), Direccion.NINGUNA);
            Coordenada cabezaActual = s.getHead();
            Coordenada nuevaCabeza;
            switch (dir) {
                case ARRIBA:    nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y - 1); break;
                case ABAJO:     nuevaCabeza = new Coordenada(cabezaActual.x, cabezaActual.y + 1); break;
                case IZQUIERDA: nuevaCabeza = new Coordenada(cabezaActual.x - 1, cabezaActual.y); break;
                case DERECHA:   nuevaCabeza = new Coordenada(cabezaActual.x + 1, cabezaActual.y); break;
                default:        continue; // No se mueve
            }
            futurosMovimientos.put(s, nuevaCabeza);
        }

        // 2. Detectar todas las colisiones
        Map<Snake, CausaMuerte> serpientesAeliminar = new HashMap<>();

        // 2a. Colisiones con paredes, cuerpo propio o cuerpo de otros
        for (Map.Entry<Snake, Coordenada> entry : futurosMovimientos.entrySet()) {
            Snake s = entry.getKey();
            Coordenada nuevaCabeza = entry.getValue();
            CausaMuerte causa = determinarCausaColision(estado, s, nuevaCabeza);
            if (causa != CausaMuerte.NINGUNA) {
                serpientesAeliminar.put(s, causa);
            }
        }

        // 2b. Colisiones de cabeza con cabeza
        Map<Coordenada, List<Snake>> cabezasEnMismaCelda = futurosMovimientos.entrySet().stream()
                .filter(entry -> !serpientesAeliminar.containsKey(entry.getKey())) // Solo considerar serpientes vivas
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        for (List<Snake> serpientesEnColision : cabezasEnMismaCelda.values()) {
            if (serpientesEnColision.size() > 1) {
                for (Snake s : serpientesEnColision) {
                    serpientesAeliminar.put(s, CausaMuerte.COLISION_CABEZA);
                }
            }
        }

        // 3. Eliminar las serpientes que colisionaron
        if (!serpientesAeliminar.isEmpty()) {
            for (Map.Entry<Snake, CausaMuerte> entry : serpientesAeliminar.entrySet()) {
                Snake s = entry.getKey();
                CausaMuerte causa = entry.getValue();
                estado.getSerpientes().remove(s);
                Logger.info(String.format("Jugador %s eliminado: %s.", s.getIdJugador(), getMensajeMuerte(causa)));
            }
        }

        // 4. Mover las serpientes que sobrevivieron y gestionar frutas
        for (Snake s : estado.getSerpientes()) {
            Coordenada nuevaCabeza = futurosMovimientos.get(s);
            if (nuevaCabeza == null) continue; // No tenía movimiento

            s.getCuerpo().addFirst(nuevaCabeza);
            s.addOcupada(nuevaCabeza);

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
                    break;
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

        // 5. Gestionar aparición de frutas
        gestionarAparicionDeFrutas(estado);

        // 6. Actualizar el tablero para la vista
        actualizarTablero(estado);
    }

    private String getMensajeMuerte(CausaMuerte causa) {
        switch (causa) {
            case COLISION_PARED:
                return "chocó contra la pared";
            case COLISION_CUERPO:
                return "se ha mordido a sí mismo";
            case COLISION_OTRO_JUGADOR:
                return "chocó contra otro jugador";
            case COLISION_CABEZA:
                return "colisión frontal con otro jugador";
            default:
                return "causa desconocida";
        }
    }

    private void gestionarAparicionDeFrutas(GameState estado) {
        if (estado.getSerpientes().isEmpty() && !estado.getFrutas().isEmpty()) {
            estado.getFrutas().clear();
            return;
        }
        if (estado.getSerpientes().isEmpty()) {
            return;
        }

        int numJugadores = estado.getSerpientes().size();
        int divisor = Math.max(1, GameConfig.FRUTAS_POR_JUGADOR_DIVISOR);
        int numFrutasDeseado = GameConfig.FRUTAS_MINIMAS + (numJugadores / divisor);

        while (estado.getFrutas().size() < numFrutasDeseado) {
            generarFruta(estado);
        }
    }

    private CausaMuerte determinarCausaColision(GameState estado, Snake serpiente, Coordenada nuevaCabeza) {
        int alto = estado.getTablero().length;
        int ancho = estado.getTablero()[0].length;

        if (nuevaCabeza.getX() < 0 || nuevaCabeza.getX() >= ancho || nuevaCabeza.getY() < 0 || nuevaCabeza.getY() >= alto) {
            return CausaMuerte.COLISION_PARED;
        }

        // Optimización: Usar el método ocupa() para una comprobación O(1) en lugar de un bucle O(L).
        if (serpiente.ocupa(nuevaCabeza)) {
            // Excepción: si la nueva cabeza está en la posición de la cola actual Y la serpiente no va a crecer,
            // no es una colisión, porque la cola se moverá.
            Coordenada cola = serpiente.getCuerpo().getLast();
            if (nuevaCabeza.equals(cola) && serpiente.getSegmentosPorCrecer() == 0 && serpiente.getCuerpo().size() > 1) {
                // No es una colisión, es un movimiento válido para "perseguir la cola".
            } else {
                return CausaMuerte.COLISION_CUERPO;
            }
        }

        for (Snake otra : estado.getSerpientes()) {
            if (serpiente == otra) continue;

            if (otra.ocupa(nuevaCabeza)) {
                Coordenada colaOtra = otra.getCuerpo().getLast();
                if (nuevaCabeza.equals(colaOtra) && otra.getSegmentosPorCrecer() == 0 && otra.getCuerpo().size() > 1) {
                    continue;
                }
                return CausaMuerte.COLISION_OTRO_JUGADOR;
            }
        }
        return CausaMuerte.NINGUNA;
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

    public void generarFruta(GameState estado) {
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

        if (tipoFruta < GameConfig.PROBABILIDAD_FRUTA_NORMAL) {
            nuevaFruta = new Fruta(nuevaPosicion, 1, Color.RED.getRGB());
        } else if (tipoFruta < GameConfig.PROBABILIDAD_FRUTA_NORMAL + GameConfig.PROBABILIDAD_FRUTA_BUENA) {
            nuevaFruta = new Fruta(nuevaPosicion, 2, Color.BLUE.getRGB());
        } else {
            nuevaFruta = new Fruta(nuevaPosicion, 3, Color.MAGENTA.getRGB());
        }

        estado.getFrutas().add(nuevaFruta);
        Logger.debug(String.format("Nueva fruta generada en (%d, %d) con valor %d.", nuevaPosicion.x, nuevaPosicion.y, nuevaFruta.getValor()));
    }
}
