package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
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
        // Pure UI state: a segment button can be clicked off; keep one role selected
        roleGroup.selectedToggleProperty().addListener((obs, previous, current) -> {
            if (current == null && previous != null) previous.setSelected(true);
        });
        // TODO(service): select donorToggle or foodGroupToggle from the saved draft (so Back keeps the role)
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleNext() {
        // TODO(service): build a Registration from the form (role: foodGroupToggle selected = FOOD_GROUP, else DONOR)
        //                and carry it to step 2, which shows the donor or food group documents from it
        // TODO(service): fill typeCombo and cityCombo from the service instead of FXML sample items
        Navigator.show(Screen.REGISTER_STEP2);
    }
}