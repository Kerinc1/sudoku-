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

        private static final int PISTAS_VISIBLES = 12;

        private final int[][] solucion = new int[TAMAÑO][TAMAÑO];

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

        public boolean hayConflicto(int fila, int col) {
            int val = tablero[fila][col];
            if (val == 0) return false;

            for (int c = 0; c < TAMAÑO; c++)
                if (c != col && tablero[fila][c] == val) return true;

            for (int f = 0; f < TAMAÑO; f++)
                if (f != fila && tablero[f][col] == val) return true;

            int fb = (fila / 2) * 2, cb = (col / 3) * 3;
            for (int df = 0; df < 2; df++)
                for (int dc = 0; dc < 3; dc++) {
                    int rf = fb + df, rc = cb + dc;
                    if ((rf != fila || rc != col) && tablero[rf][rc] == val) return true;
                }
            return false;
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
            if (!fija[fila][col]) tablero[fila][col] = valor;
        }

        public int getValorAyuda() {
            int cantidad = 0;

            for (int fila = 0; fila < tablero.length; fila++) {
                for (int col = 0; col < tablero[fila].length; col++) {

                    if (tablero[fila][col] != 0) {
                        cantidad++;
                    }

                }
            }

            return cantidad;
        }

        /**
         * Restablece a {@code 0} todas las celdas no fijas, devolviendo el tablero
         * al estado inicial de la partida actual sin regenerar ni mover las pistas.
         */
        public void resetearJugador() {
            for (int f = 0; f < TAMAÑO; f++)
                for (int c = 0; c < TAMAÑO; c++)
                    if (!fija[f][c]) tablero[f][c] = 0;
        }

        // ── Ayuda ────────────────────────────────────────────────────────────────

        public int[] sugerirCelda() {
            List<int[]> vacias = new ArrayList<>();
            for (int f = 0; f < TAMAÑO; f++)
                for (int c = 0; c < TAMAÑO; c++)
                    if (!fija[f][c] && tablero[f][c] == 0)
                        vacias.add(new int[]{f, c, solucion[f][c]});

            if (vacias.isEmpty()) return null;
            Collections.shuffle(vacias, rng);
            return vacias.get(0);
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
            limpiar();
            rellenarCompleto(0, 0);
            for (int f = 0; f < TAMAÑO; f++)
                System.arraycopy(tablero[f], 0, solucion[f], 0, TAMAÑO);
            ocultarCeldas();
        }

        /** Pone a cero todos los valores y las marcas de celda fija. */
        private void limpiar() {
            for (int f = 0; f < TAMAÑO; f++)
                for (int c = 0; c < TAMAÑO; c++) {
                    tablero[f][c]  = 0;
                    solucion[f][c] = 0;
                    fija[f][c]     = false;
                }
        }

        private boolean rellenarCompleto(int fila, int col) {
            if (fila == TAMAÑO) return true;
            int sigFila = (col == TAMAÑO - 1) ? fila + 1 : fila;
            int sigCol  = (col == TAMAÑO - 1) ? 0 : col + 1;

            for (int n : numerosAleatorios()) {
                if (esValido(fila, col, n)) {
                    tablero[fila][col] = n;
                    if (rellenarCompleto(sigFila, sigCol)) return true;
                    tablero[fila][col] = 0;
                }
            }
            return false;
        }

        private List<Integer> numerosAleatorios() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
            Collections.shuffle(list, rng);
            return list;
        }

        private boolean esValido(int fila, int col, int val) {
            for (int c = 0; c < TAMAÑO; c++) if (tablero[fila][c] == val) return false;
            for (int f = 0; f < TAMAÑO; f++) if (tablero[f][col]  == val) return false;
            int fb = (fila / 2) * 2, cb = (col / 3) * 3;
            for (int df = 0; df < 2; df++)
                for (int dc = 0; dc < 3; dc++)
                    if (tablero[fb + df][cb + dc] == val) return false;
            return true;
        }

        private void ocultarCeldas() {
            for (int fb = 0; fb < TAMAÑO; fb += 2) {
                for (int cb = 0; cb < TAMAÑO; cb += 3) {
                    List<int[]> celdas = new ArrayList<>(6);
                    for (int df = 0; df < 2; df++)
                        for (int dc = 0; dc < 3; dc++)
                            celdas.add(new int[]{fb + df, cb + dc});
                    Collections.shuffle(celdas, rng);

                    for (int i = 0; i < 2; i++) {          // 2 pistas visibles
                        fija[celdas.get(i)[0]][celdas.get(i)[1]] = true;
                    }
                    for (int i = 2; i < 6; i++) {           // 4 celdas ocultas
                        tablero[celdas.get(i)[0]][celdas.get(i)[1]] = 0;
                        fija[celdas.get(i)[0]][celdas.get(i)[1]]    = false;
                    }
                }
            }
        }
    }
