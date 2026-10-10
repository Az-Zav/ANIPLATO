package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class RegisterStep2Controller {

    @FXML private Label roleBadgeLabel;
    @FXML private VBox donorDocsBox;
    @FXML private VBox foodGroupDocsBox;
    @FXML private CheckBox consentCheckBox;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): roleBadgeLabel.setText("Required for: " + role + " · " + orgType)
        // TODO(service): show donorDocsBox or foodGroupDocsBox by the registration's role (setVisible + setManaged)
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleSubmit() {
        // TODO(service): registrationService.submit(...) checks consent and R1.1-R1.4;
        //                a RuleViolationException is shown as a message (A8)
        Navigator.show(Screen.REGISTER_SUBMITTED);
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleOpenTerms() {
        // TODO: open the Terms and Conditions (no terms file exists yet)
    }
}
