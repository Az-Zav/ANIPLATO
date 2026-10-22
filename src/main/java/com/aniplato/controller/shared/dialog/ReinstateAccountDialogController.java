package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Reinstate Account: gives a Suspended account its full access back. */
public class ReinstateAccountDialogController extends DialogCard {

    @FXML private Label reinstateDescriptionLabel;

    private Runnable onReinstate;

    /** Reinstate: {@code onConfirm} runs the service call. */
    public void show(String orgName, String details, Runnable onConfirm) {
        onReinstate = onConfirm;
        open();
        if (details != null && !details.isEmpty()) {
            reinstateDescriptionLabel.setText(orgName + " gets full access again. " + details);
        } else {
            reinstateDescriptionLabel.setText(orgName + " gets full access again.");
        }
    }

    // PATTERN:ACTION confirms account reinstatement
    @FXML
    private void handleConfirmReinstate() {
        confirm(onReinstate);
    }
}
