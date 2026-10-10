package com.aniplato.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private VBox rootContainer;
    @FXML private Button logoutBtn;

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/aniplato/view/shared/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            System.err.println("❌ Failed to navigate to login view:");
            e.printStackTrace();
        }
    }
}