package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Upgrade to Business Donor. Does nothing but close until the upgrade feature is built (FLOWS.md module 4). */
public class UpgradeBusinessDonorDialogController extends DialogCard {

    @FXML private Label selectedMayorPermitFileLabel;
    @FXML private TextField mayorPermitExpiryDateField;
    @FXML private Label selectedSanitaryPermitFileLabel;
    @FXML private TextField sanitaryPermitExpiryDateField;

    public void show() {
        open();
        selectedMayorPermitFileLabel.setText("No file chosen");
        mayorPermitExpiryDateField.clear();
        selectedSanitaryPermitFileLabel.setText("No file chosen");
        sanitaryPermitExpiryDateField.clear();
    }

    @FXML
    private void handleChooseMayorPermitFile() {
        // TODO(file-picker): choose the file with an UploadSlot and show it in selectedMayorPermitFileLabel (FLOWS.md module 4)
    }

    @FXML
    private void handleChooseSanitaryPermitFile() {
        // TODO(file-picker): choose the file with an UploadSlot and show it in selectedSanitaryPermitFileLabel (FLOWS.md module 4)
    }

    // PATTERN:ACTION confirms upgrading to business donor
    @FXML
    private void handleConfirmUpgradeBusinessDonor() {
        close();
    }
}
