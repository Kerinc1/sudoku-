package com.sudoku.sudoku.controller;

import com.sudoku.sudoku.model.SudokuModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

/**
 * Controlador de la vista Sudoku 6×6 — HU-3.
 *
 * <p>Actúa como puente entre la vista (FXML + CSS) y el {@link SudokuModel}.
 * Al presionar "Iniciar Juego" delega al {@link GestorAlerta} la confirmación
 * del usuario; si esta es positiva, ordena al modelo generar las pistas y
 * luego las pinta en el tablero, marcándolas como no editables.</p>
 *
 * <p>Las celdas vacías reciben el {@link FiltroEntrada}, una clase interna
 * adaptadora que intercepta eventos de teclado y acepta únicamente dígitos
 * del 1 al 6, actualizando el modelo y la vista de forma sincrónica. Las
 * celdas con pistas bloquean completamente cualquier interacción del usuario.</p>
 *
 * <p>A partir de la versión 3.1, el sistema de ayudas es ilimitado, permitiendo
 * al jugador solicitar sugerencias sin restricción de cantidad.</p>
 *
 * @author Jhon Acosta
 * @version 3.1
 */
public class SudokuController {

    /* ── Componentes declarados en el FXML ── */
    @FXML private GridPane sudokuGrid;
    @FXML private Button   btnStart;
    @FXML private Button   btnValidate;
    @FXML private Button   btnReset;
    @FXML private Button   btnAyuda;
    @FXML private Label    lblMensaje;

    private static final int TAMAÑO = SudokuModel.TAMAÑO;

    private int[]  celdaAyuda   = null;

    /**
     * Referencias directas a cada TextField para evitar consultas costosas
     * al GridPane durante el renderizado y la gestión de eventos.
     */
    private final TextField[][] celdas = new TextField[TAMAÑO][TAMAÑO];

    /** Única instancia del modelo; el controlador solo conoce su interfaz pública. */
    private final SudokuModel modelo = new SudokuModel();

    private Timeline ocultarMensaje;

    /**
     * JavaFX invoca este método al terminar de cargar el FXML.
     * Construye las 36 celdas, aplica los bordes de bloque e inicializa
     * cada una como no editable y no enfocable (la interacción se habilita
     * celda a celda en {@link #renderizarTablero()} al generar el tablero).
     */
    @FXML
    private void initialize() {
        for (int fila = 0; fila < TAMAÑO; fila++) {
            for (int col = 0; col < TAMAÑO; col++) {
                TextField celda = new TextField();
                celda.getStyleClass().add("cell");
                celda.setEditable(false);
                celda.setFocusTraversable(false);
                applyBlockBorders(celda, fila, col);
                celdas[fila][col] = celda;
                sudokuGrid.add(celda, col, fila);
            }
        }
        lblMensaje.setVisible(false);
    }

    // ── Manejadores de botones ────────────────────────────────────────────────

    /**
     * Muestra un diálogo de confirmación antes de iniciar una nueva partida.
     * Si el usuario acepta, genera el tablero y lo renderiza; si cancela,
     * el estado actual no se modifica.
     */
    @FXML
    private void onIniciarJuego() {
        GestorAlerta gestor = new GestorAlerta(
            "Sudoku 6×6",
            "¿Iniciar nueva partida?",
            "Se generará un tablero con pistas aleatorias.\n" +
                    "¿Desea continuar?"
        );
        if (!gestor.confirmar()) return;

        celdaAyuda   = null;
        modelo.generarTablero();
        renderizarTablero();

        // Los botones de acción solo tienen sentido con una partida activa
        btnValidate.setDisable(false);
        btnReset.setDisable(false);
        btnAyuda.setDisable(false);
        actualizarTextoAyuda();
        ocultarMensajeBanner();
    }

    /** @see SudokuController — lógica en HU-4 */
    @FXML
    private void onValidar() {
        boolean hayErrores = false;
        boolean completo   = true;

        for (int f = 0; f < TAMAÑO; f++) {
            for (int c = 0; c < TAMAÑO; c++) {
                if (modelo.getValor(f, c) == 0)           completo   = false;
                if (!modelo.esFija(f, c) && modelo.getValor(f, c) != 0
                        && modelo.hayConflicto(f, c))     hayErrores = true;
            }
        }

        if (hayErrores) {
            mostrarBanner("Hay errores en el tablero.", "banner-error");
        } else if (completo) {
            mostrarBanner("Sudoku completado correctamente.", "banner-ok");
        } else {
            mostrarBanner("Sin errores hasta ahora.", "banner-ok");
        }
    }

    /**
     * Borra todos los números ingresados por el jugador y restaura el tablero
     * al estado inicial de la partida actual (las pistas permanecen intactas).
     */
    @FXML
    private void onReiniciar() {
        celdaAyuda   = null;
        modelo.resetearJugador();
        renderizarTablero();
        actualizarTextoAyuda();
        ocultarMensajeBanner();
    }

    @FXML
    private void onAyuda() {
        if (modelo.getValorAyuda() == 35) {
            mostrarBanner("Las ayudas están desactivadas para el último numero", "banner-warn");
            return;
        }

        int[] sugerencia = modelo.sugerirCelda();
        if (sugerencia == null) {
            mostrarBanner("No hay celdas vacías disponibles.", "banner-warn");
            return;
        }

        limpiarResaltadoAyuda();

        int f = sugerencia[0], c = sugerencia[1], val = sugerencia[2];

        celdaAyuda = new int[]{f, c};
        TextField celda = celdas[f][c];
        celda.setText("? → " + val);
        celda.getStyleClass().removeAll("cell-user", "cell-error", "cell-hint");
        celda.getStyleClass().add("cell-hint");

        actualizarTextoAyuda();
        mostrarBanner(
                "Sugerencia: coloca " + val + " en fila " + (f + 1) + ", columna " + (c + 1) + ".",
                "banner-hint"
        );
    }

    // ── Ayuda: helpers ───────────────────────────────────────────────────────

    private void limpiarResaltadoAyuda() {
        if (celdaAyuda == null) return;
        int f = celdaAyuda[0], c = celdaAyuda[1];
        TextField celda = celdas[f][c];
        celda.getStyleClass().remove("cell-hint");
        int val = modelo.getValor(f, c);
        celda.setText(val == 0 ? "" : String.valueOf(val));
        celdaAyuda = null;
    }

    private void actualizarTextoAyuda() {
        btnAyuda.setText("Ayuda");
        btnAyuda.setDisable(false);
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    private void mostrarBanner(String texto, String estilo) {
        lblMensaje.setText(texto);
        lblMensaje.getStyleClass().removeAll("banner-ok", "banner-error", "banner-warn", "banner-hint");
        lblMensaje.getStyleClass().add(estilo);
        lblMensaje.setVisible(true);

        if (ocultarMensaje != null) ocultarMensaje.stop();
        ocultarMensaje = new Timeline(
                new KeyFrame(Duration.seconds(4), e -> lblMensaje.setVisible(false))
        );
        ocultarMensaje.play();
    }

    private void ocultarMensajeBanner() {
        if (ocultarMensaje != null) ocultarMensaje.stop();
        lblMensaje.setVisible(false);
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    /**
     * Lee la matriz del modelo y actualiza todas las celdas del GridPane.
     *
     * <p>Las celdas con valor fijo reciben la clase CSS {@code .cell-fixed}
     * y se bloquean completamente via {@link #bloquearCeldaFija(TextField)}.
     * Las celdas libres obtienen su {@link FiltroEntrada} via
     * {@link #habilitarCeldaEditable(int, int, TextField)} y quedan listas
     * para la interacción del jugador.</p>
     */
    private void renderizarTablero() {
        for (int fila = 0; fila < TAMAÑO; fila++) {
            for (int col = 0; col < TAMAÑO; col++) {
                TextField celda = celdas[fila][col];

                // Limpiar estilos de estado para que el repintado sea idempotente
                celda.getStyleClass().removeAll("cell-fixed", "cell-user", "cell-error", "cell-hint");

                if (modelo.esFija(fila, col)) {
                    celda.setText(String.valueOf(modelo.getValor(fila, col)));
                    celda.getStyleClass().add("cell-fixed");
                    bloquearCeldaFija(celda);
                } else {
                    int valor = modelo.getValor(fila, col);
                    celda.setText(valor == 0 ? "" : String.valueOf(valor));
                    if (valor != 0) celda.getStyleClass().add("cell-user");
                    habilitarCeldaEditable(fila, col, celda);
                }
            }
        }
    }

    private void actualizarValidacion() {
        for (int f = 0; f < TAMAÑO; f++) {
            for (int c = 0; c < TAMAÑO; c++) {
                if (modelo.esFija(f, c)) continue;
                TextField celda = celdas[f][c];
                celda.getStyleClass().removeAll("cell-error");
                if (modelo.getValor(f, c) != 0 && modelo.hayConflicto(f, c))
                    celda.getStyleClass().add("cell-error");
            }
        }
    }

    /**
     * Configura una celda de pista para que ignore por completo la interacción
     * del usuario: sin foco por teclado ni ratón, sin manejadores de eventos.
     *
     * @param celda TextField que representa la pista a bloquear
     */
    private void bloquearCeldaFija(TextField celda) {
        celda.setEditable(false);
        celda.setFocusTraversable(false);
        celda.setOnMouseClicked(null);
        celda.setOnKeyTyped(null);
        celda.setOnKeyPressed(null);
    }

    /**
     * Habilita la interacción del jugador sobre una celda libre: permite el
     * enfoque por clic y asocia el {@link FiltroEntrada} para interceptar
     * las pulsaciones de teclado antes de actualizar la vista y el modelo.
     *
     * <p>Se mantiene {@code setEditable(false)} intencionalmente para que todo
     * el control del texto quede en manos de {@link FiltroEntrada} y el
     * TextField no procese entradas por su cuenta.</p>
     *
     * @param fila  fila de la celda en el tablero (0–5)
     * @param col   columna de la celda en el tablero (0–5)
     * @param celda TextField a habilitar
     */
    private void habilitarCeldaEditable(int fila, int col, TextField celda) {
        celda.setEditable(false);
        celda.setFocusTraversable(true);
        // El clic otorga el foco para que los eventos de teclado lleguen a esta celda
        celda.setOnMouseClicked(e -> celda.requestFocus());

        FiltroEntrada filtro = new FiltroEntrada(fila, col);
        celda.setOnKeyTyped(filtro);
        celda.setOnKeyPressed(filtro);
    }

    // ── Estilos de borde (sin cambios respecto a HU-1) ───────────────────────

    /**
     * Asigna el borde correcto a cada celda según su posición en el tablero.
     *
     * <p>Estrategia de borde único por línea: cada celda dibuja solo su borde
     * derecho e inferior (más el marco en fila 0 y columna 0). Esto garantiza
     * que cada línea la pinte una sola celda y no haya acumulación de bordes.</p>
     *
     * <ul>
     *   <li>3 px azul cobalto → límite de bloque 2×3 o marco exterior</li>
     *   <li>1 px gris claro   → línea interna de la cuadrícula</li>
     *   <li>0 px transparente → lado que dibuja la celda vecina</li>
     * </ul>
     *
     * @param celda celda a estilizar
     * @param fila  fila de la celda (0–5)
     * @param col   columna de la celda (0–5)
     */
    private void applyBlockBorders(TextField celda, int fila, int col) {
        double top    = (fila == 0) ? 3 : 0;
        double left   = (col  == 0) ? 3 : 0;
        double bottom = (fila == 1 || fila == 3 || fila == 5) ? 3 : 1;
        double right  = (col  == 2 || col  == 5) ? 3 : 1;

        // JavaFX CSS: -fx-border-width sigue el orden  top | right | bottom | left
        celda.setStyle(String.format(
            "-fx-border-width: %.0f %.0f %.0f %.0f; -fx-border-color: %s %s %s %s;",
            top, right, bottom, left,
            borderColor(top), borderColor(right), borderColor(bottom), borderColor(left)
        ));
    }

    /** Azul cobalto para bordes de bloque, gris claro para líneas internas, transparente si no aplica. */
    private String borderColor(double width) {
        if (width == 0) return "transparent";
        if (width >= 3) return "#1a3a6b";
        return "#c0c8d4";
    }

    // ════════════════════════════════════════════════════════════════════════
    // Clase interna adaptadora: FiltroEntrada
    // Intercepta eventos de teclado sobre las celdas editables del jugador,
    // acepta solo dígitos del 1 al 6 y rechaza cualquier otra entrada.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Adaptadora de eventos de teclado para las celdas libres del tablero.
     *
     * <p>Se registra como manejador tanto de {@code KEY_TYPED} como de
     * {@code KEY_PRESSED} en la celda objetivo. El método {@link #handle}
     * delega al sub-manejador correcto según el tipo de evento:</p>
     *
     * <ul>
     *   <li>{@code KEY_TYPED} → {@link #manejarCaracter}: permite '1'–'6' y
     *       consume todo lo demás antes de que el TextField lo procese.</li>
     *   <li>{@code KEY_PRESSED} → {@link #manejarBorrado}: limpia la celda
     *       si la tecla es {@code BACK_SPACE} o {@code DELETE}.</li>
     * </ul>
     *
     * <p>El consumo del evento ({@code evento.consume()}) es el mecanismo que
     * impide que JavaFX propague la entrada al comportamiento interno del
     * TextField, manteniendo el control exclusivo sobre el texto visible.</p>
     */
    private final class FiltroEntrada implements EventHandler<KeyEvent> {

        /** Fila de la celda supervisada por este filtro. */
        private final int fila;

        /** Columna de la celda supervisada por este filtro. */
        private final int col;

        /**
         * @param fila fila de la celda objetivo en el tablero (0–5)
         * @param col  columna de la celda objetivo en el tablero (0–5)
         */
        FiltroEntrada(int fila, int col) {
            this.fila = fila;
            this.col  = col;
        }

        /**
         * Punto de entrada unificado: delega al sub-manejador adecuado según
         * el tipo de evento recibido (presión física de tecla o carácter tipeado).
         *
         * @param evento evento de teclado generado por JavaFX
         */
        @Override
        public void handle(KeyEvent evento) {
            if (evento.getEventType() == KeyEvent.KEY_PRESSED) {
                manejarBorrado(evento);
            } else if (evento.getEventType() == KeyEvent.KEY_TYPED) {
                manejarCaracter(evento);
            }
        }

        /**
         * Detecta las teclas de borrado y vacía la celda tanto en la vista
         * como en el modelo cuando el usuario las presiona.
         *
         * @param evento evento KEY_PRESSED capturado por el filtro
         */
        private void manejarBorrado(KeyEvent evento) {
            KeyCode codigo = evento.getCode();
            if (codigo == KeyCode.BACK_SPACE || codigo == KeyCode.DELETE) {
                limpiarResaltadoAyuda();
                TextField celda = celdas[fila][col];
                celda.setText("");
                celda.getStyleClass().removeAll("cell-user", "cell-error");
                modelo.setValor(fila, col, 0);
                actualizarValidacion();
                evento.consume();
            }
        }

        /**
         * Filtra el carácter tipeado: si está en el rango '1'–'6', lo escribe
         * en la celda con la clase CSS {@code .cell-user} y persiste el valor
         * en el modelo. Cualquier otro carácter (letras, símbolos, 0, 7–9) es
         * consumido antes de llegar al TextField, dejando la celda intacta.
         *
         * @param evento evento KEY_TYPED capturado por el filtro
         */
        private void manejarCaracter(KeyEvent evento) {
            String caracter = evento.getCharacter();
            if (caracter.length() == 1) {
                char ch = caracter.charAt(0);
                if (ch >= '1' && ch <= '6') {
                    int numero = ch - '0';
                    limpiarResaltadoAyuda();
                    TextField celda = celdas[fila][col];
                    celda.setText(String.valueOf(numero));
                    celda.getStyleClass().removeAll("cell-user", "cell-error", "cell-hint");
                    celda.getStyleClass().add("cell-user");
                    modelo.setValor(fila, col, numero);


                    actualizarValidacion();   // valida todo el tablero

                    if (modelo.hayConflicto(fila, col)) {
                        celda.getStyleClass().remove("cell-user");
                        celda.getStyleClass().add("cell-error");
                        mostrarBanner("Número repetido en fila, columna o bloque.", "banner-error");
                    }

                    evento.consume();
                    return;
                }
            }
            // carácter fuera del rango permitido: descartar sin modificar la celda
            evento.consume();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Clase interna adaptadora: GestorAlerta
    // Encapsula la construcción y evaluación del diálogo de confirmación,
    // separando su lógica del flujo principal del controlador.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Adaptadora que centraliza la creación y evaluación del diálogo modal
     * de confirmación nativo de JavaFX.
     *
     * <p>Separar este comportamiento del controlador facilita extender el
     * diálogo (icono personalizado, botones adicionales) sin modificar
     * el flujo de {@code onIniciarJuego}.</p>
     */
    private static final class GestorAlerta {

        private final String titulo;
        private final String encabezado;
        private final String contenido;

        /**
         * @param titulo     texto de la barra de título de la ventana del diálogo
         * @param encabezado texto del encabezado interior del diálogo
         * @param contenido  texto del cuerpo con la pregunta al usuario
         */
        GestorAlerta(String titulo, String encabezado, String contenido) {
            this.titulo     = titulo;
            this.encabezado = encabezado;
            this.contenido  = contenido;
        }

        /**
         * Muestra el diálogo de forma modal y espera la respuesta.
         *
         * @return {@code true} solo si el usuario presionó "Aceptar"
         */
        boolean confirmar() {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle(titulo);
            alerta.setHeaderText(encabezado);
            alerta.setContentText(contenido);
            return alerta.showAndWait()
                         .filter(r -> r == ButtonType.OK)
                         .isPresent();
        }
    }
}
