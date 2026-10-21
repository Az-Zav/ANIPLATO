package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

/** Presentation-only request form until the Route 2 service is delivered. */
public class NewRequestController {

    // PATTERN:ACTION placeholder: creates a request once the Route 2 service exists
    @FXML
    private void handlePost() {
        // TODO(service): postService.createRequest(...)
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }

    // PATTERN:NAV -> MY_REQUESTS
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }
}
