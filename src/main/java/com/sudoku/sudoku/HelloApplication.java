package com.sudoku.sudoku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada de la aplicación JavaFX.
 *
 * <p>Carga la vista principal del Sudoku y fija el tamaño de ventana
 * al ancho mínimo que necesita el tablero de 6×6 (6 celdas × 64 px)
 * más los márgenes de la interfaz.</p>
 *
 * @author Jhon Acosta
 * @version 1.1
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            HelloApplication.class.getResource("views/sudoku-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 620, 680);
        stage.setTitle("Sudoku");
        stage.setMinWidth(620);
        stage.setMinHeight(680);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }
}
