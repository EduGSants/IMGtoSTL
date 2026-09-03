package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root =
                FXMLLoader.load(getClass().getResource("/views/main.fxml"));

        Scene scene = new Scene(root, 600, 700);

        primaryStage.setTitle("Conversor IMG → STL");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}