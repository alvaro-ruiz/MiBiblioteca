package Controller;

import Model.Book;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class BookCardController {

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label authorLabel;

    @FXML
    private Button viewDetailsButton;

    private Book book;
    
    private EventHandler<ActionEvent> onViewDetailsAction;

    @FXML
    void initialize() {
    }

    public void setBook(Book book) {
        this.book = book;

        titleLabel.setText(book.getTitle());

        authorLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty()
                ? String.join(", ", book.getAuthors())
                : "Autor desconocido");

        String imageUrl = book.getThumbnail();

        if (imageUrl != null && !imageUrl.isBlank() && imageUrl.startsWith("http")) {
            try {
                Image image = new Image(imageUrl, true);
                coverImageView.setImage(image);
            } catch (Exception e) {
                loadFallbackImage();
            }
        } else {
            loadFallbackImage();
        }
    }

    private void loadFallbackImage() {
        try {
            Image fallback = new Image("/recurses/libro-abierto.png");
            coverImageView.setImage(fallback);
        } catch (Exception ex) {
            System.err.println("No se pudo cargar la imagen de fallback: " + ex.getMessage());
            coverImageView.setImage(null);
        }
    }

    public void setOnViewDetailsAction(EventHandler<ActionEvent> handler) {
        this.onViewDetailsAction = handler;
        viewDetailsButton.setOnAction(handler);
    }

    @FXML
    void handleViewDetails(ActionEvent event) {
        if (onViewDetailsAction != null) {
            onViewDetailsAction.handle(event);
        }
    }
}
