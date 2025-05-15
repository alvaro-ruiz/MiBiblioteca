package Controller;

import Model.Book;
import Model.BookCollection;
import Model.UserService;
import bd.DaoBook;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.function.Consumer;

public class EditarLibroController {

    @FXML private Label lblTitulo;
    @FXML private ComboBox<String> comboEstado;
    @FXML private VBox seccionLeido;
    @FXML private VBox seccionPrestado;
    @FXML private DatePicker fechaLectura;
    @FXML private Slider sliderNota;
    @FXML private Label lblNota;
    @FXML private TextArea txtComentario;
    @FXML private TextField txtPrestadoA;
    @FXML private DatePicker fechaPrestamo;
    @FXML private CheckBox chkDevuelto;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;
    
    private BookCollection bookCollection;
    private UserService userService = new UserService();
    private Consumer<Void> onSaveCallback;
    
    @FXML
    void initialize() {
        // Inicializar ComboBox de estados
        comboEstado.setItems(FXCollections.observableArrayList(
            "Leído", "Leyendo", "Pendiente", "Prestado", "Deseado"
        ));
        
        // Configurar listener para el cambio de estado
        comboEstado.valueProperty().addListener((observable, oldValue, newValue) -> {
            actualizarVisibilidadSecciones(newValue);
        });
        
        // Configurar listener para el slider de nota
        sliderNota.valueProperty().addListener((observable, oldValue, newValue) -> {
            lblNota.setText(String.format("%.1f", newValue.doubleValue()));
        });
        
        // Ocultar secciones inicialmente
        seccionLeido.setVisible(false);
        seccionLeido.setManaged(false);
        seccionPrestado.setVisible(false);
        seccionPrestado.setManaged(false);
    }
    
    public void setBookCollection(BookCollection bookCollection) {
        this.bookCollection = bookCollection;
        
        // Actualizar título
        lblTitulo.setText("Editar " + bookCollection.getBook().getTitle());
        
        // Seleccionar estado actual
        String estado = bookCollection.getEstado();
        comboEstado.setValue(capitalizarPrimeraLetra(estado));
        
        // Cargar datos según el estado
        if ("leído".equals(estado)) {
            fechaLectura.setValue(bookCollection.getFechaLectura());
            sliderNota.setValue(bookCollection.getNota());
            txtComentario.setText(bookCollection.getComentario());
        } else if ("prestado".equals(estado)) {
            txtPrestadoA.setText(bookCollection.getPrestadoA());
            fechaPrestamo.setValue(bookCollection.getFechaPrestamo());
            chkDevuelto.setSelected(bookCollection.isDevuelto());
        }
        
        // Actualizar visibilidad de secciones
        actualizarVisibilidadSecciones(comboEstado.getValue());
    }
    
    private void actualizarVisibilidadSecciones(String estado) {
        if (estado == null) return;
        
        seccionLeido.setVisible("Leído".equals(estado));
        seccionLeido.setManaged("Leído".equals(estado));
        
        seccionPrestado.setVisible("Prestado".equals(estado));
        seccionPrestado.setManaged("Prestado".equals(estado));
    }
    
    @FXML
    void handleCancelar(ActionEvent event) {
        cerrarVentana();
    }
    
    // Mejorar el método handleGuardar para incluir la opción de mover entre categorías
    @FXML
    void handleGuardar(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe iniciar sesión para guardar cambios.");
            return;
        }
        
        int userId = userService.getCurrentUser().getId();
        Book libro = bookCollection.getBook();
        String estado = comboEstado.getValue().toLowerCase();
        
        // Verificar si el estado ha cambiado
        boolean cambioEstado = !estado.equals(bookCollection.getEstado());
        
        // Guardar estado del libro
        boolean estadoGuardado = DaoBook.guardarEstadoLibro(userId, libro, estado);
        
        // Guardar información adicional según el estado
        boolean infoAdicionalGuardada = true;
        
        if ("leído".equals(estado)) {
            double nota = sliderNota.getValue();
            String comentario = txtComentario.getText();
            infoAdicionalGuardada = DaoBook.guardarInformacionLectura(userId, libro, fechaLectura.getValue(), nota, comentario);
        } else if ("prestado".equals(estado)) {
            String prestadoA = txtPrestadoA.getText();
            if (prestadoA == null || prestadoA.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe indicar a quién prestó el libro.");
                return;
            }
            boolean devuelto = chkDevuelto.isSelected();
            infoAdicionalGuardada = DaoBook.guardarInformacionPrestamo(userId, libro, prestadoA, fechaPrestamo.getValue(), devuelto);
        }
        
        if (estadoGuardado && infoAdicionalGuardada) {
            String mensaje = cambioEstado 
                ? "Libro movido a " + estado + " correctamente."
                : "Información del libro guardada correctamente.";
            showAlert(Alert.AlertType.INFORMATION, "Éxito", mensaje);
            if (onSaveCallback != null) {
                onSaveCallback.accept(null);
            }
            cerrarVentana();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar la información del libro.");
        }
    }
    
    public void setOnSaveCallback(Consumer<Void> callback) {
        this.onSaveCallback = callback;
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
    
    private String capitalizarPrimeraLetra(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
