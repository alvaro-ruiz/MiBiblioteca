package Controller;

import Model.Usuario;
import Model.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import Main.MainApp;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    //private UserService userService = new UserService();

    @FXML
    void initialize() {
        // Inicialización si es necesaria
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error de inicio de sesión", 
                    "Por favor, complete todos los campos.");
            return;
        }

        try {
            if (email.contains("@")) {
                Usuario loggedInUser = new Usuario();
                userService.setCurrentUser(loggedInUser);
                
                // Navegar a la pantalla principal
                navigateToDashboard(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error de inicio de sesión", 
                        "Credenciales incorrectas. Por favor, inténtelo de nuevo.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Ha ocurrido un error: " + e.getMessage());
        }
    }

    @FXML
    void handleRegister(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(MainApp.class.getResource("/View/Register.fxml"));
            Scene registerScene = new Scene(registerView);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(registerScene);
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "No se pudo cargar la pantalla de registro: " + e.getMessage());
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent dashboardView = FXMLLoader.load(MainApp.class.getResource("/fxml/Dashboard.fxml"));
            Scene dashboardScene = new Scene(dashboardView);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(dashboardScene);
            window.setTitle("Biblioteca Digital - Panel Principal");
            window.setMaximized(true);
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "No se pudo cargar el panel principal: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

