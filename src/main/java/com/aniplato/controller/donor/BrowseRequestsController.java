package com.aniplato.controller.donor;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

public class BrowseRequestsController {

    @FXML
    private void initialize() {
        // TODO(service): load open food-group requests for the donor's city
    }

    // PATTERN:ACTION placeholder: refreshes request data
    @FXML
    private void handleRefresh() {
        // TODO(service): reload open requests and summary metrics
    }

    // PATTERN:ACTION placeholder: opens the selected request offer flow
    @FXML
    private void handleMakeOffer() {
        Navigator.show(Navigator.Screen.DONOR_MAKE_OFFER);
    }
}
