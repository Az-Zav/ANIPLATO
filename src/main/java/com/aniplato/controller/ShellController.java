package com.aniplato.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ShellController {

    @FXML private StackPane contentArea;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        // Load default main page into the content frame on open
        loadView("/com/aniplato/view/admin/dashboard.fxml");
    }

    public void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("❌ Could not load view into shell container: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateDashboard(ActionEvent event) {
        loadView("/com/aniplato/view/admin/dashboard.fxml");
    }

    @FXML
    private void handleNavigateAccounts(ActionEvent event) {
        loadView("/com/aniplato/view/admin/pending-registrations.fxml");
    }

    @FXML
    private void handleNavigateReports(ActionEvent event) {
        loadView("/com/aniplato/view/admin/reports.fxml");
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