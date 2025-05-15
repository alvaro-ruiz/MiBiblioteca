package Controller;

import Model.Book;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class BookCardController {

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label authorLabel;

    @FXML
    private Button viewDetailsButton;

    private EventHandler<ActionEvent> onViewDetailsAction;

    @FXML
    void initialize() {
    }

    public void setBook(Book book) {
        titleLabel.setText(book.getTitle());

        authorLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty()
                ? String.join(", ", book.getAuthors())
                : "Autor desconocido");

        String imageUrl = book.getThumbnail();

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                if (imageUrl.startsWith("http:")) {
                    imageUrl = imageUrl.replace("http:", "https:");
                }
                
                Image image = new Image(imageUrl, true);
                image.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        System.err.println("Error al cargar la imagen: ");
                        loadFallbackImage();
                    }
                });
                
                coverImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Excepción al cargar la imagen: " + e.getMessage());
                loadFallbackImage();
            }
        } else {
            loadFallbackImage();
        }
    }

    private void loadFallbackImage() {
        try {
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

    public void setOnViewDetailsAction(EventHandler<ActionEvent> handler) {
        this.onViewDetailsAction = handler;
        viewDetailsButton.setOnAction(handler);
    }
    
	public EventHandler<ActionEvent> getOnViewDetailsAction() {
		return onViewDetailsAction;
	}

	@FXML
    void handleViewDetails(ActionEvent event) {
        if (onViewDetailsAction != null) {
            onViewDetailsAction.handle(event);
        }
    }
}
