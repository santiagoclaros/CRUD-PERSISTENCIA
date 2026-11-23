package com.finanzas;

import com.finanzas.repository.MovimientoRepository;
import com.finanzas.service.FinanzasService;
import com.finanzas.ui.FinanzasController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FinanzasService service = new FinanzasService(
                    new MovimientoRepository(Path.of("movimientos.csv")));
            FinanzasController controller = new FinanzasController(service);
            Scene scene = new Scene(controller.createView(), 1180, 760);
            stage.setTitle("Finanzas personales");
            stage.setMinWidth(980);
            stage.setMinHeight(660);
            stage.setScene(scene);
            stage.show();
        } catch (IOException | RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudo iniciar");
            alert.setHeaderText("La aplicación no pudo cargar los movimientos.");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
            javafx.application.Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
