package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.function.Consumer;

/** Cancel Listing (Donor) and Cancel Request (Food Group): the poster's reason is shown to the offerers. */
public class CancelListingDialogController extends DialogCard {

    @FXML private Label cancelListingDescriptionLabel;
    @FXML private TextArea cancelListingReasonTextArea;

    private Consumer<String> onCancel;   // gets the reason typed in the dialog

    public void show(String listingTitle, String recipientName) {
        onCancel = null;
        open();
        if (recipientName != null && !recipientName.isEmpty()) {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board. " + recipientName + "’s accepted offer is cancelled with it, and they see your reason.");
        } else {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board and its listing is cancelled.");
        }
        cancelListingReasonTextArea.clear();
    }

    /** Cancel with a real action: {@code onConfirm} gets the reason. */
    public void show(String listingTitle, String recipientName, Consumer<String> onConfirm) {
        show(listingTitle, recipientName);
        onCancel = onConfirm;
    }

    // PATTERN:ACTION confirms listing cancellation
    @FXML
    private void handleConfirmCancelListing() {
        if (onCancel != null) {
            confirm(() -> onCancel.accept(cancelListingReasonTextArea.getText()));
            return;
        }
        close();
    }
}
