package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Upload Renewal. Does nothing but close until the renewal feature is built (FLOWS.md module 4). */
public class UploadRenewalDialogController extends DialogCard {

    @FXML private Label uploadRenewalTitleLabel;
    @FXML private Label selectedRenewalFileLabel;
    @FXML private TextField newExpiryDateField;

    public void show(String documentName) {
        open();
        uploadRenewalTitleLabel.setText("Upload renewal: " + documentName);
        selectedRenewalFileLabel.setText("No file chosen");
        newExpiryDateField.clear();
    }

    @FXML
    private void handleChooseRenewalFile() {
        // TODO(file-picker): Open FileChooser and update selectedRenewalFileLabel
    }

    // PATTERN:ACTION confirms uploading renewal document
    @FXML
    private void handleConfirmUploadRenewal() {
        close();
    }
}
