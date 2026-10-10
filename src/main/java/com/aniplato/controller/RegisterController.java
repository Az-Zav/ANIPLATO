package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public class RegisterController {

    @FXML private ToggleGroup roleGroup;
    @FXML private TextField orgNameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> cityCombo;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleNext() {
        // TODO(service): build a Registration from the form (role from roleGroup) and carry it to step 2
        // TODO(service): fill typeCombo and cityCombo from the service instead of FXML sample items
        Navigator.show(Screen.REGISTER_STEP2);
    }
}
