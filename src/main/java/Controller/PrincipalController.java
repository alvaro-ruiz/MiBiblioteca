package Controller;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import Model.*;
import Api.GoogleBooksAPI;

public class PrincipalController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab searchTab;

    @FXML
    private Tab favoritesTab;

    @FXML
    private GridPane booksGrid;

    @FXML
    private Label statusLabel;

    @FXML
    private Button logoutButton;

    private UserService userService = new UserService();
    private ObservableList<Book> books = FXCollections.observableArrayList();
    private ObservableList<Book> favorites = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        // Configurar el mensaje de bienvenida con el nombre del usuario
        if (userService.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenido, " + userService.getCurrentUser().getNombre());
        }

        // Cargar libros iniciales
        loadInitialBooks();
    }

    private void loadInitialBooks() {
        statusLabel.setText("Cargando libros...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return GoogleBooksAPI.searchBooks("programming");
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
            showAlert(Alert.AlertType.WARNING, "Búsqueda vacía", 
                    "Por favor, ingrese un término de búsqueda.");
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
                    statusLabel.setText("");
                    
                    if (result.isEmpty()) {
                        statusLabel.setText("No se encontraron resultados para: " + query);
                    }
                } else {
                    statusLabel.setText("Error en la búsqueda");
                }
            });
        });
    }

    private void displayBooks(List<Book> booksList) {
        booksGrid.getChildren().clear();
        
        int column = 0;
        int row = 0;
        
        for (Book book : booksList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BookCard.fxml"));
                VBox bookCard = loader.load();
                
                BookCardController controller = loader.getController();
                controller.setBook(book);
                controller.setOnViewDetailsAction(e -> viewBookDetails(book));
                
                booksGrid.add(bookCard, column, row);
                
                column++;
                if (column > 2) {  // 3 columnas
                    column = 0;
                    row++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void viewBookDetails(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BookDetail.fxml"));
            Parent detailView = loader.load();
            
            BookDetailController controller = loader.getController();
            controller.setBook(book);
            
            Stage detailStage = new Stage();
            detailStage.setTitle(book.getTitle());
            detailStage.setScene(new Scene(detailView, 800, 600));
            detailStage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "No se pudo cargar los detalles del libro: " + e.getMessage());
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        userService.logout();
        
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
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
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "No se pudo cargar la pantalla de inicio de sesión: " + e.getMessage());
        }
    }

    @FXML
    void handleTabChange() {
        if (tabPane.getSelectionModel().getSelectedItem() == favoritesTab) {
            displayBooks(favorites);
            
            if (favorites.isEmpty()) {
                statusLabel.setText("No tienes libros favoritos guardados.");
            } else {
                statusLabel.setText("");
            }
        } else {
            displayBooks(books);
            
            if (books.isEmpty()) {
                statusLabel.setText("No hay resultados de búsqueda.");
            } else {
                statusLabel.setText("");
            }
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
