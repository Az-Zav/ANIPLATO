package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Withdraw Offer (the offerer, donor or food group). */
public class WithdrawOfferDialogController extends DialogCard {

    @FXML private Label withdrawOfferDescriptionLabel;

    private Runnable onWithdraw;

    public void show(String donorName) {
        onWithdraw = null;
        open();
        withdrawOfferDescriptionLabel.setText(donorName + " won’t see this offer anymore. You can send a new one while the listing is still open.");
    }

    /** Withdraw with a real action. */
    public void show(String posterName, Runnable onConfirm) {
        show(posterName);
        onWithdraw = onConfirm;
    }

    // PATTERN:ACTION confirms withdrawing offer
    @FXML
    private void handleConfirmWithdrawOffer() {
        if (onWithdraw != null) {
            confirm(onWithdraw);
            return;
        }
        close();
    }
}
