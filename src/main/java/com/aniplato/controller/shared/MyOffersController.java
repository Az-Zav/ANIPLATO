package com.aniplato.controller.shared;

import com.aniplato.model.Account;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shared offers destination whose copy can adapt to the signed-in role. */
public class MyOffersController {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;

    @FXML
    private void initialize() {
        Account.Role role = Navigator.currentRole();
        if (role == Account.Role.BUSINESS_DONOR || role == Account.Role.CASUAL_DONOR) {
            pageTitle.setText("My Offers");
            pageSubtitle.setText("Track offers you have made on available requests.");
        } else if (role == Account.Role.FOOD_GROUP) {
            pageTitle.setText("My Offers");
            pageSubtitle.setText("Track your offers and accepted food pickups.");
        } else {
            pageTitle.setText("My Offers");
            pageSubtitle.setText("Review offers associated with your account.");
        }
    }

    // PATTERN:NAV -> DONOR_BROWSE_REQUESTS
    @FXML
    private void handleBrowseRequests() {
        Navigator.show(Navigator.Screen.DONOR_BROWSE_REQUESTS);
    }

    // PATTERN:ACTION placeholder: opens the shared handoff confirmation flow
    @FXML
    private void handleConfirmReceipt() {
        Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }
}
