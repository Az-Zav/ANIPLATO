package com.aniplato.controller;

import com.aniplato.model.Account.Role;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
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
        boolean foodGroup = RegistrationDraft.role() == Role.FOOD_GROUP;
        donorDocsBox.setVisible(!foodGroup);
        donorDocsBox.setManaged(!foodGroup);
        foodGroupDocsBox.setVisible(foodGroup);
        foodGroupDocsBox.setManaged(foodGroup);

        String orgType = RegistrationDraft.orgTypeLabel();
        roleBadgeLabel.setText("Required for: " + (foodGroup ? "Food group" : "Business donor")
                + (orgType.isBlank() ? "" : " · " + orgType));
        // TODO(service): read role and org type from the registration draft in RegistrationService
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