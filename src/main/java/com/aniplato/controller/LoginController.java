package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.AuthenticationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {

    // FXML: fx:id="emailField" (TextField) and fx:id="passwordField" (PasswordField) in login.fxml
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // FXML: errorLabel (style class "error-text", in the Forgot password row so the form never shifts) shows a failed login in place, instead of a dialog (A8)
    @FXML private Label errorLabel;

    //FXML: shows the dialog for setting a new password when the user logs in with a temporary password
    @FXML private VBox dialog;
    @FXML private DialogController dialogController;

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML
    private void initialize() {
        // the message goes away as soon as the user edits either field
        emailField.textProperty().addListener((o, was, now) -> hideError());
        passwordField.textProperty().addListener((o, was, now) -> hideError());
    }

    // FXML: loginButton onAction="#handleLogin"
    // PATTERN:ACTION
    @FXML
    private void handleLogin() {
        hideError();
        try {
            authenticationService.login(emailField.getText(), passwordField.getText());
            if (authenticationService.mustChangePassword()) {
                // R1.7: nothing else is reachable until the temporary password is replaced
                dialog.setVisible(true);
                dialog.setManaged(true);
                dialogController.showSetNewPasswordMode(passwordField.getText(), () -> Navigator.show(Screen.SHELL));
            } else {
                Navigator.show(Screen.SHELL);
            }
        } catch (RuleViolationException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    // FXML: forgotPasswordLink onAction="#handleForgotPassword"
    // PATTERN:ACTION opens the Forgot Password dialog with whatever email was typed (R1.7)
    @FXML
    private void handleForgotPassword() {
        dialogController.showForgotPasswordMode(emailField.getText());
    }
}
