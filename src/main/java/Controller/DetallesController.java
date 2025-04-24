package Controller;

import Model.Book;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.image.ImageView;

public class DetallesController {

    @FXML
    private RadioButton guardar;

    @FXML
    private Label lblAutor;

    @FXML
    private Label lblDescripcion;
    
    @FXML
    private ImageView img;

    @FXML
    private Label lbltitle;
    
    @FXML
    void initialize() {
    }
    
    public void setBook(Book book) {
    	
    }

}
