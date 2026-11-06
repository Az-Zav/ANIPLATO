package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Withdraw Offer (the offerer, donor or food group). */
public class WithdrawOfferDialogController extends DialogCard {

    @FXML private Label withdrawOfferDescriptionLabel;

    private Runnable onWithdraw;

    /** Withdraw: {@code onConfirm} runs the service call. */
    public void show(String posterName, Runnable onConfirm) {
        onWithdraw = onConfirm;
        open();
        withdrawOfferDescriptionLabel.setText(posterName + " won’t see this offer anymore. You can send a new one while the listing is still open.");
    }

    // PATTERN:ACTION confirms withdrawing offer
    @FXML
    private void handleConfirmWithdrawOffer() {
        confirm(onWithdraw);
    }
}
