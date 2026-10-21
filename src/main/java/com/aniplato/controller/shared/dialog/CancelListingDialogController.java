package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/** Cancel Listing (Donor). Does nothing but close until the cancel feature is built (FLOWS.md module 8). */
public class CancelListingDialogController extends DialogCard {

    @FXML private Label cancelListingDescriptionLabel;
    @FXML private TextArea cancelListingReasonTextArea;

    public void show(String listingTitle, String recipientName) {
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
        close();
    }
}
