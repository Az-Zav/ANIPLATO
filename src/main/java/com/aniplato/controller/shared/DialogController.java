package com.aniplato.controller.shared;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class DialogController {

    // Modal Containers
    @FXML private VBox rejectDialogBox;
    @FXML private VBox approveDialogBox;

    // Reject Dialog Dynamic Fields
    @FXML private Label rejectDescriptionLabel;
    @FXML private TextArea rejectReasonTextArea;

    // Approve Dialog Dynamic Fields
    @FXML private Label approveDescriptionLabel;

    @FXML
    private void initialize() {
        // Overlay stays hidden until showRejectMode() or showApproveMode() is called by parent controller
    }

    /** Prepares dialog for Reject mode with target organization details. */
    public void showRejectMode(String orgName) {
        rejectDialogBox.setVisible(true);
        rejectDialogBox.setManaged(true);

        approveDialogBox.setVisible(false);
        approveDialogBox.setManaged(false);

        rejectDescriptionLabel.setText("We’ll email " + orgName + " your reason so they can fix their documents and register again. No account is created.");
        rejectReasonTextArea.clear();
    }

    /** Prepares dialog for Approve mode with target organization details. */
    public void showApproveMode(String orgName, String orgEmail) {
        approveDialogBox.setVisible(true);
        approveDialogBox.setManaged(true);

        rejectDialogBox.setVisible(false);
        rejectDialogBox.setManaged(false);

        approveDescriptionLabel.setText("We’ll create " + orgName + "’s account and email temporary login details to " + orgEmail + ".");
    }

    // PATTERN:ACTION cancels dialog overlay
    @FXML
    private void handleCancel() {
        closeDialog();
    }

    // PATTERN:ACTION confirms rejection reason and routes back to pending registrations
    @FXML
    private void handleConfirmReject() {
        String reason = rejectReasonTextArea.getText();

        // TODO(service): registrationService.rejectRegistration(orgId, reason)
        closeDialog();
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    // PATTERN:ACTION confirms approval and routes back to pending registrations
    @FXML
    private void handleConfirmApprove() {
        // TODO(service): registrationService.approveRegistration(orgId)
        closeDialog();
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    /** Helper method to hide the outer modal overlay container when dismissing dialog */
    private void closeDialog() {
        if (rejectDialogBox != null && rejectDialogBox.getParent() != null) {
            rejectDialogBox.getParent().setVisible(false);
            rejectDialogBox.getParent().setManaged(false);
        }
    }
}