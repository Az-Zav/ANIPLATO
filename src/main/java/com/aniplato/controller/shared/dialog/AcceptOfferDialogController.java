package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Accept Offer (the poster, donor or food group). */
public class AcceptOfferDialogController extends DialogCard {

    @FXML private Label acceptOfferDescriptionLabel;

    private Runnable onAccept;

    /** Accept: {@code onConfirm} runs the service call. */
    public void show(String groupName, String quantity, String pickupTime, int otherOffersCount, Runnable onConfirm) {
        onAccept = onConfirm;
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
        confirm(onAccept);
    }
}
