package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Accept Offer (Donor). Does nothing but close until the accept feature is built (FLOWS.md module 7). */
public class AcceptOfferDialogController extends DialogCard {

    @FXML private Label acceptOfferDescriptionLabel;

    public void show(String groupName, String quantity, String pickupTime, int otherOffersCount) {
        open();

        String otherOffersText = (otherOffersCount > 0)
                ? " The other " + otherOffersCount + " " + (otherOffersCount == 1 ? "offer is" : "offers are") + " declined automatically."
                : "";

        acceptOfferDescriptionLabel.setText(
                groupName + " picks up the full " + quantity + " " + pickupTime + "." + otherOffersText +
                        " Your contact number and handover instructions are shared with " + groupName + ", and you’ll see theirs."
        );
    }

    // PATTERN:ACTION confirms accepting offer
    @FXML
    private void handleConfirmAcceptOffer() {
        close();
    }
}
