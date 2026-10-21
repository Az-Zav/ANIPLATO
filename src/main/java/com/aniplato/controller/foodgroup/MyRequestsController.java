package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

/** Presentation-only request list until the Route 2 service is delivered. */
public class MyRequestsController {

    // PATTERN:ACTION placeholder: opens the selected request's incoming offers
    @FXML
    private void handleReviewOffers() {
        // TODO(service): load selected request offers
        Navigator.show(Navigator.Screen.FOODGROUP_OFFERS_RECEIVED);
    }

    // PATTERN:ACTION placeholder: opens receipt confirmation/report flow
    @FXML
    private void handleConfirmReceipt() {
        // TODO(service): confirm receipt for selected accepted offer
        Navigator.show(Navigator.Screen.FOODGROUP_SUBMIT_REPORT);
    }
}
