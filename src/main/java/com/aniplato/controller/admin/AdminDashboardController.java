package com.aniplato.controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminDashboardController {

    @FXML private VBox rootContainer;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (rootContainer != null && rootContainer.getScene() != null) {
                // Absolute classpath lookup
                URL cssUrl = AdminDashboardController.class.getResource("/com/aniplato/view/shared/aniplato.css");

                if (cssUrl != null) {
                    rootContainer.getScene().getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("❌ Could not find aniplato.css on classpath!");
                }
            }
        });
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/aniplato/view/shared/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}