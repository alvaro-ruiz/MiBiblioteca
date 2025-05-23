package Controller;

import Model.Book;
import Model.UserService;
import Model.Usuario;
import bd.DaoBook;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AgregarLibroManualController {

    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private TextField txtEditorial;
    @FXML private TextField txtIsbn;
    @FXML private DatePicker fechaPublicacion;
    @FXML private ComboBox<String> comboGenero;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Button btnVolver;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private Usuario usuario;

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    private UserService userService = new UserService();
    
    @FXML
    void initialize() {
        // Inicializar ComboBox de géneros
        comboGenero.setItems(FXCollections.observableArrayList(
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
        ));
        comboGenero.getSelectionModel().selectFirst();
        
        // Inicializar ComboBox de estados
        comboEstado.setItems(FXCollections.observableArrayList(
            "Leído", "Leyendo", "Pendiente", "Prestado", "Deseado", "Favorito"
        ));
        comboEstado.getSelectionModel().select("Pendiente");
        
        // Inicializar fecha de publicación con la fecha actual
        fechaPublicacion.setValue(LocalDate.now());
    }
    
    @FXML
    void handleVolver(ActionEvent event) {
        volverAPantallaPrincipal();
    }
    
    @FXML
    void handleCancelar(ActionEvent event) {
        volverAPantallaPrincipal();
    }
    
    @FXML
    void handleGuardar(ActionEvent event) {
        // Obtener el usuario actual, ya sea del servicio o del que se pasó directamente
        Usuario usuarioActual = usuario != null ? usuario : userService.getCurrentUser();
        
        if (usuarioActual == null) {
            showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe iniciar sesión para guardar libros.");
            return;
        }
        
        // Validar campos obligatorios
        if (txtTitulo.getText().trim().isEmpty() || txtAutor.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos incompletos", "El título y el autor son obligatorios.");
            return;
        }
        
        // Validar que se haya seleccionado un estado
        if (comboEstado.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Campos incompletos", "Debe seleccionar un estado para el libro.");
            return;
        }
        
        // Validar que se haya seleccionado un género
        if (comboGenero.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Campos incompletos", "Debe seleccionar un género para el libro.");
            return;
        }
        
        try {
            // Crear un ID único para el libro
            String uniqueId = UUID.randomUUID().toString();
            
            // Crear lista de autores (separados por comas)
            List<String> autores = new ArrayList<>(Arrays.asList(txtAutor.getText().split(",")));
            
            // Crear lista de géneros
            List<String> generos = new ArrayList<>();
            generos.add(comboGenero.getValue());
            
            // Crear objeto Book
            Book libro = new Book(
                uniqueId,
                txtIsbn.getText().trim(),
                txtTitulo.getText().trim(),
                autores,
                txtDescripcion.getText().trim()
            );
            
            // Establecer editorial
            libro.setPublisher(txtEditorial.getText().trim());
            
            // Establecer fecha de publicación
            if (fechaPublicacion.getValue() != null) {
                libro.setPublishedDate(fechaPublicacion.getValue().toString());
            }
            
            // Establecer categorías
            libro.setCategories(generos);
            
            // Guardar el libro en la base de datos
            int userId = usuarioActual.getId();
            String estado = comboEstado.getValue().toLowerCase();
            
            System.out.println("Intentando guardar libro: " + libro.getTitle());
            System.out.println("Usuario ID: " + userId);
            System.out.println("Estado: " + estado);
            
            // Manejar el caso especial de "Favorito"
            if (estado.equals("favorito")) {
                if (DaoBook.guardarFavorito(userId, libro)) {
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Libro agregado correctamente a tus favoritos.");
                    cerrarVentana();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar el libro como favorito. Inténtalo de nuevo.");
                }
            } else {
                if (DaoBook.guardarLibroManual(userId, libro, estado)) {
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Libro agregado correctamente a tu colección.");
                    cerrarVentana();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar el libro. Inténtalo de nuevo.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al guardar el libro: " + e.getMessage());
        }
    }
    
    private void volverAPantallaPrincipal() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
