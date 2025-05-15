package Controller;

import Api.GoogleBooksAPI;
import Model.Book;
import Model.BookCollection;
import Model.UserService;
import bd.DaoBook;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class PrincipalController {

    @FXML private BorderPane mainBorderPane;
    @FXML private VBox topBar;
    @FXML private HBox searchBar;
    @FXML private Label welcomeLabel;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private TabPane tabPane;
    @FXML private Tab searchTab;
    @FXML private Tab favoritesTab;
    @FXML private GridPane booksGrid;
    
    // Nuevos componentes para la vista de favoritos organizada por categorías
    @FXML private ScrollPane favoritesScrollPane;
    @FXML private VBox favoritesContainer;
    
    // Secciones para cada categoría
    @FXML private VBox seccionFavoritos;
    @FXML private VBox seccionLeidos;
    @FXML private VBox seccionLeyendo;
    @FXML private VBox seccionPendientes;
    @FXML private VBox seccionPrestados;
    @FXML private VBox seccionDeseados;
    
    // GridPanes para cada categoría
    @FXML private GridPane favoritesGrid;
    @FXML private GridPane leidosGrid;
    @FXML private GridPane leyendoGrid;
    @FXML private GridPane pendientesGrid;
    @FXML private GridPane prestadosGrid;
    @FXML private GridPane deseadosGrid;
    
    @FXML private Label statusLabel;
    @FXML private Button logoutButton;

    private UserService userService = new UserService();
    private ObservableList<Book> books = FXCollections.observableArrayList();
    private ObservableList<Book> favorites = FXCollections.observableArrayList();
    
    // Listas para cada categoría
    private ObservableList<Book> librosLeidos = FXCollections.observableArrayList();
    private ObservableList<Book> librosLeyendo = FXCollections.observableArrayList();
    private ObservableList<Book> librosPendientes = FXCollections.observableArrayList();
    private ObservableList<Book> librosPrestados = FXCollections.observableArrayList();
    private ObservableList<Book> librosDeseados = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        if (userService.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenido, " + userService.getCurrentUser().getNombre());
        }

        loadInitialBooks();

        Platform.runLater(() -> {
            tabPane.prefHeightProperty().bind(mainBorderPane.heightProperty().subtract(topBar.heightProperty()).subtract(20));
            booksGrid.prefWidthProperty().bind(((ScrollPane)((BorderPane)searchTab.getContent()).getCenter()).widthProperty().subtract(40));
        });
    }

    private void loadInitialBooks() {
        statusLabel.setText("Cargando libros...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBooks(userService.getCurrentUser().getGenero());
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null) {
                    books.setAll(result);
                    displayBooks(books);
                    statusLabel.setText("");
                } else {
                    statusLabel.setText("Error al cargar los libros iniciales");
                }
            });
        });
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Búsqueda vacía", "Por favor, ingrese un término de búsqueda.");
            return;
        }

        statusLabel.setText("Buscando libros...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBooks(query);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result != null) {
                    books.setAll(result);
                    displayBooks(books);
                    statusLabel.setText(result.isEmpty() ? "No se encontraron resultados para: " + query : "");
                } else {
                    statusLabel.setText("Error en la búsqueda");
                }
            });
        });
    }

    private void displayBooks(List<Book> booksList) {
        booksGrid.getChildren().clear();

        int column = 0, row = 1;
        for (Book book : booksList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/BookCard.fxml"));
                VBox bookCard = loader.load();

                BookCardController controller = loader.getController();
                controller.setBook(book);
                controller.setOnViewDetailsAction(e -> viewBookDetails(book));

                booksGrid.add(bookCard, column, row);
                column = (column + 1) % 5; // Cambiado de 6 a 5 para coincidir con el FXML
                if (column == 0) row++;

                // Agregar RowConstraints al booksGrid cuando sea necesario
                while (booksGrid.getRowConstraints().size() <= row) {
                    booksGrid.getRowConstraints().add(new RowConstraints());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método modificado para mostrar libros por categorías
    private void displayFavorites() {
        favoritesGrid.getChildren().clear();
        leidosGrid.getChildren().clear();
        leyendoGrid.getChildren().clear();
        pendientesGrid.getChildren().clear();
        prestadosGrid.getChildren().clear();
        deseadosGrid.getChildren().clear();
        
        favorites.clear();
        librosLeidos.clear();
        librosLeyendo.clear();
        librosPendientes.clear();
        librosPrestados.clear();
        librosDeseados.clear();

        if (userService.getCurrentUser() == null) {
            statusLabel.setText("Debe iniciar sesión para ver favoritos");
            return;
        }

        int userId = userService.getCurrentUser().getId();
        statusLabel.setText("Cargando libros...");

        CompletableFuture.supplyAsync(() -> {
            return DaoBook.getLibroIDsFavoritosPorUsuario(userId);
        }).thenCompose(favoriteIDs -> {
            List<CompletableFuture<Book>> futures = new ArrayList<>();

            for (String id : favoriteIDs) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    return GoogleBooksAPI.searchBookById(id);
                }));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(book -> book != null)
                            .collect(Collectors.toList()));
        }).thenAccept(favoriteBooks -> {
            favorites.addAll(favoriteBooks);
            
            cargarLibrosPorEstado(userId, "leído", librosLeidos);
            cargarLibrosPorEstado(userId, "leyendo", librosLeyendo);
            cargarLibrosPorEstado(userId, "pendiente", librosPendientes);
            cargarLibrosPorEstado(userId, "prestado", librosPrestados);
            cargarLibrosPorEstado(userId, "deseado", librosDeseados);
            
            Platform.runLater(() -> {
                displayBooksInGrid(favorites, favoritesGrid);
                
                actualizarVisibilidadSecciones();
                
                statusLabel.setText("");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                ex.printStackTrace();
                statusLabel.setText("Error al cargar libros: " + ex.getMessage());
            });
            return null;
        });
    }
    
    // Método para cargar libros por estado
    private void cargarLibrosPorEstado(int userId, String estado, ObservableList<Book> lista) {
        CompletableFuture.supplyAsync(() -> {
            List<BookCollection> librosCollection = DaoBook.getLibrosPorEstado(userId, estado);
            List<Book> libros = new ArrayList<>();
            
            for (BookCollection bc : librosCollection) {
                libros.add(bc.getBook());
            }
            
            return libros;
        }).thenAccept(libros -> {
            Platform.runLater(() -> {
                lista.addAll(libros);
                
                switch (estado) {
                    case "leído":
                        displayBooksInGrid(lista, leidosGrid);
                        break;
                    case "leyendo":
                        displayBooksInGrid(lista, leyendoGrid);
                        break;
                    case "pendiente":
                        displayBooksInGrid(lista, pendientesGrid);
                        break;
                    case "prestado":
                        displayBooksInGrid(lista, prestadosGrid);
                        break;
                    case "deseado":
                        displayBooksInGrid(lista, deseadosGrid);
                        break;
                }
                
                actualizarVisibilidadSecciones();
            });
        });
    }
    
    // Método para mostrar libros en una grid específica
    private void displayBooksInGrid(List<Book> booksList, GridPane grid) {
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

                while (grid.getRowConstraints().size() <= row) {
                    grid.getRowConstraints().add(new RowConstraints());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Método para actualizar la visibilidad de las secciones
    private void actualizarVisibilidadSecciones() {
        seccionFavoritos.setVisible(!favorites.isEmpty());
        seccionFavoritos.setManaged(!favorites.isEmpty());
        
        seccionLeidos.setVisible(!librosLeidos.isEmpty());
        seccionLeidos.setManaged(!librosLeidos.isEmpty());
        
        seccionLeyendo.setVisible(!librosLeyendo.isEmpty());
        seccionLeyendo.setManaged(!librosLeyendo.isEmpty());
        
        seccionPendientes.setVisible(!librosPendientes.isEmpty());
        seccionPendientes.setManaged(!librosPendientes.isEmpty());
        
        seccionPrestados.setVisible(!librosPrestados.isEmpty());
        seccionPrestados.setManaged(!librosPrestados.isEmpty());
        
        seccionDeseados.setVisible(!librosDeseados.isEmpty());
        seccionDeseados.setManaged(!librosDeseados.isEmpty());
        
        // Actualizar mensaje de estado
        if (favorites.isEmpty() && librosLeidos.isEmpty() && librosLeyendo.isEmpty() && 
            librosPendientes.isEmpty() && librosPrestados.isEmpty() && librosDeseados.isEmpty()) {
            statusLabel.setText("No tienes libros guardados en tu colección.");
        } else {
            statusLabel.setText("");
        }
    }

    private void viewBookDetails(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Detalles.fxml"));
            Parent detailView = loader.load();

            DetallesController controller = loader.getController();
            controller.setBook(book);
            controller.setFavorites(favorites);
            controller.setOnFavoriteChangedCallback(this::handleFavoriteChanged);

            Stage detailStage = new Stage();
            detailStage.setTitle(book.getTitle());
            detailStage.setScene(new Scene(detailView, 800, 600));
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(mainBorderPane.getScene().getWindow());
            detailStage.showAndWait();
            
            // Recargar favoritos después de cerrar la ventana de detalles
            if (tabPane.getSelectionModel().getSelectedItem() == favoritesTab) {
                displayFavorites();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar los detalles del libro: " + e.getMessage());
        }
    }

    private void handleFavoriteChanged() {
        if (tabPane.getSelectionModel().getSelectedItem() == favoritesTab) {
            displayFavorites();
        }
    }
    
    @FXML
    void handleVerColeccion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Coleccion.fxml"));
            Parent coleccionView = loader.load();
            
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.setScene(new Scene(coleccionView));
            stage.setTitle("Mi Colección");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la vista de colección: " + e.getMessage());
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        userService.logout();
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/View/Login.fxml"));
            Scene loginScene = new Scene(loginView);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.setTitle("Biblioteca Digital - Inicio de Sesión");
            window.setMaximized(false);
            window.setWidth(600);
            window.setHeight(400);
            window.centerOnScreen();
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la pantalla de inicio de sesión: " + e.getMessage());
        }
    }

    @FXML
    void handleTabChange() {
        if (tabPane.getSelectionModel().getSelectedItem() == favoritesTab) {
            displayFavorites();
        } else {
            displayBooks(books);
            statusLabel.setText(books.isEmpty() ? "No hay resultados de búsqueda." : "");
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
