package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Reinstate Account: gives a Suspended account its full access back. */
public class ReinstateAccountDialogController extends DialogCard {

    @FXML private Label reinstateDescriptionLabel;

    private Runnable onReinstate;

    public void show(String orgName, String details) {
        onReinstate = null;
        open();
        if (details != null && !details.isEmpty()) {
            reinstateDescriptionLabel.setText(orgName + " gets full access again. " + details);
        } else {
            reinstateDescriptionLabel.setText(orgName + " gets full access again.");
        }
    }

    /** Reinstate with a real action. */
    public void show(String orgName, String details, Runnable onConfirm) {
        show(orgName, details);
        onReinstate = onConfirm;
    }

    // PATTERN:ACTION confirms account reinstatement
    @FXML
    private void handleConfirmReinstate() {
        if (onReinstate != null) {
            confirm(onReinstate);
            return;
        }
        close();
    }
}
