package com.aniplato.controller.shared.dialog;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.AuthenticationService;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** Set New Password: a temporary password must be replaced before anything else (R1.7). */
public class SetNewPasswordDialogController extends DialogCard {

    @FXML private VBox currentPasswordRow;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private final AuthenticationService authenticationService = new AuthenticationService();
    private Runnable afterPasswordSet;   // what to do once the new password is saved

    /** {@code currentPassword} is null when the user has to type it; otherwise it was just typed at login. */
    public void show(String currentPassword, Runnable onDone) {
        this.afterPasswordSet = onDone;
        open();
        currentPasswordField.setText(currentPassword == null ? "" : currentPassword);
        // R1.7 first login: the temporary password was just typed at login, so only ask for the new one
        boolean askCurrent = currentPassword == null;
        currentPasswordRow.setVisible(askCurrent);
        currentPasswordRow.setManaged(askCurrent);
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    // PATTERN:ACTION confirms setting new password
    @FXML
    private void handleConfirmSetNewPassword() {
        try {
            authenticationService.changePassword(currentPasswordField.getText(), newPasswordField.getText(), confirmPasswordField.getText());
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());   // A8: stay open until the password is accepted
            return;
        }
        close();
        if (afterPasswordSet != null) afterPasswordSet.run();
    }
}
