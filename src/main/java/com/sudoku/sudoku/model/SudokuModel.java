package com.sudoku.sudoku.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Modelo del juego Sudoku 6×6 — HU-3.
 *
 * <p>Almacena el estado del tablero como una matriz {@code int[][]} donde
 * {@code 0} representa celda vacía y {@code 1–6} un número colocado.
 * Expone {@link #generarTablero()} para distribuir exactamente
 * {@value #PISTAS_POR_BLOQUE} pistas aleatorias válidas en cada uno de los
 * {@value #TOTAL_BLOQUES} bloques de 2×3, sin ningún import de JavaFX.</p>
 *
 * <p>Desde HU-3 el modelo distingue entre celdas fijas (pistas) y celdas
 * libres (editables por el jugador). {@link #setValor(int, int, int)} almacena
 * la entrada del usuario; {@link #resetearJugador()} devuelve el tablero al
 * estado inicial sin regenerar las pistas.</p>
 *
 * <p>La validez se garantiza revisando fila, columna y bloque antes de
 * aceptar cada número. Si un intento parcial queda sin candidatos para alguna
 * celda, el proceso se descarta y reinicia hasta obtener un tablero limpio.</p>
 *
 * @author Jhon Acosta
 * @version 3.0
 */
public class SudokuModel {

    /** Dimensión del tablero (6 filas × 6 columnas). */
    public static final int TAMAÑO = 6;

    /** Pistas que se colocan en cada bloque de 2×3 al iniciar la partida. */
    private static final int PISTAS_POR_BLOQUE = 2;

    /** Total de bloques: 3 grupos de filas × 2 grupos de columnas. */
    private static final int TOTAL_BLOQUES = 6;

    /** Estado del tablero: 0 = vacío, 1–6 = número colocado. */
    private final int[][] tablero = new int[TAMAÑO][TAMAÑO];

    /** Marca las celdas que son pistas fijas (no deben ser editables por el jugador). */
    private final boolean[][] fija = new boolean[TAMAÑO][TAMAÑO];

    private final Random rng = new Random();

    // ── Lectura del estado ────────────────────────────────────────────────────

    /**
     * Devuelve el valor almacenado en la celda indicada.
     *
     * @param fila fila del tablero (0–5)
     * @param col  columna del tablero (0–5)
     * @return valor entre 0 y 6; 0 significa celda vacía
     */
    public int getValor(int fila, int col) {
        return tablero[fila][col];
    }

    /**
     * Indica si la celda fue asignada como pista inicial (no editable).
     *
     * @param fila fila del tablero (0–5)
     * @param col  columna del tablero (0–5)
     * @return {@code true} si la celda es una pista fija
     */
    public boolean esFija(int fila, int col) {
        return fija[fila][col];
    }

    // ── Escritura del estado (jugador) ────────────────────────────────────────

    /**
     * Almacena un número ingresado por el jugador en la celda indicada.
     * La invocación se ignora silenciosamente si la celda es una pista fija,
     * garantizando que las pistas iniciales nunca sean sobreescritas.
     *
     * @param fila  fila del tablero (0–5)
     * @param col   columna del tablero (0–5)
     * @param valor número a guardar (1–6) o {@code 0} para limpiar la celda
     */
    public void setValor(int fila, int col, int valor) {
        if (!fija[fila][col]) {
            tablero[fila][col] = valor;
        }
    }

    /**
     * Restablece a {@code 0} todas las celdas no fijas, devolviendo el tablero
     * al estado inicial de la partida actual sin regenerar ni mover las pistas.
     */
    public void resetearJugador() {
        for (int f = 0; f < TAMAÑO; f++) {
            for (int c = 0; c < TAMAÑO; c++) {
                if (!fija[f][c]) {
                    tablero[f][c] = 0;
                }
            }
        }
    }

    // ── Generación del tablero ────────────────────────────────────────────────

    /**
     * Limpia el tablero y distribuye {@value #PISTAS_POR_BLOQUE} pistas
     * aleatorias y válidas en cada uno de los {@value #TOTAL_BLOQUES} bloques.
     *
     * <p>El algoritmo reintenta desde cero si en algún bloque no quedan
     * candidatos libres. En la práctica converge en pocos intentos porque
     * solo se colocan 12 números sobre 36 celdas.</p>
     */
    public void generarTablero() {
        boolean exito;
        do {
            limpiar();
            exito = colocarPistasEnTodosLosBloques();
        } while (!exito);
    }

    /** Pone a cero todos los valores y las marcas de celda fija. */
    private void limpiar() {
        for (int f = 0; f < TAMAÑO; f++) {
            for (int c = 0; c < TAMAÑO; c++) {
                tablero[f][c] = 0;
                fija[f][c]    = false;
            }
        }
    }

    /**
     * Itera sobre los {@value #TOTAL_BLOQUES} bloques e intenta colocar
     * las pistas en cada uno.
     *
     * @return {@code true} si todos los bloques recibieron sus pistas
     */
    private boolean colocarPistasEnTodosLosBloques() {
        for (int b = 0; b < TOTAL_BLOQUES; b++) {
            BloqueCoord bloque = new BloqueCoord(b);
            if (!colocarPistasEnBloque(bloque)) return false;
        }
        return true;
    }

    /**
     * Elige celdas al azar del bloque y les asigna un número válido hasta
     * completar las {@value #PISTAS_POR_BLOQUE} pistas requeridas.
     *
     * @param bloque adaptador con las coordenadas del bloque objetivo
     * @return {@code true} si se colocaron las dos pistas sin conflicto
     */
    private boolean colocarPistasEnBloque(BloqueCoord bloque) {
        // Las 6 celdas del bloque en orden aleatorio evitan sesgos de posición
        List<int[]> celdas = bloque.celdasBarajadas(rng);
        int colocados = 0;

        for (int[] celda : celdas) {
            if (colocados == PISTAS_POR_BLOQUE) break;

            int f = celda[0];
            int c = celda[1];

            List<Integer> candidatos = numerosDisponibles(f, c);
            if (candidatos.isEmpty()) return false; // sin opciones válidas: reintentar todo

            int num = candidatos.get(rng.nextInt(candidatos.size()));
            tablero[f][c] = num;
            fija[f][c]    = true;
            colocados++;
        }

        return colocados == PISTAS_POR_BLOQUE;
    }

    /**
     * Calcula qué números del 1 al 6 pueden colocarse en {@code (fila, col)}
     * sin violar las reglas de fila, columna ni bloque 2×3.
     *
     * @param fila fila objetivo
     * @param col  columna objetivo
     * @return lista barajada de candidatos válidos (puede estar vacía)
     */
    private List<Integer> numerosDisponibles(int fila, int col) {
        // usados[n] = true si n ya aparece en la fila, columna o bloque
        boolean[] usados = new boolean[TAMAÑO + 1];

        for (int c = 0; c < TAMAÑO; c++) usados[tablero[fila][c]] = true; // fila
        for (int f = 0; f < TAMAÑO; f++) usados[tablero[f][col]]  = true; // columna

        // Esquina superior-izquierda del bloque 2×3 al que pertenece la celda
        int filaBloque = (fila / 2) * 2;
        int colBloque  = (col  / 3) * 3;
        for (int df = 0; df < 2; df++) {
            for (int dc = 0; dc < 3; dc++) {
                usados[tablero[filaBloque + df][colBloque + dc]] = true;
            }
        }
        usados[0] = true; // el 0 no es un candidato válido

        List<Integer> disponibles = new ArrayList<>();
        for (int n = 1; n <= TAMAÑO; n++) {
            if (!usados[n]) disponibles.add(n);
        }
        Collections.shuffle(disponibles, rng);
        return disponibles;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Clase interna adaptadora: BloqueCoord
    // Traduce un índice de bloque (0–5) a las coordenadas reales del tablero.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Adaptadora de coordenadas que encapsula la correspondencia entre el
     * índice lógico de un bloque 2×3 (0–5) y su posición física en la matriz.
     */
    private static final class BloqueCoord {

        /** Fila de la esquina superior-izquierda del bloque. */
        final int filaInicio;

        /** Columna de la esquina superior-izquierda del bloque. */
        final int colInicio;

        /**
         * Construye el adaptador a partir del índice de bloque.
         *
         * @param indice índice del bloque (0–5)
         */
        BloqueCoord(int indice) {
            // Hay 2 columnas de bloques → % 2 da el grupo de columnas (0 ó 1)
            // Hay 3 filas de bloques    → / 2 da el grupo de filas (0, 1 ó 2)
            this.filaInicio = (indice / 2) * 2;
            this.colInicio  = (indice % 2) * 3;
        }

        /**
         * Devuelve las 6 celdas del bloque en orden aleatorio para
         * evitar sesgos al seleccionar dónde colocar las pistas.
         *
         * @param rng generador aleatorio compartido
         * @return lista de pares {@code [fila, col]}
         */
        List<int[]> celdasBarajadas(Random rng) {
            List<int[]> lista = new ArrayList<>(6);
            for (int df = 0; df < 2; df++) {
                for (int dc = 0; dc < 3; dc++) {
                    lista.add(new int[]{ filaInicio + df, colInicio + dc });
                }
            }
            Collections.shuffle(lista, rng);
            return lista;
        }
    }
}
