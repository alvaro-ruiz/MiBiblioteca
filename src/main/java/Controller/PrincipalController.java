package Controller;

import Api.GoogleBooksAPI;
import Model.Book;
import Model.BookCollection;
import Model.Opinion;
import Model.Usuario;
import Model.UserService;
import bd.DaoBook;
import bd.DaoOpinion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PrincipalController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(PrincipalController.class.getName());
    private static final int MAX_OPINIONES_PREVIEW = 3; // Número máximo de opiniones a mostrar en la vista previa
    private static final int MAX_RECOMENDACIONES = 4; // Número máximo de recomendaciones a mostrar

    @FXML private BorderPane mainBorderPane;
    @FXML private VBox topBar;
    @FXML private Label welcomeLabel;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private BorderPane searchPane;
    @FXML private BorderPane favoritesPane;
    @FXML private BorderPane addBookPane;
    @FXML private BorderPane detailsPane;
    @FXML private GridPane booksGrid;
    @FXML private ScrollPane favoritesScrollPane;
    @FXML private VBox favoritesContainer;
    @FXML private VBox seccionFavoritos;
    @FXML private VBox seccionLeidos;
    @FXML private VBox seccionLeyendo;
    @FXML private VBox seccionPendientes;
    @FXML private VBox seccionPrestados;
    @FXML private VBox seccionDeseados;
    @FXML private GridPane favoritesGrid;
    @FXML private GridPane leidosGrid;
    @FXML private GridPane leyendoGrid;
    @FXML private GridPane pendientesGrid;
    @FXML private GridPane prestadosGrid;
    @FXML private GridPane deseadosGrid;
    @FXML private Label statusLabel;
    @FXML private Label statusLabelFavorites;
    @FXML private Label statusLabelAddBook;
    @FXML private Label statusLabelDetails;
    @FXML private Button logoutButton;
    @FXML private Button btnAgregarManual;
    @FXML private Button btnRecomendaciones;
    @FXML private ProgressIndicator searchProgressIndicator;
    @FXML private Button btnBusqueda;
    @FXML private Button btnColeccion;
    @FXML private Button btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnVolverDetalles;
    
    // Campos para añadir libro
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private TextField txtEditorial;
    @FXML private TextField txtIsbn;
    @FXML private DatePicker fechaPublicacion;
    @FXML private ComboBox<String> comboGenero;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> comboEstado;
    
    // Campos para detalles del libro
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label publisherLabel;
    @FXML private Label publishedDateLabel;
    @FXML private Label pageCountLabel;
    @FXML private Label isbnLabel;
    @FXML private FlowPane categoriesPane;
    @FXML private TextArea descriptionTextArea;
    @FXML private Button favoriteButton;
    @FXML private Hyperlink previewLink;
    @FXML private Button btnVerOpiniones;
    
    // Controles para el estado del libro
    @FXML private ComboBox<String> estadoComboBox;
    @FXML private Button guardarEstadoButton;
    
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
    
    // Nuevos controles para opiniones y recomendaciones
    @FXML private VBox opinionesContainer;
    @FXML private VBox opinionesList;
    @FXML private Label noOpinionesLabel;
    @FXML private Button btnAgregarOpinion;
    @FXML private Button btnVerMasOpiniones;
    
    @FXML private VBox recomendacionesContainer;
    @FXML private HBox recomendacionesHBox;
    @FXML private Label noRecomendacionesLabel;
    @FXML private ProgressIndicator recomendacionesProgress;
    @FXML private Button btnVerMasRecomendaciones;

    private Usuario usuarioActual;
    private UserService userService = new UserService();
    private ObservableList<Book> books = FXCollections.observableArrayList();
    private ObservableList<Book> favorites = FXCollections.observableArrayList();
    
    // Libro actual en detalles
    private Book currentBook;
    private boolean isFavorite = false;
    
    // Listas para cada categoría
    private ObservableList<Book> librosLeidos = FXCollections.observableArrayList();
    private ObservableList<Book> librosLeyendo = FXCollections.observableArrayList();
    private ObservableList<Book> librosPendientes = FXCollections.observableArrayList();
    private ObservableList<Book> librosPrestados = FXCollections.observableArrayList();
    private ObservableList<Book> librosDeseados = FXCollections.observableArrayList();
    
    // Cache para evitar múltiples búsquedas del mismo libro
    private final java.util.Map<String, Book> bookCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Formato para fechas
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (userService.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenido, " + userService.getCurrentUser().getNombre());
        }
        
        // Ocultar indicador de progreso inicialmente
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisible(false);
        }

        // Configurar búsqueda al presionar Enter en el campo de búsqueda
        searchField.setOnAction(event -> handleSearch(event));

        // Mostrar panel de búsqueda por defecto
        showSearchPane();
        
        // Inicializar componentes de añadir libro
        initializeAddBookComponents();
        
        // Inicializar componentes de detalles
        initializeDetailsComponents();
        
        loadInitialBooks();

        Platform.runLater(() -> {
            booksGrid.prefWidthProperty().bind(searchPane.widthProperty().subtract(40));
        });
    }
    
    private void initializeAddBookComponents() {
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
    
    private void initializeDetailsComponents() {
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
        
        // Inicializar botones de opiniones y recomendaciones
        btnVerMasOpiniones.setDisable(true);
        btnVerMasRecomendaciones.setDisable(true);
    }

    public void setUsuario(Usuario usuario) {
        this.usuarioActual = usuario;
        welcomeLabel.setText("Bienvenido, " + usuario.getNombre());
    }

    private void loadInitialBooks() {
        updateStatus("Cargando libros...");
        
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisible(true);
        }

        Task<List<Book>> task = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                Usuario user = userService.getCurrentUser();
                if (user != null && user.getGenero() != null && !user.getGenero().isEmpty()) {
                    return GoogleBooksAPI.searchBooks(user.getGenero());
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(event -> {
            List<Book> result = task.getValue();
            Platform.runLater(() -> {
                books.setAll(result);
                displayBooks(books);
                updateStatus("");
                
                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisible(false);
                }
            });
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            LOGGER.log(Level.SEVERE, "Error al cargar libros iniciales", exception);
            Platform.runLater(() -> {
                updateStatus("Error al cargar los libros iniciales: " + exception.getMessage());
                
                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisible(false);
                }
            });
        });

        new Thread(task).start();
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Búsqueda vacía", "Por favor, ingrese un término de búsqueda.");
            return;
        }

        updateStatus("Buscando libros...");
        
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisible(true);
        }
        
        // Deshabilitar botón de búsqueda durante la operación
        searchButton.setDisable(true);
        
        // Asegurarse de que el panel de búsqueda esté visible
        showSearchPane();
        
        // Paso 1: Buscar en la base de datos local
        List<Book> localResults = DaoBook.buscarLibrosLocales(query);
        
        // Si encontramos resultados locales, mostrarlos inmediatamente
        if (!localResults.isEmpty()) {
            Platform.runLater(() -> {
                books.setAll(localResults);
                displayBooks(books);
                updateStatus("Mostrando " + localResults.size() + " resultados de la base de datos local.");
                
                // Ocultar indicador de progreso
                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisible(false);
                }
                
                // Habilitar botón de búsqueda
                searchButton.setDisable(false);
            });
            return;
        }
        
        // Si no hay resultados locales, buscar en la API
        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                return GoogleBooksAPI.searchBooks(query);
            }
        };
        
        // Manejar el resultado de la búsqueda
        searchTask.setOnSucceeded(e -> {
            List<Book> result = searchTask.getValue();
            Platform.runLater(() -> {
                books.setAll(result);
                displayBooks(books);
                updateStatus(result.isEmpty() ? 
                        "No se encontraron resultados para: " + query : 
                        "Mostrando " + result.size() + " resultados de la API de Google Books.");
                
                // Ocultar indicador de progreso
                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisible(false);
                }
                
                // Habilitar botón de búsqueda
                searchButton.setDisable(false);
            });
        });
        
        // Manejar errores
        searchTask.setOnFailed(e -> {
            Throwable exception = searchTask.getException();
            LOGGER.log(Level.SEVERE, "Error en la búsqueda", exception);
            
            Platform.runLater(() -> {
                updateStatus("Error en la búsqueda: " + exception.getMessage());
                
                // Ocultar indicador de progreso
                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisible(false);
                }
                
                // Habilitar botón de búsqueda
                searchButton.setDisable(false);
            });
        });
        
        // Iniciar la tarea en un hilo separado
        new Thread(searchTask).start();
    }

    @FXML
    void handleShowSearchTab(ActionEvent event) {
        showSearchPane();
        displayBooks(books);
        updateStatus(books.isEmpty() ? "No hay resultados de búsqueda." : "");
        
        // Resaltar el botón activo
        btnBusqueda.setStyle("-fx-background-color: #1c6399;"); // Azul más oscuro
        btnColeccion.setStyle("-fx-background-color: #27ae60;"); // Verde normal
        btnAgregarManual.setStyle("-fx-background-color: #e67e22;"); // Naranja normal
    }

    @FXML
    void handleShowFavoritesTab(ActionEvent event) {
        showFavoritesPane();
        displayFavorites();
        
        // Resaltar el botón activo
        btnBusqueda.setStyle("-fx-background-color: #2980b9;"); // Azul normal
        btnColeccion.setStyle("-fx-background-color: #1e8449;"); // Verde más oscuro
        btnAgregarManual.setStyle("-fx-background-color: #e67e22;"); // Naranja normal
    }
    
    @FXML
    void handleShowAddBookTab(ActionEvent event) {
        showAddBookPane();
        
        // Resaltar el botón activo
        btnBusqueda.setStyle("-fx-background-color: #2980b9;"); // Azul normal
        btnColeccion.setStyle("-fx-background-color: #27ae60;"); // Verde normal
        btnAgregarManual.setStyle("-fx-background-color: #d35400;"); // Naranja más oscuro
    }
    
    @FXML
    void handleVolverDetalles(ActionEvent event) {
        // Volver a la pantalla anterior
        if (searchPane.isVisible()) {
            showSearchPane();
        } else if (favoritesPane.isVisible()) {
            showFavoritesPane();
            displayFavorites(); // Recargar favoritos
        } else {
            showSearchPane(); // Por defecto, volver a búsqueda
        }
    }
    
    private void showSearchPane() {
        searchPane.setVisible(true);
        favoritesPane.setVisible(false);
        addBookPane.setVisible(false);
        detailsPane.setVisible(false);
    }
    
    private void showFavoritesPane() {
        searchPane.setVisible(false);
        favoritesPane.setVisible(true);
        addBookPane.setVisible(false);
        detailsPane.setVisible(false);
    }
    
    private void showAddBookPane() {
        searchPane.setVisible(false);
        favoritesPane.setVisible(false);
        addBookPane.setVisible(true);
        detailsPane.setVisible(false);
    }
    
    private void showDetailsPane() {
        searchPane.setVisible(false);
        favoritesPane.setVisible(false);
        addBookPane.setVisible(false);
        detailsPane.setVisible(true);
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
                column = (column + 1) % 5;
                if (column == 0) row++;

                // Agregar RowConstraints al booksGrid cuando sea necesario
                while (booksGrid.getRowConstraints().size() <= row) {
                    booksGrid.getRowConstraints().add(new RowConstraints());
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error al mostrar libro", e);
            }
        }
    }

    private void displayFavorites() {
        clearAllGrids();
        clearAllLists();

        if (userService.getCurrentUser() == null) {
            updateStatusFavorites("Debe iniciar sesión para ver favoritos");
            return;
        }

        int userId = userService.getCurrentUser().getId();
        updateStatusFavorites("Cargando libros...");
        
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisible(true);
        }

        // Cargar favoritos
        CompletableFuture.supplyAsync(() -> DaoBook.getLibroIDsFavoritosPorUsuario(userId))
            .thenCompose(favoriteIDs -> {
                List<CompletableFuture<Book>> futures = new ArrayList<>();

                for (String id : favoriteIDs) {
                    // Verificar si el libro está en caché
                    Book cachedBook = bookCache.get(id);
                    if (cachedBook != null) {
                        CompletableFuture<Book> future = new CompletableFuture<>();
                        future.complete(cachedBook);
                        futures.add(future);
                    } else {
                        // Si no está en caché, buscarlo en la API
                        futures.add(GoogleBooksAPI.searchBookByIdAsync(id).thenApply(book -> {
                            if (book != null) {
                                bookCache.put(id, book);
                            }
                            return book;
                        }));
                    }
                }

                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> futures.stream()
                                .map(CompletableFuture::join)
                                .filter(book -> book != null)
                                .collect(Collectors.toList()));
            })
            .thenAccept(favoriteBooks -> {
                favorites.addAll(favoriteBooks);
                
                // Cargar libros por estado
                loadBooksByState(userId);
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Error al cargar favoritos", ex);
                Platform.runLater(() -> {
                    updateStatusFavorites("Error al cargar libros: " + ex.getMessage());
                    
                    if (searchProgressIndicator != null) {
                        searchProgressIndicator.setVisible(false);
                    }
                });
                return null;
            });
    }
    
    private void loadBooksByState(int userId) {
        // Crear una lista de futuros para todas las categorías
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Cargar libros leídos
        futures.add(loadBooksForState(userId, "leído", librosLeidos, leidosGrid));
        
        // Cargar libros leyendo
        futures.add(loadBooksForState(userId, "leyendo", librosLeyendo, leyendoGrid));
        
        // Cargar libros pendientes
        futures.add(loadBooksForState(userId, "pendiente", librosPendientes, pendientesGrid));
        
        // Cargar libros prestados
        futures.add(loadBooksForState(userId, "prestado", librosPrestados, prestadosGrid));
        
        // Cargar libros deseados
        futures.add(loadBooksForState(userId, "deseado", librosDeseados, deseadosGrid));
        
        // Cuando todos los futuros se completen
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                Platform.runLater(() -> {
                    // Mostrar libros favoritos
                    displayBooksInGrid(favorites, favoritesGrid);
                    
                    // Actualizar visibilidad de secciones
                    actualizarVisibilidadSecciones();
                    
                    // Actualizar estado
                    updateStatusFavorites("");
                    
                    if (searchProgressIndicator != null) {
                        searchProgressIndicator.setVisible(false);
                    }
                });
            });
    }
    
    private CompletableFuture<Void> loadBooksForState(int userId, String estado, ObservableList<Book> lista, GridPane grid) {
        return CompletableFuture.supplyAsync(() -> DaoBook.getLibrosPorEstado(userId, estado))
            .thenAccept(librosCollection -> {
                List<Book> libros = librosCollection.stream()
                    .map(BookCollection::getBook)
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    lista.addAll(libros);
                    displayBooksInGrid(lista, grid);
                });
            });
    }
    
    private void clearAllGrids() {
        favoritesGrid.getChildren().clear();
        leidosGrid.getChildren().clear();
        leyendoGrid.getChildren().clear();
        pendientesGrid.getChildren().clear();
        prestadosGrid.getChildren().clear();
        deseadosGrid.getChildren().clear();
    }
    
    private void clearAllLists() {
        favorites.clear();
        librosLeidos.clear();
        librosLeyendo.clear();
        librosPendientes.clear();
        librosPrestados.clear();
        librosDeseados.clear();
    }
    
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
                LOGGER.log(Level.SEVERE, "Error al mostrar libro en grid", e);
            }
        }
    }
    
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
            updateStatusFavorites("No tienes libros guardados en tu colección.");
        } else {
            updateStatusFavorites("");
        }
    }

    private void viewBookDetails(Book book) {
        // Guardar referencia al libro actual
        this.currentBook = book;
        
        // Mostrar panel de detalles
        showDetailsPane();
        
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
                
                Image image = new Image(imageUrl, true);
                
                // Agregar un listener para manejar errores de carga
                image.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        loadFallbackImage();
                    }
                });
                
                coverImageView.setImage(image);
            } catch (Exception e) {
                loadFallbackImage();
            }
        } else {
            loadFallbackImage();
        }
        
        // Verificar si el libro ya está en favoritos
        if (userService.getCurrentUser() != null) {
            try {
                isFavorite = DaoBook.isFavorite(userService.getCurrentUser().getId(), book.getId());
                favoriteButton.setText(isFavorite ? "Quitar de favoritos" : "Añadir a favoritos");
                
                // Cargar el estado actual del libro
                cargarEstadoLibro();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Cargar detalles completos del libro
        loadBookDetails();
        
        // Cargar opiniones
        loadOpiniones();
        
        // Cargar recomendaciones
        loadRecomendaciones();
    }
    
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
    
    private void cargarEstadoLibro() {
        try {
            String estado = DaoBook.getEstadoLibro(userService.getCurrentUser().getId(), currentBook.getId());
            if (estado != null && !estado.isEmpty()) {
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
            updateStatusDetails("Error: No se pudo cargar el estado del libro");
        }
    }
    
    private void cargarInformacionLectura() {
        try {
            // Obtener información de lectura
            Object[] infoLectura = DaoBook.getInformacionLectura(userService.getCurrentUser().getId(), currentBook.getId());
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
            updateStatusDetails("Error: No se pudo cargar la información de lectura");
        }
    }
    
    private void cargarInformacionPrestamo() {
        try {
            // Obtener información de préstamo
            Object[] infoPrestamo = DaoBook.getInformacionPrestamo(userService.getCurrentUser().getId(), currentBook.getId());
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
            updateStatusDetails("Error: No se pudo cargar la información de préstamo");
        }
    }
    
    private void actualizarSeccionesVisibles(String estado) {
        // Ocultar todas las secciones primero
        seccionLeido.setVisible(false);
        seccionLeido.setManaged(false);
        seccionPrestado.setVisible(false);
        seccionPrestado.setManaged(false);
        
        // Mostrar la sección correspondiente según el estado seleccionado
        switch (estado) {
            case "leído":
                seccionLeido.setVisible(true);
                seccionLeido.setManaged(true);
                break;
            case "prestado":
                seccionPrestado.setVisible(true);
                seccionPrestado.setManaged(true);
                break;
        }
    }
    
    private void loadBookDetails() {
        updateStatusDetails("Cargando detalles del libro...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBookById(currentBook.getId());
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(detailedBook -> {
            Platform.runLater(() -> {
                if (detailedBook != null) {
                    updateBookDetails(detailedBook);
                }
                updateStatusDetails("");
            });
        });
    }
    
    private void updateBookDetails(Book detailedBook) {
        // Actualizar el libro con los detalles completos
        this.currentBook = detailedBook;
        
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
            // Eliminar posibles etiquetas HTML de la descripción
            String cleanDescription = detailedBook.getDescription().replaceAll("<[^>]*>", "");
            descriptionTextArea.setText(cleanDescription);
        } else {
            descriptionTextArea.setText("No hay descripción disponible.");
        }
        
        // Configurar enlace de vista previa
        if (detailedBook.getPreviewLink() != null && !detailedBook.getPreviewLink().isEmpty()) {
            previewLink.setVisible(true);
        } else {
            previewLink.setVisible(false);
        }
    }
    
    private void loadOpiniones() {
        // Limpiar la lista de opiniones
        opinionesList.getChildren().clear();
        opinionesList.getChildren().add(noOpinionesLabel);
        
        // Verificar si hay un libro actual
        if (currentBook == null) {
            return;
        }
        
        // Cargar opiniones desde la base de datos
        CompletableFuture.supplyAsync(() -> DaoOpinion.getOpinionesPorLibro(currentBook.getId()))
            .thenAccept(opiniones -> {
                Platform.runLater(() -> {
                    if (opiniones != null && !opiniones.isEmpty()) {
                        // Mostrar solo las primeras opiniones (limitado por MAX_OPINIONES_PREVIEW)
                        opinionesList.getChildren().clear();
                        
                        int count = Math.min(opiniones.size(), MAX_OPINIONES_PREVIEW);
                        for (int i = 0; i < count; i++) {
                            Opinion opinion = opiniones.get(i);
                            opinionesList.getChildren().add(createOpinionNode(opinion));
                        }
                        
                        // Habilitar el botón "Ver más" si hay más opiniones
                        btnVerMasOpiniones.setDisable(opiniones.size() <= MAX_OPINIONES_PREVIEW);
                    }
                });
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Error al cargar opiniones", ex);
                return null;
            });
    }
    
    private Node createOpinionNode(Opinion opinion) {
        VBox opinionBox = new VBox(5);
        opinionBox.getStyleClass().add("opinion-item");
        
        // Nombre de usuario y fecha
        HBox headerBox = new HBox(10);
        
        Label usuarioLabel = new Label(opinion.getNombreUsuario());
        usuarioLabel.getStyleClass().add("opinion-usuario");
        
        Label fechaLabel = new Label(opinion.getFecha().format(dateFormatter));
        fechaLabel.getStyleClass().add("opinion-fecha");
        
        headerBox.getChildren().addAll(usuarioLabel, fechaLabel);
        
        // Valoración
        Label valoracionLabel = new Label(String.format("Valoración: %.1f/10", opinion.getNota()));
        valoracionLabel.getStyleClass().add("opinion-valoracion");
        
        // Comentario
        Label comentarioLabel = new Label(opinion.getComentario());
        comentarioLabel.getStyleClass().add("opinion-comentario");
        comentarioLabel.setWrapText(true);
        
        opinionBox.getChildren().addAll(headerBox, valoracionLabel, comentarioLabel);
        
        return opinionBox;
    }
    
    private void loadRecomendaciones() {
        // Limpiar el contenedor de recomendaciones
        recomendacionesHBox.getChildren().clear();
        recomendacionesHBox.getChildren().addAll(recomendacionesProgress, noRecomendacionesLabel);
        
        // Verificar si hay un libro actual
        if (currentBook == null) {
            return;
        }
        
        // Obtener categorías o autor para buscar libros similares
        String searchQuery = "";
        if (currentBook.getCategories() != null && !currentBook.getCategories().isEmpty()) {
            searchQuery = "subject:" + currentBook.getCategories().get(0);
        } else if (currentBook.getAuthors() != null && !currentBook.getAuthors().isEmpty()) {
            searchQuery = "inauthor:" + currentBook.getAuthors().get(0);
        } else {
            // Si no hay categorías ni autores, usar el título
            searchQuery = "intitle:" + currentBook.getTitle().split(" ")[0]; // Usar la primera palabra del título
        }
        
        // Buscar libros similares
        final String query = searchQuery;
        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBooks(query);
            } catch (Exception e) {
                return new ArrayList<Book>();
            }
        }).thenAccept(recomendaciones -> {
            Platform.runLater(() -> {
                recomendacionesHBox.getChildren().clear();
            
                // Filtrar el libro actual de las recomendaciones
                List<Book> filteredRecomendaciones = recomendaciones.stream()
                    .filter(book -> !book.getId().equals(currentBook.getId()))
                    .limit(MAX_RECOMENDACIONES)
                    .collect(Collectors.toList());
            
                if (filteredRecomendaciones.isEmpty()) {
                    noRecomendacionesLabel.setText("No se encontraron recomendaciones similares.");
                    recomendacionesHBox.getChildren().add(noRecomendacionesLabel);
                } else {
                    // Mostrar las recomendaciones
                    for (Book book : filteredRecomendaciones) {
                        recomendacionesHBox.getChildren().add(createRecomendacionNode(book));
                    }
                
                    // Habilitar el botón "Ver más" si hay suficientes recomendaciones
                    btnVerMasRecomendaciones.setDisable(filteredRecomendaciones.size() < MAX_RECOMENDACIONES);
                }
            });
        })
        .exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error al cargar recomendaciones", ex);
            Platform.runLater(() -> {
                recomendacionesHBox.getChildren().clear();
                noRecomendacionesLabel.setText("Error al cargar recomendaciones.");
                recomendacionesHBox.getChildren().add(noRecomendacionesLabel);
            });
            return null;
        });
    }
    
    private Node createRecomendacionNode(Book book) {
        VBox bookBox = new VBox(5);
        bookBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        bookBox.setPrefWidth(120);
        bookBox.setMaxWidth(120);
        bookBox.getStyleClass().add("book-card");
    
        // Portada
        ImageView coverView = new ImageView();
        coverView.setFitHeight(160);
        coverView.setFitWidth(100);
        coverView.setPreserveRatio(true);
    
        // Cargar imagen
        if (book.getThumbnail() != null && !book.getThumbnail().isEmpty()) {
            String imageUrl = book.getThumbnail();
            if (imageUrl.startsWith("http:")) {
                imageUrl = imageUrl.replace("http:", "https:");
            }
        
            Image image = new Image(imageUrl, true);
            coverView.setImage(image);
        } else {
            // Imagen por defecto
            try {
                Image fallback = new Image(getClass().getResourceAsStream("/recurses/libro-abierto.png"));
                coverView.setImage(fallback);
            } catch (Exception e) {
                // No hacer nada si no se puede cargar la imagen
            }
        }
    
        // Título
        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    
        // Autor
        String authorText = book.getAuthors() != null && !book.getAuthors().isEmpty() 
                ? String.join(", ", book.getAuthors()) 
                : "Autor desconocido";
        Label authorLabel = new Label(authorText);
        authorLabel.getStyleClass().add("book-author");
        authorLabel.setWrapText(true);
        authorLabel.setAlignment(javafx.geometry.Pos.CENTER);
        authorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    
        // Botón para ver detalles
        Button verButton = new Button("Ver detalles");
        verButton.setOnAction(e -> viewBookDetails(book));
    
        bookBox.getChildren().addAll(coverView, titleLabel, authorLabel, verButton);
    
        return bookBox;
    }
    
    @FXML
    void handlePreviewLink(ActionEvent event) {
        if (currentBook != null && currentBook.getPreviewLink() != null && !currentBook.getPreviewLink().isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI(currentBook.getPreviewLink()));
            } catch (Exception ex) {
                updateStatusDetails("Error al abrir el enlace de vista previa");
            }
        }
    }

    @FXML
    void handleToggleFavorite(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para agregar favoritos");
            return;
        }
        
        try {
            isFavorite = !isFavorite;

            if (isFavorite) {
                favoriteButton.setText("Quitar de favoritos");
                if (DaoBook.guardarFavorito(userService.getCurrentUser().getId(), currentBook)) {
                    if (!favorites.stream().anyMatch(b -> b.getId().equals(currentBook.getId()))) {
                        favorites.add(currentBook);
                    }
                    updateStatusDetails("Libro agregado a favoritos");
                } else {
                    isFavorite = false;
                    favoriteButton.setText("Añadir a favoritos");
                    updateStatusDetails("Error: No se pudo agregar el libro a favoritos");
                }
            } else {
                favoriteButton.setText("Añadir a favoritos");
                if (DaoBook.removeBookFromFavorites(userService.getCurrentUser().getId(), currentBook.getId())) {
                    favorites.removeIf(b -> b.getId().equals(currentBook.getId()));
                    updateStatusDetails("Libro eliminado de favoritos");
                } else {
                    isFavorite = true;
                    favoriteButton.setText("Quitar de favoritos");
                    updateStatusDetails("Error: No se pudo eliminar el libro de favoritos");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusDetails("Error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGuardarEstado(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para cambiar el estado del libro");
            return;
        }
        
        String nuevoEstado = estadoComboBox.getValue();
        
        try {
            if (DaoBook.guardarEstadoLibro(userService.getCurrentUser().getId(), currentBook, nuevoEstado)) {
                String mensaje = "Estado del libro actualizado a: " + nuevoEstado;
                
                // Añadir información adicional según el estado
                if ("leído".equals(nuevoEstado)) {
                    mensaje += "\nAhora puede añadir su valoración y comentarios en la sección correspondiente.";
                } else if ("prestado".equals(nuevoEstado)) {
                    mensaje += "\nNo olvide registrar a quién prestó el libro en la sección correspondiente.";
                }
                
                updateStatusDetails(mensaje);
                actualizarSeccionesVisibles(nuevoEstado);
            } else {
                updateStatusDetails("Error: No se pudo actualizar el estado del libro");
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusDetails("Error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGuardarLectura(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Debe iniciar sesión para guardar información de lectura");
            return;
        }
        
        LocalDate fechaLectura = fechaLecturaPicker.getValue();
        double nota = valoracionSlider.getValue();
        String comentario = comentarioTextArea.getText();
        
        try {
            if (DaoBook.guardarInformacionLectura(userService.getCurrentUser().getId(), currentBook, fechaLectura, nota, comentario)) {
                updateStatusDetails("Información de lectura guardada correctamente");
            } else {
                updateStatusDetails("Error: No se pudo guardar la información de lectura");
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusDetails("Error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGuardarPrestamo(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
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
            if (DaoBook.guardarInformacionPrestamo(userService.getCurrentUser().getId(), currentBook, prestadoA, fechaPrestamo, devuelto)) {
                updateStatusDetails("Información de préstamo guardada correctamente");
            } else {
                updateStatusDetails("Error: No se pudo guardar la información de préstamo");
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusDetails("Error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleVerOpiniones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Opiniones.fxml"));
            Parent opinionesView = loader.load();
            
            OpinionesController controller = loader.getController();
            controller.setBook(currentBook);
            
            Stage opinionesStage = new Stage();
            opinionesStage.setTitle("Opiniones sobre " + currentBook.getTitle());
            opinionesStage.setScene(new Scene(opinionesView, 600, 500));
            opinionesStage.initModality(Modality.WINDOW_MODAL);
            opinionesStage.initOwner(mainBorderPane.getScene().getWindow());
            opinionesStage.showAndWait();
            
            // Recargar opiniones después de cerrar la ventana
            loadOpiniones();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatusDetails("Error: No se pudo cargar la ventana de opiniones");
        }
    }
    
    @FXML
    void handleAgregarOpinion(ActionEvent event) {
        if (userService.getCurrentUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Advertencia", "Debe iniciar sesión para publicar opiniones.");
            return;
        }
        
        try {
            // Crear un diálogo para agregar opinión
            Dialog<Opinion> dialog = new Dialog<>();
            dialog.setTitle("Añadir opinión");
            dialog.setHeaderText("Comparte tu opinión sobre \"" + currentBook.getTitle() + "\"");
            
            // Botones
            ButtonType buttonTypeOk = new ButtonType("Publicar", ButtonBar.ButtonData.OK_DONE);
            ButtonType buttonTypeCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);
            
            // Crear el contenido del diálogo
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            
            // Valoración
            Label valoracionLabel = new Label("Valoración:");
            Slider valoracionSlider = new Slider(0, 10, 5);
            valoracionSlider.setShowTickLabels(true);
            valoracionSlider.setShowTickMarks(true);
            valoracionSlider.setMajorTickUnit(1);
            valoracionSlider.setMinorTickCount(0);
            valoracionSlider.setSnapToTicks(true);
            valoracionSlider.setBlockIncrement(1);
            
            Label valoracionValueLabel = new Label("5.0");
            valoracionSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                valoracionValueLabel.setText(String.format("%.1f", newValue.doubleValue()));
            });
            
            HBox valoracionBox = new HBox(10, valoracionSlider, valoracionValueLabel);
            
            // Comentario
            Label comentarioLabel = new Label("Comentario:");
            TextArea comentarioTextArea = new TextArea();
            comentarioTextArea.setPrefHeight(150);
            comentarioTextArea.setWrapText(true);
            
            grid.add(valoracionLabel, 0, 0);
            grid.add(valoracionBox, 1, 0);
            grid.add(comentarioLabel, 0, 1);
            grid.add(comentarioTextArea, 1, 1);
            
            dialog.getDialogPane().setContent(grid);
            
            // Convertir el resultado
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == buttonTypeOk) {
                    if (comentarioTextArea.getText().trim().isEmpty()) {
                        showAlert(Alert.AlertType.WARNING, "Campos incompletos", "El comentario no puede estar vacío.");
                        return null;
                    }
                    
                    return new Opinion(
                        0, // ID temporal
                        userService.getCurrentUser().getId(),
                        userService.getCurrentUser().getNombre(),
                        currentBook.getId(),
                        valoracionSlider.getValue(),
                        comentarioTextArea.getText().trim(),
                        LocalDate.now()
                    );
                }
                return null;
            });
            
            // Mostrar el diálogo y procesar el resultado
            dialog.showAndWait().ifPresent(opinion -> {
                // Guardar la opinión en la base de datos
                if (DaoOpinion.agregarOpinion(opinion)) {
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Tu opinión ha sido publicada correctamente.");
                    
                    // Recargar opiniones
                    loadOpiniones();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo publicar la opinión. Inténtalo de nuevo.");
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al publicar la opinión: " + e.getMessage());
        }
    }
    
    @FXML
    void handleVerMasRecomendaciones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Recomendaciones.fxml"));
            Parent root = loader.load();
        
            RecomendacionesController controller = loader.getController();
        
            // Si el libro tiene categorías, buscar por la primera categoría
            if (currentBook.getCategories() != null && !currentBook.getCategories().isEmpty()) {
                controller.setGenero(currentBook.getCategories().get(0));
            } 
            // Si no tiene categorías pero tiene autor, buscar por autor
            else if (currentBook.getAuthors() != null && !currentBook.getAuthors().isEmpty()) {
                controller.setAutor(currentBook.getAuthors().get(0));
            }
        
            Stage stage = new Stage();
            stage.setTitle("Recomendaciones similares a " + currentBook.getTitle());
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainBorderPane.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar vista de recomendaciones", e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la vista de recomendaciones: " + e.getMessage());
        }
    }

    private void handleFavoriteChanged() {
        if (favoritesPane.isVisible()) {
            displayFavorites();
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
            LOGGER.log(Level.SEVERE, "Error al cargar pantalla de login", e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la pantalla de inicio de sesión: " + e.getMessage());
        }
    }

    @FXML
    void handleLimpiar(ActionEvent event) {
        // Limpiar todos los campos del formulario
        txtTitulo.clear();
        txtAutor.clear();
        txtEditorial.clear();
        txtIsbn.clear();
        fechaPublicacion.setValue(LocalDate.now());
        comboGenero.getSelectionModel().selectFirst();
        comboEstado.getSelectionModel().select("Pendiente");
        txtDescripcion.clear();
        updateStatusAddBook("");
    }
    
    @FXML
    void handleGuardar(ActionEvent event) {
        // Obtener el usuario actual
        Usuario usuarioActual = userService.getCurrentUser();
        
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
            
            updateStatusAddBook("Guardando libro...");
            
            // Manejar el caso especial de "Favorito"
            if (estado.equals("favorito")) {
                if (DaoBook.guardarFavorito(userId, libro)) {
                    updateStatusAddBook("Libro agregado correctamente a tus favoritos.");
                    handleLimpiar(null); // Limpiar el formulario
                } else {
                    updateStatusAddBook("No se pudo guardar el libro como favorito. Inténtalo de nuevo.");
                }
            } else {
                if (DaoBook.guardarLibroManual(userId, libro, estado)) {
                    updateStatusAddBook("Libro agregado correctamente a tu colección.");
                    handleLimpiar(null); // Limpiar el formulario
                } else {
                    updateStatusAddBook("No se pudo guardar el libro. Inténtalo de nuevo.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusAddBook("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void verRecomendaciones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Recomendaciones.fxml"));
            Parent root = loader.load();
            
            // Crear una nueva ventana (Stage) en lugar de reemplazar la actual
            Stage stage = new Stage();
            stage.setTitle("Recomendaciones de libros");
            stage.setScene(new Scene(root, 800, 600));
            
            // Hacer que la ventana sea modal (bloquea la interacción con la ventana principal)
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainBorderPane.getScene().getWindow());
            
            // Mostrar la ventana
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar vista de recomendaciones", e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la vista de recomendaciones: " + e.getMessage());
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void updateStatusFavorites(String message) {
        if (statusLabelFavorites != null) {
            statusLabelFavorites.setText(message);
        }
    }
    
    private void updateStatusAddBook(String message) {
        if (statusLabelAddBook != null) {
            statusLabelAddBook.setText(message);
        }
    }
    
    private void updateStatusDetails(String message) {
        if (statusLabelDetails != null) {
            statusLabelDetails.setText(message);
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
