package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

/** Presentation-only offers view until the Route 2 service is delivered. */
public class FoodGroupIncomingOffersController {

    // PATTERN:NAV -> MY_REQUESTS
    @FXML
    private void handleBack() {
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }

    // PATTERN:ACTION placeholder: accepts the selected offer
    @FXML
    private void handleAcceptOffer() {
        // TODO(service): offerService.acceptOffer(...)
    }
}
