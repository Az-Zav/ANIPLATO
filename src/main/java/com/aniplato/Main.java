package com.aniplato;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
             // Path pointing directly to login.fxml inside view/login.fxml
            URL fxmlLocation = getClass().getResource("/com/aniplato/view/login.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ CRITICAL ERROR: Could not find /com/aniplato/view/login.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Baseline 1280x720 scene
            Scene scene = new Scene(root, 1280, 720);

            primaryStage.setTitle("ANIPLATO - Sign In");
            primaryStage.setScene(scene);

            // Allow window resizability so metric cards and data tables scale smoothly
            primaryStage.setResizable(true);

            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ ERROR STARTING APPLICATION:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}