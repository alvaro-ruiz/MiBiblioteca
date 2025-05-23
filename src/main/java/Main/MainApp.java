package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.io.IOException;

public class MainApp extends Application{

    public void start(Stage primaryStage) throws Exception {
        try {
            Parent root = null;
            try {
                root = FXMLLoader.load(MainApp.class.getResource("/View/Login.fxml"));
            } catch (Exception e1) {
                try {
                    root = FXMLLoader.load(MainApp.class.getClassLoader().getResource("View/Login.fxml"));
                } catch (Exception e2) {
                    root = FXMLLoader.load(getClass().getResource("../View/Login.fxml"));
                }
            }

            if (root == null) {
                throw new IOException("No se pudo cargar el archivo FXML de inicio de sesión.");
            }

            primaryStage.setTitle("Biblioteca Digital - Google Books");
            primaryStage.setScene(new Scene(root, 600, 400));
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Inicialización");
            alert.setHeaderText("Error al iniciar la aplicación");
            alert.setContentText("Detalles: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
