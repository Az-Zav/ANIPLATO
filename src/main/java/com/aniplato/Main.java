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
            // Path pointing directly to dashboard.fxml inside view/admin/
            URL fxmlLocation = getClass().getResource("/com/aniplato/view/admin/dashboard.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ CRITICAL ERROR: Could not find /com/aniplato/view/admin/dashboard.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Baseline 1280x720 scene
            Scene scene = new Scene(root, 1280, 720);

            primaryStage.setTitle("ANIPLATO - Surplus Food Rescue System (Admin Dashboard)");
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