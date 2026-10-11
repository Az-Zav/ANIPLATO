package com.aniplato.controller.donor;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DonorDashboardController {

    @FXML private Label openListingsCountLabel;
    @FXML private Label offersToReviewCountLabel;
    @FXML private Label rescuedKgLabel;
    @FXML private VBox recentListingsBox;
    @FXML private Label emptyListingsLabel;

    // PATTERN:DYNAMIC fills dashboard summary and empty state
    @FXML
    private void initialize() {
        // TODO(service): openListingsCountLabel.setText(listingService.countActiveForDonor(...))
        // TODO(service): offersToReviewCountLabel.setText(offerService.countOfferedForDonor(...))
        // TODO(service): rescuedKgLabel.setText(listingService.sumRescuedKgForDonor(...))
        // TODO(service): show emptyListingsLabel when the donor has no listings
        // TODO(service): add one row to recentListingsBox per recent donor listing
    }
}
