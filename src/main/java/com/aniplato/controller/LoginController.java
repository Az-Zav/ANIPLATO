package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.service.AuthenticationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {

    // FXML: fx:id="emailField" (TextField) and fx:id="passwordField" (PasswordField) in login.fxml
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // FXML: login.fxml needs <fx:include source="shared/dialog.fxml" fx:id="dialog"/>, which injects both
    // fields below ("dialog" = the include's root VBox, "dialogController" = its controller).
    // Without it a login with a temporary password cannot show the Set New Password dialog (R1.7).
    @FXML private VBox dialog;
    @FXML private DialogController dialogController;

    private final AuthenticationService authenticationService = new AuthenticationService();

    // FXML: loginButton onAction="#handleLogin"
    // PATTERN:ACTION
    @FXML
    private void handleLogin() {
        try {
            Account account = authenticationService.login(emailField.getText(), passwordField.getText());
            if (account.mustChangePassword) {
                // R1.7: nothing else is reachable until the temporary password is replaced
                dialog.setVisible(true);
                dialog.setManaged(true);
                dialogController.showSetNewPasswordMode(passwordField.getText(), () -> Navigator.show(Screen.SHELL));
            } else {
                Navigator.show(Screen.SHELL);
            }
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());   // A8
        }
    }
}
