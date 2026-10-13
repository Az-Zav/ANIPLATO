package com.aniplato.controller.admin;

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
        // TODO(service): summaryLabel.setText(pendingCount + " registrations awaiting review · " + registeredAgainCount + " registered again")  -- counts are derived (A9)

        // PATTERN:LIST fills rows
        // TODO(service): replace the sample row in pendingRowsBox with one row per pending registration, oldest submitted_at first;
        //                per row: org name, tag-new or tag-reregistered (earlier rejected registration with the same email),
        //                "{uploaded} of {required} docs" from requiredDocuments(), role pill, type, city, submitted date.
        //                No phone numbers in this list (A7).
        // TODO(service): each row's Review button calls Navigator.show(Screen.REVIEW_REGISTRATION) for that registration
        // TODO(service): emptyLabel.setVisible(isEmpty) and emptyLabel.setManaged(isEmpty)
    }
}