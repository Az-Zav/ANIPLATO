package com.aniplato.controller.shared.dialog;

import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.AuthenticationService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** Forgot Password: emails a temporary password (R1.7). */
public class ForgotPasswordDialogController extends DialogCard {

    @FXML private TextField forgotPasswordEmailField;

    private final AuthenticationService authenticationService = new AuthenticationService();

    public void show(String email) {
        open();
        forgotPasswordEmailField.setText(email != null ? email : "");
    }

    // PATTERN:ACTION confirms forgot password request
    @FXML
    private void handleConfirmForgotPassword() {
        String email = forgotPasswordEmailField.getText().trim();
        if (email.isEmpty()) {
            showProblem("Please enter your email address.");
            return;
        }
        try {
            authenticationService.requestPasswordReset(email);
        } catch (RuleViolationException e) {
            showProblem(e.getMessage());   // A8
            return;
        }
        // same answer either way, so the dialog does not reveal which emails have accounts; it stays open and says so inline
        showNote("If that email has an account, a temporary password is on its way.");
    }
}
