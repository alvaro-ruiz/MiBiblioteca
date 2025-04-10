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
        // Inicialización si es necesaria
    }

    public void setBook(Book book) {
        this.book = book;
        
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
