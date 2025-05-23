package Controller;

import Api.GoogleBooksAPI;
import Model.Book;
import Model.UserService;
import Model.Usuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecomendacionesController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private TabPane tabPane;
    @FXML private Tab tabGenero;
    @FXML private Tab tabNuevos;
    @FXML private Tab tabPopulares;
    @FXML private ScrollPane scrollGenero;
    @FXML private ScrollPane scrollNuevos;
    @FXML private ScrollPane scrollPopulares;
    @FXML private GridPane gridGenero;
    @FXML private GridPane gridNuevos;
    @FXML private GridPane gridPopulares;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> comboGenero;
    
    private UserService userService = new UserService();
    private ObservableList<Book> librosGenero = FXCollections.observableArrayList();
    private ObservableList<Book> librosNuevos = FXCollections.observableArrayList();
    private ObservableList<Book> librosPopulares = FXCollections.observableArrayList();
    private String generoSeleccionado;
    
    @FXML
    void initialize() {
        if (userService.getCurrentUser() != null) {
            welcomeLabel.setText("Recomendaciones para " + userService.getCurrentUser().getNombre());
        }
        
        // Inicializar ComboBox de géneros
        initializeGenreComboBox();
        
        // Cargar recomendaciones
        cargarRecomendaciones();
    }
    
    /**
     * Establece el género para las recomendaciones y recarga los libros por género
     * @param genero El género a establecer
     */
    public void setGenero(String genero) {
        if (genero != null && !genero.isEmpty()) {
            // Cargar libros por el nuevo género
            cargarLibrosPorGenero(genero);
        
            // Actualizar el título de la pestaña
            tabGenero.setText("Por género: " + genero);
        }
    }

    /**
     * Establece el autor para las recomendaciones
     * @param autor El autor a establecer
     */
    public void setAutor(String autor) {
        if (autor != null && !autor.isEmpty()) {
            // Buscar libros del autor
            CompletableFuture.supplyAsync(() -> {
                try {
                    return GoogleBooksAPI.searchBooks("inauthor:" + autor);
                } catch (Exception e) {
                    return null;
                }
            }).thenAccept(result -> {
                Platform.runLater(() -> {
                    if (result != null && !result.isEmpty()) {
                        librosGenero.setAll(result);
                        displayBooks(librosGenero, gridGenero);
                        tabGenero.setText("Por autor: " + autor);
                        statusLabel.setText("");
                    } else {
                        statusLabel.setText("No se pudieron cargar libros del autor: " + autor);
                    }
                });
            });
        }
    }
    
    private void initializeGenreComboBox() {
        // Lista de géneros comunes
        ObservableList<String> generos = FXCollections.observableArrayList(
            "Ficción", "No ficción", "Fantasía", "Ciencia ficción", "Romance", 
            "Misterio", "Thriller", "Terror", "Biografía", "Historia", 
            "Autoayuda", "Negocios", "Infantil", "Juvenil", "Poesía"
        );
        
        comboGenero.setItems(generos);
        
        // Establecer el género favorito del usuario como predeterminado si está disponible
        if (userService.getCurrentUser() != null && userService.getCurrentUser().getGenero() != null) {
            generoSeleccionado = userService.getCurrentUser().getGenero();
            comboGenero.setValue(generoSeleccionado);
        } else {
            // Valor predeterminado si no hay género favorito
            generoSeleccionado = "Ficción";
            comboGenero.setValue(generoSeleccionado);
        }
        
        // Listener para cambios en el ComboBox
        comboGenero.setOnAction(event -> {
            setGenero(comboGenero.getValue());
        });
    }
    
    private void cargarRecomendaciones() {
        if (userService.getCurrentUser() == null) {
            statusLabel.setText("Debe iniciar sesión para ver recomendaciones personalizadas.");
            return;
        }
        
        // Obtener el género favorito del usuario o usar el seleccionado
        if (generoSeleccionado == null || generoSeleccionado.isEmpty()) {
            generoSeleccionado = userService.getCurrentUser().getGenero();
            if (generoSeleccionado == null || generoSeleccionado.isEmpty()) {
                generoSeleccionado = "Ficción";
            }
        }
        
        // Actualizar ComboBox
        comboGenero.setValue(generoSeleccionado);
        
        // Cargar libros por género
        cargarLibrosPorGenero(generoSeleccionado);
        
        // Cargar nuevas publicaciones (ordenadas por fecha de publicación)
        cargarNuevasPublicaciones();
        
        // Cargar libros populares (basados en valoraciones)
        cargarLibrosPopulares();
    }
    
    private void cargarLibrosPorGenero(String genero) {
        statusLabel.setText("Cargando recomendaciones de " + genero + "...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBooksByGenre(genero);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null && !result.isEmpty()) {
                    librosGenero.setAll(result);
                    displayBooks(librosGenero, gridGenero);
                    statusLabel.setText("");
                } else {
                    statusLabel.setText("No se pudieron cargar recomendaciones para el género: " + genero);
                }
            });
        });
    }
    
    private void cargarNuevasPublicaciones() {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Buscar libros recientes (ordenados por fecha)
                return GoogleBooksAPI.searchBooks("subject:fiction&orderBy=newest");
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null && !result.isEmpty()) {
                    librosNuevos.setAll(result);
                    displayBooks(librosNuevos, gridNuevos);
                }
            });
        });
    }
    
    private void cargarLibrosPopulares() {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Buscar libros populares
                return GoogleBooksAPI.searchBooks("subject:fiction&orderBy=relevance");
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null && !result.isEmpty()) {
                    librosPopulares.setAll(result);
                    displayBooks(librosPopulares, gridPopulares);
                }
            });
        });
    }
    
    private void displayBooks(List<Book> booksList, GridPane grid) {
        grid.getChildren().clear();

        int column = 0, row = 1;
        for (Book book : booksList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/BookCard.fxml"));
                VBox bookCard = loader.load();

                BookCardController controller = loader.getController();
                controller.setBook(book);
                controller.setOnViewDetailsAction(e -> viewBookDetails(book));

                grid.add(bookCard, column, row);
                column = (column + 1) % 5;
                if (column == 0) row++;

                // Agregar RowConstraints al grid cuando sea necesario
                while (grid.getRowConstraints().size() <= row) {
                    grid.getRowConstraints().add(new RowConstraints());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void viewBookDetails(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Detalles.fxml"));
            Parent detailView = loader.load();

            DetallesController controller = loader.getController();
            controller.setBook(book);

            Stage detailStage = new Stage();
            detailStage.setTitle(book.getTitle());
            detailStage.setScene(new Scene(detailView, 800, 600));
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(mainBorderPane.getScene().getWindow());
            detailStage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar los detalles del libro: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Método para cargar recomendaciones basadas en un libro específico
     * @param book El libro base para las recomendaciones
     */
    public void cargarRecomendacionesPorLibro(Book book) {
        if (book != null) {
            // Establecer título
            welcomeLabel.setText("Recomendaciones similares a: " + book.getTitle());
            
            // Obtener género del libro o autor para buscar similares
            String genero = book.getCategories().get(0);
            if (genero == null || genero.isEmpty()) {
                genero = "Ficción";
            }
            
            // Establecer género y cargar recomendaciones
            setGenero(genero);
            
            // También podemos buscar por autor
            buscarPorAutor(book.getAuthors().get(0));
        }
    }
    
    /**
     * Busca libros del mismo autor
     * @param autor El autor a buscar
     */
    private void buscarPorAutor(String autor) {
        if (autor != null && !autor.isEmpty()) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return GoogleBooksAPI.searchBooks("inauthor:" + autor);
                } catch (Exception e) {
                    return null;
                }
            }).thenAccept(result -> {
                Platform.runLater(() -> {
                    if (result != null && !result.isEmpty()) {
                        // Crear una nueva pestaña para mostrar libros del mismo autor
                        Tab tabAutor = new Tab("Por autor: " + autor);
                        ScrollPane scrollAutor = new ScrollPane();
                        GridPane gridAutor = new GridPane();
                        scrollAutor.setContent(gridAutor);
                        tabAutor.setContent(scrollAutor);
                        
                        // Agregar la pestaña al TabPane
                        tabPane.getTabs().add(tabAutor);
                        
                        // Mostrar los libros
                        displayBooks(result, gridAutor);
                    }
                });
            });
        }
    }
}
