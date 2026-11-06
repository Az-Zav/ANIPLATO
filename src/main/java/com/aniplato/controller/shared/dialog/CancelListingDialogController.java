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

    /** Cancel: {@code onConfirm} gets the reason. */
    public void show(String listingTitle, String recipientName, Consumer<String> onConfirm) {
        onCancel = onConfirm;
        open();
        if (recipientName != null && !recipientName.isEmpty()) {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board. " + recipientName + "’s accepted offer is cancelled with it, and they see your reason.");
        } else {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board and its listing is cancelled.");
        }
        cancelListingReasonTextArea.clear();
    }

    // PATTERN:ACTION confirms listing cancellation
    @FXML
    private void handleConfirmCancelListing() {
        confirm(() -> onCancel.accept(cancelListingReasonTextArea.getText()));
    }
}
