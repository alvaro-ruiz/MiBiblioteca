package Controller;

import Model.Book;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import Api.GoogleBooksAPI;

public class DetallesController {

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label authorLabel;

    @FXML
    private Label publisherLabel;

    @FXML
    private Label publishedDateLabel;

    @FXML
    private Label pageCountLabel;

    @FXML
    private Label isbnLabel;

    @FXML
    private FlowPane categoriesPane;

    @FXML
    private WebView descriptionWebView;

    @FXML
    private Button favoriteButton;

    @FXML
    private Hyperlink previewLink;

    @FXML
    private Button closeButton;

    private Book book;
    private boolean isFavorite = false;
    private ObservableList<Book> favorites;
    private Runnable onFavoriteChangedCallback;

    @FXML
    void initialize() {
        // Inicialización si es necesaria
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
                Image image = new Image(book.getThumbnail(), true);
                coverImageView.setImage(image);
            } catch (Exception e) {
                // Si hay un error al cargar la imagen, usar una imagen por defecto
                coverImageView.setImage(new Image(getClass().getResourceAsStream("/images/book-placeholder.png")));
            }
        } else {
            // Si no hay imagen, usar una imagen por defecto
            coverImageView.setImage(new Image(getClass().getResourceAsStream("/images/book-placeholder.png")));
        }
        
        // Cargar detalles completos del libro
        loadBookDetails();
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
        isFavorite = !isFavorite;
        
        if (isFavorite) {
            favoriteButton.setText("Quitar de favoritos");
            if (!favorites.stream().anyMatch(b -> b.getId().equals(book.getId()))) {
                favorites.add(book);
            }
        } else {
            favoriteButton.setText("Añadir a favoritos");
            favorites.removeIf(b -> b.getId().equals(book.getId()));
        }
        
        // Notificar al controlador principal que los favoritos han cambiado
        if (onFavoriteChangedCallback != null) {
            onFavoriteChangedCallback.run();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
