package com.leprpht.clickerapp;

import com.leprpht.clickerapp.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 400);
        MainController controller = fxmlLoader.getController();
        String css = Objects.requireNonNull(getClass().getResource("/com/leprpht/clickerapp/css/style.css")).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Clicker App");
        stage.setScene(scene);
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> {
            controller.shutdown();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}