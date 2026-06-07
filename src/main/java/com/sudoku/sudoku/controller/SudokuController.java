package com.sudoku.sudoku.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Controlador base de la vista Sudoku 6×6 — HU-1.
 *
 * <p>Para esta historia solo construye la cuadrícula vacía con sus bordes
 * visuales y conecta los botones con stubs de consola. Toda la lógica del
 * juego (puzzles, validación, estado de celdas) va en historias posteriores.</p>
 *
 * @author Jhon Acosta
 * @version 1.0
 */
public class SudokuController {

    /* ── Componentes declarados en el FXML ── */
    @FXML private GridPane sudokuGrid;
    @FXML private Button   btnStart;
    @FXML private Button   btnValidate;
    @FXML private Button   btnReset;

    /** Dimensión del tablero 6×6. */
    private static final int SIZE = 6;

    /**
     * JavaFX invoca este método al terminar de cargar el FXML.
     * Aquí creamos las 36 celdas vacías porque el grosor de borde de cada
     * una depende de su posición (no es posible calcularlo solo en CSS).
     */
    @FXML
    private void initialize() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                TextField cell = new TextField();
                cell.getStyleClass().add("cell");
                applyBlockBorders(cell, row, col);
                sudokuGrid.add(cell, col, row);
            }
        }
    }

    /**
     * Asigna el borde correcto a cada celda según su posición en el tablero.
     *
     * <p>Estrategia de borde único por línea: cada celda dibuja solo su borde
     * derecho e inferior (más el marco superior/izquierdo en fila 0 y columna 0).
     * Así cada línea la pinta una sola celda y no hay acumulación de bordes.</p>
     *
     * <ul>
     *   <li>3 px azul cobalto → límite de bloque 2×3 o marco exterior</li>
     *   <li>1 px gris claro   → línea interna de la cuadrícula</li>
     *   <li>0 px transparente → ese lado ya lo dibuja la celda vecina</li>
     * </ul>
     *
     * @param cell celda a la que aplicar el estilo de borde
     * @param row  fila de la celda (0–5)
     * @param col  columna de la celda (0–5)
     */
    private void applyBlockBorders(TextField cell, int row, int col) {
        // Marco superior: solo la primera fila lo necesita
        double top  = (row == 0) ? 3 : 0;
        // Marco izquierdo: solo la primera columna
        double left = (col == 0) ? 3 : 0;

        // Borde grueso al final de cada bloque horizontal (filas 1, 3) y en el cierre (fila 5)
        double bottom = (row == 1 || row == 3 || row == 5) ? 3 : 1;
        // Borde grueso al final de cada bloque vertical (col 2) y en el cierre (col 5)
        double right  = (col == 2 || col == 5) ? 3 : 1;

        // JavaFX CSS: -fx-border-width sigue el orden  top | right | bottom | left
        cell.setStyle(String.format(
            "-fx-border-width: %.0f %.0f %.0f %.0f; -fx-border-color: %s %s %s %s;",
            top, right, bottom, left,
            borderColor(top),    // color borde superior
            borderColor(right),  // color borde derecho
            borderColor(bottom), // color borde inferior
            borderColor(left)    // color borde izquierdo
        ));
    }

    /**
     * Devuelve el color de borde según su peso:
     * grueso → azul, fino → gris, ausente → transparente.
     */
    private String borderColor(double width) {
        if (width == 0)  return "transparent";
        if (width >= 3)  return "#1a3a6b";   // azul para delimitar bloques
        return "#c0c8d4";                     // gris claro para líneas interiores
    }

    // Botones

    /** @see SudokuController — lógica en HU-2 */
    @FXML
    private void onIniciarJuego() {
        System.out.println("Botón Iniciar Juego presionado");
    }

    /** @see SudokuController — lógica  en HU-3 */
    @FXML
    private void onValidar() {
        System.out.println("Botón Validar presionado");
    }

    /** @see SudokuController — lógica en HU-3 */
    @FXML
    private void onReiniciar() {
        System.out.println("Botón Reiniciar presionado");
    }
}
