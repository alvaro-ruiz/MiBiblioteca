package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application{

    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(MainApp.class.getResource("/View/Login.fxml"));
        primaryStage.setTitle("Biblioteca Digital - Google Books");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
