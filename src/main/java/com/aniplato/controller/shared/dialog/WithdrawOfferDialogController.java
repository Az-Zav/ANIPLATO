package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Withdraw Offer (Food Group). Does nothing but close until the offer feature is built (FLOWS.md module 6). */
public class WithdrawOfferDialogController extends DialogCard {

    @FXML private Label withdrawOfferDescriptionLabel;

    public void show(String donorName) {
        open();
        withdrawOfferDescriptionLabel.setText(donorName + " won’t see this offer anymore. You can send a new one while the listing is still open.");
    }

    // PATTERN:ACTION confirms withdrawing offer
    @FXML
    private void handleConfirmWithdrawOffer() {
        close();
    }
}
