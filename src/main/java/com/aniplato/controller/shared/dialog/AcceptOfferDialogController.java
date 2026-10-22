package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Accept Offer (the poster, donor or food group). */
public class AcceptOfferDialogController extends DialogCard {

    @FXML private Label acceptOfferDescriptionLabel;

    private Runnable onAccept;

    public void show(String groupName, String quantity, String pickupTime, int otherOffersCount) {
        onAccept = null;
        open();

        String otherOffersText = (otherOffersCount > 0)
                ? " The other " + otherOffersCount + " " + (otherOffersCount == 1 ? "offer is" : "offers are") + " declined automatically."
                : "";

        acceptOfferDescriptionLabel.setText(
                groupName + " picks up the full " + quantity + " " + pickupTime + "." + otherOffersText +
                        " Your contact number and handover instructions are shared with " + groupName + ", and you’ll see theirs."
        );
    }

    /** Accept with a real action. */
    public void show(String groupName, String quantity, String pickupTime, int otherOffersCount, Runnable onConfirm) {
        show(groupName, quantity, pickupTime, otherOffersCount);
        onAccept = onConfirm;
    }

    // PATTERN:ACTION confirms accepting offer
    @FXML
    private void handleConfirmAcceptOffer() {
        if (onAccept != null) {
            confirm(onAccept);
            return;
        }
        close();
    }
}
