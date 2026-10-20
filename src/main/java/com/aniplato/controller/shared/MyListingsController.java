package com.aniplato.controller.shared;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Shared screen. Never checks the role. */
public class MyListingsController {

    @FXML private Label listingCountLabel;
    @FXML private Label activeListingsCountLabel;
    @FXML private Label awaitingPickupCountLabel;
    @FXML private Label openOnBoardCountLabel;
    @FXML private TextField listingSearchField;
    @FXML private VBox listingRowsBox;
    @FXML private Label emptyListingsLabel;
    @FXML private DialogController dialogController;

    // PATTERN:DYNAMIC fills listing count and empty state
    @FXML
    private void initialize() {
        // TODO(service): listingCountLabel.setText(listingService.countForDonor(...) + " listings")
        // TODO(service): add one row to listingRowsBox per donor listing
        // TODO(service): show emptyListingsLabel when the donor has no listings
    }

    // PATTERN:ACTION placeholders: filter/search/release behavior is service-backed later
    @FXML private void handleFilterAll() { }
    @FXML private void handleFilterAccepted() { }
    @FXML private void handleFilterOpen() { }
    @FXML private void handleConfirmRelease() { }

    // PATTERN:ACTION opens the existing cancel-listing confirmation dialog
    @FXML
    private void handleCancelListing() {
        dialogController.showCancelListingMode("Artisan Sourdough & Baguettes", "San Roque Parish Feeding Mission");
    }
}
