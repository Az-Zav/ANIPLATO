package com.aniplato.controller.foodgroup;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FoodGroupDashboardController {

    @FXML private Label organizationNameLabel;
    @FXML private Label cityLabel;
    @FXML private Label openListingsCountLabel;
    @FXML private Label acceptedOffersCountLabel;
    @FXML private Label rescuedKgLabel;
    @FXML private VBox openListingsBox;
    @FXML private Label emptyListingsLabel;

    // PATTERN:DYNAMIC fills organization details and dashboard summaries
    @FXML
    private void initialize() {
        // TODO(service): organizationNameLabel.setText(account.getOrganizationName())
        // TODO(service): cityLabel.setText(account.getCity())
        // TODO(service): openListingsCountLabel.setText(listingService.countOpenInCity(account.getCity()))
        // TODO(service): acceptedOffersCountLabel.setText(offerService.countAcceptedForFoodGroup(...))
        // TODO(service): rescuedKgLabel.setText(listingService.sumRescuedKgForFoodGroup(...))
        // TODO(service): show emptyListingsLabel when there are no open listings in the group's city
        // TODO(service): add one row to openListingsBox per open listing in the group's city
    }
}
