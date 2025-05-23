package Controller;

import Model.Book;
import Model.Opinion;
import Model.UserService;
import bd.DaoOpinion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OpinionesController {

    @FXML private Label lblTituloLibro;
    @FXML private ListView<Opinion> listaOpiniones;
    @FXML private VBox panelNuevaOpinion;
    @FXML private Slider sliderNota;
    @FXML private Label lblNota;
    @FXML private TextArea txtComentario;
    @FXML private Button btnPublicar;
    @FXML private Button btnCerrar;
    
    private Book book;
    private UserService userService = new UserService();
    private ObservableList<Opinion> opiniones = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    @FXML
    void initialize() {
        // Configurar el slider de nota
        sliderNota.valueProperty().addListener((observable, oldValue, newValue) -> {
            lblNota.setText(String.format("%.1f", newValue.doubleValue()));
        });
        
        // Configurar la celda personalizada para la lista de opiniones
        listaOpiniones.setCellFactory(param -> new ListCell<Opinion>() {
            @Override
            protected void updateItem(Opinion opinion, boolean empty) {
                super.updateItem(opinion, empty);
                
                if (empty || opinion == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    
                    Label lblUsuario = new Label(opinion.getNombreUsuario());
                    lblUsuario.setStyle("-fx-font-weight: bold;");
                    
                    Label lblFecha = new Label(opinion.getFecha().format(dateFormatter));
                    lblFecha.setStyle("-fx-font-size: 10px; -fx-text-fill: #757575;");
                    
                    Label lblNotaOpinion = new Label("Valoración: " + String.format("%.1f", opinion.getNota()) + "/10");
                    lblNotaOpinion.setStyle("-fx-font-style: italic;");
                    
                    Label lblComentario = new Label(opinion.getComentario());
                    lblComentario.setWrapText(true);
                    
                    container.getChildren().addAll(lblUsuario, lblFecha, lblNotaOpinion, lblComentario);
                    
                    // Añadir un separador si no es el último elemento
                    if (getIndex() < getListView().getItems().size() - 1) {
                        Separator separator = new Separator();
                        separator.setStyle("-fx-padding: 5 0 0 0;");
                        container.getChildren().add(separator);
                    }
                    
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        listaOpiniones.setItems(opiniones);
    }
    
    public void setBook(Book book) {
        this.book = book;
        lblTituloLibro.setText("Opiniones sobre: " + book.getTitle());
        
        // Cargar opiniones del libro
        cargarOpiniones();
        
        // Verificar si el usuario está logueado para mostrar el panel de nueva opinión
        if (userService.getCurrentUser() == null) {
            panelNuevaOpinion.setVisible(false);
            panelNuevaOpinion.setManaged(false);
        }
    }
    
    private void cargarOpiniones() {
        opiniones.clear();
        
        // Obtener opiniones de la base de datos
        List<Opinion> opinionesList = DaoOpinion.getOpinionesPorLibro(book.getId());
        if (opinionesList != null && !opinionesList.isEmpty()) {
            opiniones.addAll(opinionesList);
        }
    }
    
    @FXML
    void handlePublicar(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe iniciar sesión para publicar opiniones.");
            return;
        }
        
        if (txtComentario.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos incompletos", "El comentario no puede estar vacío.");
            return;
        }
        
        try {
            // Crear nueva opinión
            Opinion nuevaOpinion = new Opinion(
                0, // ID temporal, se asignará en la base de datos
                userService.getCurrentUser().getId(),
                userService.getCurrentUser().getNombre(),
                book.getId(),
                sliderNota.getValue(),
                txtComentario.getText().trim(),
                LocalDate.now()
            );
            
            // Guardar opinión en la base de datos
            if (DaoOpinion.agregarOpinion(nuevaOpinion)) {
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "Tu opinión ha sido publicada correctamente.");
                txtComentario.clear();
                sliderNota.setValue(5.0);
                
                // Recargar opiniones
                cargarOpiniones();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo publicar la opinión. Inténtalo de nuevo.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al publicar la opinión: " + e.getMessage());
        }
    }
    
    @FXML
    void handleCerrar(ActionEvent event) {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
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
