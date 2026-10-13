package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

public class BrowseListingsController {

    @FXML
    private void initialize() {
        // TODO(service): load open listings for the food group's city, soonest use-by first
        // TODO(service): populate the summary metrics and listing rows
    }

    // PATTERN:ACTION placeholder: refreshes the service-backed listing data
    @FXML
    private void handleRefresh() {
        // TODO(service): reload open listings and summary metrics
    }

    // PATTERN:ACTION placeholder: opens the selected listing's offer form
    @FXML
    private void handleMakeOffer() {
        Navigator.show(Navigator.Screen.MAKE_OFFER);
    }
}
