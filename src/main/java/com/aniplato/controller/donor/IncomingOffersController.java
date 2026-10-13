package com.aniplato.controller.donor;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class IncomingOffersController {

    @FXML private Label offerCountLabel;
    @FXML private VBox offerRowsBox;
    @FXML private Label emptyOffersLabel;

    // PATTERN:DYNAMIC fills offer count and empty state
    @FXML
    private void initialize() {
        // TODO(service): offerCountLabel.setText(offerService.countIncomingForDonor(...) + " offers")
        // TODO(service): add one row to offerRowsBox per incoming offer
        // TODO(service): show emptyOffersLabel when there are no incoming offers
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleReviewOffer() {
        // TODO(service): load the selected offer review state
    }
}
