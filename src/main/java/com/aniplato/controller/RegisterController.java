package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegisterController {

    // Nested Dialog Controller and Overlay Container
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Role Buttons
    @FXML private Button donorRoleBtn;
    @FXML private Button foodGroupRoleBtn;

    // Permits Containers & Radio Buttons
    @FXML private ToggleGroup permitsToggleGroup;
    @FXML private HBox permitCardHas;
    @FXML private HBox permitCardNo;
    @FXML private RadioButton hasPermitsRadio;
    @FXML private RadioButton noPermitsRadio;

    // Form Inputs
    @FXML private TextField businessNameField;
    @FXML private ComboBox<String> cityComboBox;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    private boolean isDonorSelected = true;

    @FXML
    private void initialize() {
        if (cityComboBox != null) {
            cityComboBox.getItems().addAll(
                    "Pasig City", "Mandaluyong City", "Marikina City", "Quezon City", "Manila", "Taguig City"
            );
        }
        updatePermitCardStyles();
    }

    // PATTERN:ACTION updates permit card styles so only the selected item is green
    @FXML
    private void handlePermitSelectionChanged() {
        updatePermitCardStyles();
    }

    private void updatePermitCardStyles() {
        if (hasPermitsRadio == null || noPermitsRadio == null) return;

        if (hasPermitsRadio.isSelected()) {
            permitCardHas.getStyleClass().removeAll("permit-card-unselected");
            if (!permitCardHas.getStyleClass().contains("permit-card-selected")) {
                permitCardHas.getStyleClass().add("permit-card-selected");
            }

            permitCardNo.getStyleClass().removeAll("permit-card-selected");
            if (!permitCardNo.getStyleClass().contains("permit-card-unselected")) {
                permitCardNo.getStyleClass().add("permit-card-unselected");
            }
        } else {
            permitCardNo.getStyleClass().removeAll("permit-card-unselected");
            if (!permitCardNo.getStyleClass().contains("permit-card-selected")) {
                permitCardNo.getStyleClass().add("permit-card-selected");
            }

            permitCardHas.getStyleClass().removeAll("permit-card-selected");
            if (!permitCardHas.getStyleClass().contains("permit-card-unselected")) {
                permitCardHas.getStyleClass().add("permit-card-unselected");
            }
        }
    }

    // PATTERN:ACTION switches role selection to Donor
    @FXML
    private void handleSelectDonor() {
        isDonorSelected = true;
        donorRoleBtn.getStyleClass().removeAll("secondary-button");
        donorRoleBtn.getStyleClass().add("primary-button");

        foodGroupRoleBtn.getStyleClass().removeAll("primary-button");
        foodGroupRoleBtn.getStyleClass().add("secondary-button");
    }

    // PATTERN:ACTION switches role selection to Food Group
    @FXML
    private void handleSelectFoodGroup() {
        isDonorSelected = false;
        foodGroupRoleBtn.getStyleClass().removeAll("secondary-button");
        foodGroupRoleBtn.getStyleClass().add("primary-button");

        donorRoleBtn.getStyleClass().removeAll("primary-button");
        donorRoleBtn.getStyleClass().add("secondary-button");
    }



    // PATTERN:NAV -> routes to Login screen
    @FXML
    private void handleNavLogin() {
        Navigator.show(Screen.LOGIN);
    }

    // PATTERN:ACTION validates step 1 inputs and routes to Step 2 Documents
    @FXML
    private void handleNextStep2() {
        Navigator.show(Screen.REGISTER_STEP2);
    }
}