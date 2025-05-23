package Controller;

import Model.Book;
import Model.UserService;
import Api.GoogleBooksAPI;
import bd.DaoBook;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class DetallesController {

    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label publisherLabel;
    @FXML private Label publishedDateLabel;
    @FXML private Label pageCountLabel;
    @FXML private Label isbnLabel;
    @FXML private FlowPane categoriesPane;
    @FXML private WebView descriptionWebView;
    @FXML private Button favoriteButton;
    @FXML private Hyperlink previewLink;
    @FXML private Button closeButton;
    
    // Controles para el estado del libro
    @FXML private ComboBox<String> estadoComboBox;
    @FXML private Button guardarEstadoButton;
    @FXML private Button btnVerOpiniones;
    
    // Controles para libros leídos
    @FXML private VBox seccionLeido;
    @FXML private DatePicker fechaLecturaPicker;
    @FXML private Slider valoracionSlider;
    @FXML private Label valoracionLabel;
    @FXML private TextArea comentarioTextArea;
    @FXML private Button guardarLecturaButton;
    
    // Controles para libros prestados
    @FXML private VBox seccionPrestado;
    @FXML private TextField prestadoATextField;
    @FXML private DatePicker fechaPrestamoDatePicker;
    @FXML private ComboBox<String> devueltoComboBox;
    @FXML private Button guardarPrestamoButton;

    private Book book;
    private UserService user = new UserService();
    private boolean isFavorite = false;
    private ObservableList<Book> favorites;
    private Runnable onFavoriteChangedCallback;
    private String estadoActual = "pendiente";

    @FXML
    void initialize() {
        // Inicializar ComboBox de estados
        estadoComboBox.setItems(FXCollections.observableArrayList(
            "pendiente", "leyendo", "leído", "prestado", "deseado"
        ));
        estadoComboBox.getSelectionModel().selectFirst();
        
        // Inicializar ComboBox de devuelto
        devueltoComboBox.setItems(FXCollections.observableArrayList("No", "Sí"));
        devueltoComboBox.getSelectionModel().selectFirst();
        
        // Inicializar DatePickers con la fecha actual
        fechaLecturaPicker.setValue(LocalDate.now());
        fechaPrestamoDatePicker.setValue(LocalDate.now());
        
        // Listener para el slider de valoración
        valoracionSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            valoracionLabel.setText(String.format("%.1f", newValue.doubleValue()));
        });
        
        // Listener para el cambio de estado
        estadoComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            actualizarSeccionesVisibles(newValue);
        });
    }
    
    private void actualizarSeccionesVisibles(String estado) {
        // Ocultar todas las secciones primero
        seccionLeido.setVisible(false);
        seccionPrestado.setVisible(false);
        
        // Mostrar la sección correspondiente según el estado seleccionado
        switch (estado) {
            case "leído":
                seccionLeido.setVisible(true);
                break;
            case "prestado":
                seccionPrestado.setVisible(true);
                break;
        }
    }

    public void setBook(Book book) {
        this.book = book;
        
        // Cargar datos básicos
        titleLabel.setText(book.getTitle());
        authorLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() 
                ? String.join(", ", book.getAuthors()) 
                : "Autor desconocido");
        
        // Cargar imagen de portada
        if (book.getThumbnail() != null && !book.getThumbnail().isEmpty()) {
            try {
                // Asegurarse de que la URL use HTTPS
                String imageUrl = book.getThumbnail();
                if (imageUrl.startsWith("http:")) {
                    imageUrl = imageUrl.replace("http:", "https:");
                }
                
                System.out.println("Cargando imagen en detalles: " + imageUrl);
                Image image = new Image(imageUrl, true);
                
                // Agregar un listener para manejar errores de carga
                image.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        System.err.println("Error al cargar la imagen en detalles: ");
                        loadFallbackImage();
                    }
                });
                
                coverImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Excepción al cargar la imagen en detalles: " + e.getMessage());
                loadFallbackImage();
            }
        } else {
            loadFallbackImage();
        }
        
        // Verificar si el libro ya está en favoritos
        if (user.getCurrentUser() != null) {
            try {
                isFavorite = DaoBook.isFavorite(user.getCurrentUser().getId(), book.getId());
                favoriteButton.setText(isFavorite ? "Quitar de favoritos" : "Añadir a favoritos");
                
                // Cargar el estado actual del libro
                cargarEstadoLibro();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Cargar detalles completos del libro
        loadBookDetails();
    }
    
    private void cargarEstadoLibro() {
        try {
            String estado = DaoBook.getEstadoLibro(user.getCurrentUser().getId(), book.getId());
            if (estado != null && !estado.isEmpty()) {
                estadoActual = estado;
                estadoComboBox.getSelectionModel().select(estado);
                actualizarSeccionesVisibles(estado);
                
                // Si el libro está leído, cargar información de lectura
                if ("leído".equals(estado)) {
                    cargarInformacionLectura();
                }
                
                // Si el libro está prestado, cargar información de préstamo
                if ("prestado".equals(estado)) {
                    cargarInformacionPrestamo();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar el estado del libro: " + e.getMessage());
        }
    }
    
    private void cargarInformacionLectura() {
        try {
            // Obtener información de lectura
            Object[] infoLectura = DaoBook.getInformacionLectura(user.getCurrentUser().getId(), book.getId());
            if (infoLectura != null) {
                LocalDate fechaLectura = (LocalDate) infoLectura[0];
                Double nota = (Double) infoLectura[1];
                String comentario = (String) infoLectura[2];
                
                // Actualizar controles
                if (fechaLectura != null) {
                    fechaLecturaPicker.setValue(fechaLectura);
                }
                if (nota != null) {
                    valoracionSlider.setValue(nota);
                    valoracionLabel.setText(String.format("%.1f", nota));
                }
                if (comentario != null) {
                    comentarioTextArea.setText(comentario);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la información de lectura: " + e.getMessage());
        }
    }
    
    private void cargarInformacionPrestamo() {
        try {
            // Obtener información de préstamo
            Object[] infoPrestamo = DaoBook.getInformacionPrestamo(user.getCurrentUser().getId(), book.getId());
            if (infoPrestamo != null) {
                String prestadoA = (String) infoPrestamo[0];
                LocalDate fechaPrestamo = (LocalDate) infoPrestamo[1];
                Boolean devuelto = (Boolean) infoPrestamo[2];
                
                // Actualizar controles
                if (prestadoA != null) {
                    prestadoATextField.setText(prestadoA);
                }
                if (fechaPrestamo != null) {
                    fechaPrestamoDatePicker.setValue(fechaPrestamo);
                }
                devueltoComboBox.getSelectionModel().select(devuelto ? "Sí" : "No");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la información de préstamo: " + e.getMessage());
        }
    }
    
    public void setFavorites(ObservableList<Book> favorites) {
        this.favorites = favorites;
        // Verificar si el libro está en favoritos
        isFavorite = favorites.stream().anyMatch(b -> b.getId().equals(book.getId()));
        favoriteButton.setText(isFavorite ? "Quitar de favoritos" : "Añadir a favoritos");
    }
    
    public void setOnFavoriteChangedCallback(Runnable callback) {
        this.onFavoriteChangedCallback = callback;
    }

    private void loadBookDetails() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBookById(book.getId());
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(detailedBook -> {
            Platform.runLater(() -> {
                if (detailedBook != null) {
                    updateBookDetails(detailedBook);
                }
            });
        });
    }

    private void updateBookDetails(Book detailedBook) {
        // Actualizar el libro con los detalles completos
        this.book = detailedBook;
        
        // Actualizar la interfaz con los detalles adicionales
        publisherLabel.setText(detailedBook.getPublisher() != null ? detailedBook.getPublisher() : "No disponible");
        publishedDateLabel.setText(detailedBook.getPublishedDate() != null ? detailedBook.getPublishedDate() : "No disponible");
        pageCountLabel.setText(detailedBook.getPageCount() > 0 ? String.valueOf(detailedBook.getPageCount()) : "No disponible");
        isbnLabel.setText(detailedBook.getIsbn() != null ? detailedBook.getIsbn() : "No disponible");
        
        // Mostrar categorías
        if (detailedBook.getCategories() != null && !detailedBook.getCategories().isEmpty()) {
            categoriesPane.getChildren().clear();
            for (String category : detailedBook.getCategories()) {
                Label categoryLabel = new Label(category);
                categoryLabel.getStyleClass().add("category-chip");
                categoriesPane.getChildren().add(categoryLabel);
            }
        }
        
        // Mostrar descripción
        if (detailedBook.getDescription() != null && !detailedBook.getDescription().isEmpty()) {
            String htmlContent = "<html><body style='font-family: Arial; font-size: 14px;'>" 
                    + detailedBook.getDescription() + "</body></html>";
            descriptionWebView.getEngine().loadContent(htmlContent);
        } else {
            descriptionWebView.getEngine().loadContent("<html><body style='font-family: Arial; font-size: 14px;'><p>No hay descripción disponible.</p></body></html>");
        }
        
        // Configurar enlace de vista previa
        if (detailedBook.getPreviewLink() != null && !detailedBook.getPreviewLink().isEmpty()) {
            previewLink.setVisible(true);
            previewLink.setOnAction(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(detailedBook.getPreviewLink()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } else {
            previewLink.setVisible(false);
        }
    }

    @FXML
    void handleToggleFavorite(ActionEvent event) {
        if (user.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para agregar favoritos");
            return;
        }
        
        try {
            isFavorite = !isFavorite;

            if (isFavorite) {
                favoriteButton.setText("Quitar de favoritos");
                if (DaoBook.guardarFavorito(user.getCurrentUser().getId(), book)) {
                    if (!favorites.stream().anyMatch(b -> b.getId().equals(book.getId()))) {
                        favorites.add(book);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Libro agregado a favoritos");
                } else {
                    isFavorite = false;
                    favoriteButton.setText("Añadir a favoritos");
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo agregar el libro a favoritos");
                }
            } else {
                favoriteButton.setText("Añadir a favoritos");
                if (DaoBook.removeBookFromFavorites(user.getCurrentUser().getId(), book.getId())) {
                    favorites.removeIf(b -> b.getId().equals(book.getId()));
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Libro eliminado de favoritos");
                } else {
                    isFavorite = true;
                    favoriteButton.setText("Quitar de favoritos");
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo eliminar el libro de favoritos");
                }
            }

            if (onFavoriteChangedCallback != null) {
                onFavoriteChangedCallback.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error: " + e.getMessage());
        }
    }
    
    // Mejorar el método handleGuardarEstado para mostrar un mensaje más claro
    @FXML
    void handleGuardarEstado(ActionEvent event) {
        if (user.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para cambiar el estado del libro");
            return;
        }
        
        String nuevoEstado = estadoComboBox.getValue();
        
        try {
            if (DaoBook.guardarEstadoLibro(user.getCurrentUser().getId(), book, nuevoEstado)) {
                estadoActual = nuevoEstado;
                String mensaje = "Estado del libro actualizado a: " + nuevoEstado;
                
                // Añadir información adicional según el estado
                if ("leído".equals(nuevoEstado)) {
                    mensaje += "\nAhora puede añadir su valoración y comentarios en la sección correspondiente.";
                } else if ("prestado".equals(nuevoEstado)) {
                    mensaje += "\nNo olvide registrar a quién prestó el libro en la sección correspondiente.";
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Éxito", mensaje);
                actualizarSeccionesVisibles(nuevoEstado);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo actualizar el estado del libro");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGuardarLectura(ActionEvent event) {
        if (user.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para guardar información de lectura");
            return;
        }
        
        LocalDate fechaLectura = fechaLecturaPicker.getValue();
        double nota = valoracionSlider.getValue();
        String comentario = comentarioTextArea.getText();
        
        try {
            if (DaoBook.guardarInformacionLectura(user.getCurrentUser().getId(), book, fechaLectura, nota, comentario)) {
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "Información de lectura guardada correctamente");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar la información de lectura");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGuardarPrestamo(ActionEvent event) {
        if (user.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para guardar información de préstamo");
            return;
        }
        
        String prestadoA = prestadoATextField.getText();
        LocalDate fechaPrestamo = fechaPrestamoDatePicker.getValue();
        boolean devuelto = "Sí".equals(devueltoComboBox.getValue());
        
        if (prestadoA == null || prestadoA.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe indicar a quién prestó el libro");
            return;
        }
        
        try {
            if (DaoBook.guardarInformacionPrestamo(user.getCurrentUser().getId(), book, prestadoA, fechaPrestamo, devuelto)) {
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "Información de préstamo guardada correctamente");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar la información de préstamo");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error: " + e.getMessage());
        }
    }

    // Agregar método para mostrar alertas
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    void handleVerOpiniones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Opiniones.fxml"));
            Parent opinionesView = loader.load();
            
            OpinionesController controller = loader.getController();
            controller.setBook(book);
            
            Stage opinionesStage = new Stage();
            opinionesStage.setTitle("Opiniones sobre " + book.getTitle());
            opinionesStage.setScene(new Scene(opinionesView, 600, 500));
            opinionesStage.initModality(Modality.WINDOW_MODAL);
            opinionesStage.initOwner(closeButton.getScene().getWindow());
            opinionesStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la ventana de opiniones: " + e.getMessage());
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // Agregar método para cargar imagen por defecto
    private void loadFallbackImage() {
        try {
            // Intentar cargar la imagen desde diferentes rutas
            Image fallback = null;
            try {
                fallback = new Image(getClass().getResourceAsStream("/recurses/libro-abierto.png"));
            } catch (Exception e) {
                System.err.println("No se pudo cargar desde /recurses/: " + e.getMessage());
            }
            
            if (fallback == null || fallback.isError()) {
                try {
                    fallback = new Image(getClass().getResourceAsStream("/MiBiblioteca/recurses/libro-abierto.png"));
                } catch (Exception e) {
                    System.err.println("No se pudo cargar desde /MiBiblioteca/recurses/: " + e.getMessage());
                }
            }
            
            if (fallback == null || fallback.isError()) {
                try {
                    fallback = new Image("file:recurses/libro-abierto.png");
                } catch (Exception e) {
                    System.err.println("No se pudo cargar desde file:recurses/: " + e.getMessage());
                }
            }
            
            if (fallback != null && !fallback.isError()) {
                coverImageView.setImage(fallback);
            } else {
                System.err.println("No se pudo cargar ninguna imagen de fallback");
                coverImageView.setImage(null);
            }
        } catch (Exception ex) {
            System.err.println("Error general al cargar la imagen de fallback: " + ex.getMessage());
            coverImageView.setImage(null);
        }
    }
}
