package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
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

    // FXML: login.fxml needs <fx:include source="shared/dialog.fxml" fx:id="dialog"/>, which injects both
    // fields below ("dialog" = the include's root VBox, "dialogController" = its controller).
    // Without it a login with a temporary password cannot show the Set New Password dialog (R1.7).
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
            showError(e.getMessage());   // A8
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    // TODO(service): Forgot password. Deferred; wire when the reset flow is built:
    //   1. AuthenticationService.requestPasswordReset(String email): if the account exists, generate a temporary
    //      password (generateTemporaryPassword), hash it, set must_change_password = TRUE, then email it
    //      (EmailService.Kind.PASSWORD_RESET + email/reset.html). Return nothing and give the same answer whether
    //      or not the email exists, so the screen does not reveal which emails have accounts.
    //   2. Schema: add PASSWORD_RESET to email_log.kind (update sql/schema.sql, spec 7.6 and the ER diagram together).
    //   3. Spec: add the flow to 6.2 Stage 1 and the method to 7.3 AuthenticationService.
    //   4. View: dialog.fxml needs a "Forgot password" mode (email field + Send); Figma frame not designed yet.
    //
    // FXML: forgotPasswordLink onAction="#handleForgotPassword"
    // PATTERN:ACTION
    // @FXML
    // private void handleForgotPassword() {
    //     try {
    //         authenticationService.requestPasswordReset(emailField.getText());
    //         // show the same confirmation either way: "If that email has an account, a temporary password is on its way."
    //     } catch (RuleViolationException e) {
    //         showError(e.getMessage());   // A8, e.g. blank email
    //     }
    // }
}
