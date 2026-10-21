package com.aniplato.controller.shared.dialog;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.RegistrationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/** Reject Organization: the admin's reason is emailed and no account is created. */
public class RejectDialogController extends DialogCard {

    @FXML private Label rejectDescriptionLabel;
    @FXML private TextArea rejectReasonTextArea;

    private final RegistrationService registrationService = new RegistrationService();
    private Integer reviewedRegistrationId;   // the registration this dialog acts on

    public void show(int registrationId, String orgName) {
        this.reviewedRegistrationId = registrationId;
        open();
        rejectDescriptionLabel.setText("We’ll email " + orgName + " your reason so they can fix their documents and register again. No account is created.");
        rejectReasonTextArea.clear();
    }

    // PATTERN:ACTION confirms rejection
    @FXML
    private void handleConfirmReject() {
        boolean emailed;
        try {
            emailed = registrationService.reject(reviewedRegistrationId, rejectReasonTextArea.getText());
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());   // A8: stay open so the reason can be fixed
            return;
        }
        close();
        if (!emailed) DialogController.showError("The registration was rejected, but the email could not be sent. Check the email log.");
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }
}
