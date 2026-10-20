package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.City;
import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class RegisterController {

    // FXML: role toggle (Donor / Food group)
    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton donorToggle;
    @FXML private ToggleButton foodGroupToggle;
    // FXML: the permits question, shown for donors only. permitsBox wraps two radios in one ToggleGroup:
    //   hasPermitsRadio = "I have a Mayor's Permit and Sanitary Permit" (Business)
    //   noPermitsRadio  = "I don't have permits" (Casual)
    @FXML private VBox permitsBox;
    @FXML private ToggleGroup permitsGroup;
    @FXML private RadioButton hasPermitsRadio;
    @FXML private RadioButton noPermitsRadio;
    // FXML: orgNameField holds the organization, person or household name (neutral wording). typeCombo is removed.
    @FXML private TextField orgNameField;
    // FXML: cityCombo is filled from the service; remove its sample items
    @FXML private ComboBox<City> cityCombo;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    private final RegistrationService registrationService = new RegistrationService();

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // Pure UI state: a segment button can be clicked off; keep one role selected
        roleGroup.selectedToggleProperty().addListener((obs, previous, current) -> {
            if (current == null && previous != null) previous.setSelected(true);
        });
        // Pure UI state: the permits question belongs to donors only
        permitsBox.visibleProperty().bind(donorToggle.selectedProperty());
        permitsBox.managedProperty().bind(donorToggle.selectedProperty());

        cityCombo.getItems().setAll(registrationService.cities());

        Registration draft = RegistrationDraft.get();   // Back from step 2 keeps what was typed
        if (draft != null) {
            foodGroupToggle.setSelected(draft.role == Account.Role.FOOD_GROUP);
            hasPermitsRadio.setSelected(draft.role == Account.Role.BUSINESS_DONOR);
            noPermitsRadio.setSelected(draft.role == Account.Role.CASUAL_DONOR);
            orgNameField.setText(draft.name);
            cityCombo.setValue(draft.city);
            contactPersonField.setText(draft.contactPerson);
            contactNumberField.setText(draft.contactPhone);
            emailField.setText(draft.email);
        }
    }

    // FXML: nextBtn onAction="#handleNext"
    // PATTERN:ACTION
    @FXML
    private void handleNext() {
        Registration form = new Registration();
        if (foodGroupToggle.isSelected()) form.role = Account.Role.FOOD_GROUP;
        else if (hasPermitsRadio.isSelected()) form.role = Account.Role.BUSINESS_DONOR;
        else if (noPermitsRadio.isSelected()) form.role = Account.Role.CASUAL_DONOR;   // none chosen: role stays null
        form.name = orgNameField.getText();
        form.city = cityCombo.getValue();
        form.contactPerson = contactPersonField.getText();
        form.contactPhone = contactNumberField.getText();
        form.email = emailField.getText();
        try {
            registrationService.checkDetails(form);
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());   // A8
            return;
        }
        RegistrationDraft.set(form);
        Navigator.show(Screen.REGISTER_STEP2);
    }
}
