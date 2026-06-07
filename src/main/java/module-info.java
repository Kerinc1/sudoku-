module com.sudoku.sudoku {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    // Paquete raíz: Application y Launcher
    opens   com.sudoku.sudoku to javafx.fxml;
    exports com.sudoku.sudoku;

    // FXML necesita acceso reflectivo al controlador para inyectar @FXML
    opens   com.sudoku.sudoku.controller to javafx.fxml;
    exports com.sudoku.sudoku.controller;

    // Modelo disponible para los demás paquetes del módulo
    exports com.sudoku.sudoku.model;
}