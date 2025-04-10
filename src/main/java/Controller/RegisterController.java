package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import Main.MainApp;
import Model.Usuario;
import bd.DaoUser;

public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private ComboBox<String> favoriteGenreComboBox;

    @FXML
    private Button registerButton;

    @FXML
    private Button backToLoginButton;
    
    @FXML
    void initialize() {
        favoriteGenreComboBox.getItems().addAll(
            "Ficción",
            "No ficción",
            "Fantasía",
            "Ciencia ficción",
            "Misterio",
            "Thriller",
            "Romance",
            "Historia",
            "Biografía",
            "Autoayuda",
            "Ciencia",
            "Tecnología",
            "Programación",
            "Arte",
            "Cocina",
            "Viajes",
            "Poesía",
            "Drama",
            "Cómics",
            "Infantil"
        );
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String favoriteGenre = favoriteGenreComboBox.getValue();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "Por favor, complete todos los campos obligatorios.");
            return;
        }

        if (favoriteGenre == null) {
            showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "Por favor, seleccione un género favorito.");
            return;
        }


        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "Las contraseñas no coinciden.");
            return;
        }

        if (!email.contains("@")) {
            showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "Por favor, ingrese un correo electrónico válido.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        if (DaoUser.getUsuarioByEmail(email) != null) {
        	showAlert(Alert.AlertType.ERROR, "Error de registro", 
                    "Correo electronico ya existe");
            return;
        }
        
        if (DaoUser.addUsuario(new Usuario(name, email, favoriteGenre, password))){
        	 showAlert(Alert.AlertType.INFORMATION, "Registro exitoso", 
                     "Se ha registrado correctamente. Ahora puede iniciar sesión.");
        } else {
        	showAlert(Alert.AlertType.ERROR, "Algo ha salido mal", 
                    "Se ha intentado crear el usuario pero algo ha salido mal");
        }
        
        navigateToLogin(event);
    }

    @FXML
    void handleBackToLogin(ActionEvent event) {
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(MainApp.class.getResource("/View/Login.fxml"));
            Scene loginScene = new Scene(loginView);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "No se pudo cargar la pantalla de inicio de sesión: " + e.getMessage());
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

