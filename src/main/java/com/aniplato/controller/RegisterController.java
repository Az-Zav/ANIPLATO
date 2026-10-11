package com.aniplato.controller;

import com.aniplato.model.Account.Role;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class RegisterController {

    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton donorToggle;
    @FXML private ToggleButton foodGroupToggle;
    @FXML private TextField orgNameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> cityCombo;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // Back from step 2 reloads this screen: restore the role picked earlier
        ToggleButton picked = RegistrationDraft.role() == Role.FOOD_GROUP ? foodGroupToggle : donorToggle;
        picked.setSelected(true);
        // A segment button can be clicked off; keep one role selected at all times
        roleGroup.selectedToggleProperty().addListener((obs, previous, current) -> {
            if (current == null && previous != null) previous.setSelected(true);
        });
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleNext() {
        // Step 2 shows the donor or the food group documents from these two values
        RegistrationDraft.setRole(roleGroup.getSelectedToggle() == foodGroupToggle ? Role.FOOD_GROUP : Role.DONOR);
        RegistrationDraft.setOrgTypeLabel(typeCombo.getValue());
        // TODO(service): build a Registration from the form (role from roleGroup) and carry it to step 2
        // TODO(service): fill typeCombo and cityCombo from the service instead of FXML sample items
        Navigator.show(Screen.REGISTER_STEP2);
    }
}