package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

public class MyOffersController {

    @FXML
    private void initialize() {
        // TODO(service): load the food group's offers and summary metrics
        // TODO(service): populate offer rows and apply the selected status filter
    }

    // PATTERN:ACTION placeholder: confirms receipt through the handoff flow
    @FXML
    private void handleConfirmReceipt() {
        Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }

    // PATTERN:ACTION placeholder: withdraws the selected offer
    @FXML
    private void handleWithdraw() {
        // TODO(service): open the shared withdraw confirmation flow
    }
}
