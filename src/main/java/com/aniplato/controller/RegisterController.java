package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.City;
import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
import com.aniplato.util.Settings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class RegisterController {

    // Nested Dialog Controller and Overlay Container
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Role Buttons
    @FXML private Button donorRoleBtn;
    @FXML private Button foodGroupRoleBtn;

    // Permits Containers & Radio Buttons
    @FXML private ToggleGroup permitsToggleGroup;
    @FXML private VBox permitsBox;
    @FXML private HBox permitCardHas;
    @FXML private HBox permitCardNo;
    @FXML private RadioButton hasPermitsRadio;
    @FXML private RadioButton noPermitsRadio;

    // Labels that change with the role
    @FXML private Label nameLabel;
    @FXML private Label nameHintLabel;
    @FXML private Label cityHintLabel;

    // Form Inputs
    @FXML private TextField businessNameField;
    @FXML private ComboBox<City> cityComboBox;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    // FXML: errorLabel (style class "error-text") shows a failed check in place, instead of a dialog (A8)
    @FXML private Label errorLabel;

    private final RegistrationService registrationService = new RegistrationService();
    private final FilteredList<City> visibleCities = new FilteredList<>(FXCollections.observableArrayList());
    private boolean isDonorSelected = true;

    @FXML
    private void initialize() {
        loadCities();
        // the contact number field takes digits only, up to the allowed length
        contactNumberField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0," + Settings.CONTACT_NUMBER_DIGITS + "}") ? change : null));
        restoreDraft();
        updatePermitCardStyles();
        updateFormForRole();
        // the message goes away as soon as the user edits a field
        for (TextField field : new TextField[] {businessNameField, contactPersonField, contactNumberField, emailField}) {
            field.textProperty().addListener((o, was, now) -> hideError());
        }
        cityComboBox.valueProperty().addListener((o, was, now) -> hideError());
    }

    /** The city field is searchable: typing narrows the list to the cities whose name contains the text. */
    private void loadCities() {
        cityComboBox.setItems(visibleCities);
        cityComboBox.setVisibleRowCount(6);
        cityComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(City city) {
                return city == null ? "" : city.name;
            }

            @Override
            public City fromString(String text) {
                return cityNamed(text);   // null until the text is exactly a city's name
            }
        });
        cityComboBox.getEditor().textProperty().addListener((o, was, typed) -> {
            City chosen = cityComboBox.getValue();
            if (chosen != null && chosen.name.equals(typed)) return;   // text was filled by picking; keep the full list
            String needle = typed == null ? "" : typed.trim().toLowerCase();
            visibleCities.setPredicate(city -> city.name.toLowerCase().contains(needle));
            if (cityComboBox.isFocused() && !visibleCities.isEmpty() && !needle.isEmpty()) cityComboBox.show();
        });
        // the list opens by itself: on focus, on a click in the field, and while typing
        cityComboBox.getEditor().focusedProperty().addListener((o, was, focused) -> {
            if (focused && !visibleCities.isEmpty()) cityComboBox.show();
        });
        cityComboBox.getEditor().setOnMousePressed(e -> {
            if (!cityComboBox.isShowing() && !visibleCities.isEmpty()) cityComboBox.show();
        });
        try {
            ((ObservableList<City>) visibleCities.getSource()).setAll(registrationService.cities());
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            showError("Could not load the city list. Check the database connection and reopen this screen.");
        }
    }

    /** Coming back from step 2 (Back button): put the typed details and the role choice back. */
    private void restoreDraft() {
        Registration draft = RegistrationDraft.get();
        if (draft == null) return;
        if (draft.role == Account.Role.FOOD_GROUP) {
            handleSelectFoodGroup();
        } else {
            handleSelectDonor();
            (draft.role == Account.Role.BUSINESS_DONOR ? hasPermitsRadio : noPermitsRadio).setSelected(true);
        }
        businessNameField.setText(draft.name);
        cityComboBox.setValue(draft.city);
        contactPersonField.setText(draft.contactPerson);
        contactNumberField.setText(draft.contactPhone);
        emailField.setText(draft.email);
    }

    // PATTERN:ACTION updates permit card styles so only the selected item is green
    @FXML
    private void handlePermitSelectionChanged() {
        updatePermitCardStyles();
        updateFormForRole();
    }

    /** Business donor, Casual donor or Food group changes the wording of the form (spec 6.2 Stage 1). */
    private void updateFormForRole() {
        if (permitsBox == null) return;
        permitsBox.setVisible(isDonorSelected);
        permitsBox.setManaged(isDonorSelected);
        if (!isDonorSelected) {
            setRoleText("Organization name", "As shown on your credential", "e.g., Feeding Manila Foundation",
                    "Must match your registered address.", "e.g., Maria Santos");
        } else if (hasPermitsRadio.isSelected()) {
            setRoleText("Business name", "As shown on your permits", "e.g., Manila Artisanal Bakeshop",
                    "Must match the city on your Mayor's Permit.", "e.g., Maria Santos");
        } else {
            setRoleText("Your name or household name", "As shown on your Barangay Certificate", "e.g., Santos Household",
                    "Must match the city on your Barangay Certificate.", "Your own name");
        }
    }

    private void setRoleText(String name, String nameHint, String namePrompt, String cityHint, String contactPrompt) {
        nameLabel.setText(name);
        nameHintLabel.setText(nameHint);
        businessNameField.setPromptText(namePrompt);
        cityHintLabel.setText(cityHint);
        contactPersonField.setPromptText(contactPrompt);
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
        updateFormForRole();
    }

    // PATTERN:ACTION switches role selection to Food Group
    @FXML
    private void handleSelectFoodGroup() {
        isDonorSelected = false;
        foodGroupRoleBtn.getStyleClass().removeAll("secondary-button");
        foodGroupRoleBtn.getStyleClass().add("primary-button");

        donorRoleBtn.getStyleClass().removeAll("primary-button");
        donorRoleBtn.getStyleClass().add("secondary-button");
        updateFormForRole();
    }



    // PATTERN:NAV -> routes to Login screen
    @FXML
    private void handleNavLogin() {
        Navigator.show(Screen.LOGIN);
    }

    // PATTERN:ACTION validates step 1 inputs and routes to Step 2 Documents
    @FXML
    private void handleNextStep2() {
        hideError();
        Registration form = new Registration();
        form.role = selectedRole();
        form.name = businessNameField.getText();
        form.city = cityNamed(cityComboBox.getEditor().getText());   // typed text counts only if it is a real city
        form.contactPerson = contactPersonField.getText();
        form.contactPhone = contactNumberField.getText();
        form.email = emailField.getText();
        try {
            registrationService.checkDetails(form);   // R1 rules live in the service (A1)
        } catch (RuleViolationException e) {
            showError(e.getMessage());   // A8
            return;
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            showError("Could not check your details. Check the database connection and try again.");
            return;
        }
        RegistrationDraft.set(form);   // step 2 adds the documents and submits
        Navigator.show(Screen.REGISTER_STEP2);
    }

    /** The city whose name is exactly {@code text} (ignoring case), or null. */
    private City cityNamed(String text) {
        if (text == null) return null;
        String wanted = text.trim();
        return visibleCities.getSource().stream().filter(c -> c.name.equalsIgnoreCase(wanted)).findFirst().orElse(null);
    }

    /** Food group, or a donor with ("I have permits") or without permits: Business or Casual. */
    private Account.Role selectedRole() {
        if (!isDonorSelected) return Account.Role.FOOD_GROUP;
        return hasPermitsRadio.isSelected() ? Account.Role.BUSINESS_DONOR : Account.Role.CASUAL_DONOR;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}