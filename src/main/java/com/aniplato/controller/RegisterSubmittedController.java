package com.aniplato.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RegisterSubmittedController {

    @FXML private Label orgSubheadLabel;
    @FXML private Label emailNoticeLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): orgSubheadLabel.setText(orgName + " · " + city)
        // TODO(service): emailNoticeLabel.setText("We sent a confirmation to " + email + ". No account exists yet, so there's nothing to log in to.")
    }
}
