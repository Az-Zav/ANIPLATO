package com.aniplato.controller.admin;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PendingRegistrationsController {

    @FXML private Label summaryLabel;
    @FXML private VBox pendingRowsBox;
    @FXML private Label emptyLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): summaryLabel.setText(pendingCount + " registrations awaiting review · " + registeredAgainCount + " registered again")

        // PATTERN:LIST fills rows
        // TODO(service): replace sample rows in pendingRowsBox dynamically
        // TODO(service): emptyLabel.setVisible(isEmpty) and emptyLabel.setManaged(isEmpty)
    }

    // PATTERN:NAV -> REVIEW_REGISTRATION
    @FXML
    private void handleReviewRegistration() {
        Navigator.show(Screen.REVIEW_REGISTRATION);
    }
}