package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleLogin() {
        // TODO(service): authenticationService.login(emailField.getText(), passwordField.getText())
        // TODO(service): temporary password -> change-password dialog before anything else (R1.7)
        // TODO(service): pick the dashboard by the account's role
        Navigator.show(Screen.ADMIN_DASHBOARD);
    }
}
