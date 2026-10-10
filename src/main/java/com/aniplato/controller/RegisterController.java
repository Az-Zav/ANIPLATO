package com.aniplato.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField orgNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    private void handleRegister() {
        System.out.println("Register submit clicked for: " + orgNameField.getText());
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/aniplato/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}