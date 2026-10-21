package com.aniplato.controller.shared.dialog;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.RegistrationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Approve Organization: creates the account and emails the temporary login. */
public class ApproveDialogController extends DialogCard {

    @FXML private Label approveDescriptionLabel;

    private final RegistrationService registrationService = new RegistrationService();
    private Integer reviewedRegistrationId;   // the registration this dialog acts on

    public void show(int registrationId, String orgName, String orgEmail) {
        this.reviewedRegistrationId = registrationId;
        open();
        approveDescriptionLabel.setText("We’ll create " + orgName + "’s account and email temporary login details to " + orgEmail + ".");
    }

    // PATTERN:ACTION confirms approval
    @FXML
    private void handleConfirmApprove() {
        boolean emailed;
        try {
            emailed = registrationService.approve(reviewedRegistrationId);
        } catch (RuleViolationException e) {
            close();
            DialogController.showError(e.getMessage());   // A8
            return;
        }
        close();
        if (!emailed) DialogController.showError("The account was created, but the email could not be sent. "
                + "The user can get a temporary password with Forgot password.");
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }
}
