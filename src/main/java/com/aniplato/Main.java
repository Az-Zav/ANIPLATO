package com.aniplato;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Pointing directly to login.fxml in view/
            URL fxmlLocation = getClass().getResource("/com/aniplato/view/login.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ CRITICAL ERROR: Could not find /com/aniplato/view/login.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Fixed 1280x720 baseline scene size
            Scene scene = new Scene(root, 1280, 720);

            primaryStage.setTitle("ANIPLATO - Surplus Food Rescue System");
            primaryStage.setScene(scene);

            // Fixed non-resizable window
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