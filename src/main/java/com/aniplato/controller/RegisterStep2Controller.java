package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegisterStep2Controller {

    public enum Mode {
        BUSINESS_DONOR,
        CASUAL_DONOR,
        FOOD_GROUP
    }

    // Embedded Dialog Overlay
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Header Controls
    @FXML private Label instructionsLabel;
    @FXML private Label roleBadgeLabel;

    // 1. Business Donor Controls
    @FXML private VBox businessDonorDocsBox;
    @FXML private HBox mayorsPermitFileBox;
    @FXML private Label mayorsPermitFileLabel;
    @FXML private Button mayorsPermitUploadBtn;
    @FXML private DatePicker mayorsPermitExpiryPicker;
    @FXML private HBox sanitaryPermitFileBox;
    @FXML private Label sanitaryPermitFileLabel;
    @FXML private Button sanitaryPermitUploadBtn;
    @FXML private DatePicker sanitaryPermitExpiryPicker;

    // 2. Casual Donor Controls
    @FXML private VBox casualDonorDocsBox;
    @FXML private HBox barangayCertFileBox;
    @FXML private Label barangayCertFileLabel;
    @FXML private Button barangayCertUploadBtn;
    @FXML private DatePicker barangayCertExpiryPicker;

    // 3. Food Group Controls
    @FXML private VBox foodGroupDocsBox;
    @FXML private ComboBox<String> credentialTypeCombo;
    @FXML private HBox credentialFileBox;
    @FXML private Label credentialFileLabel;
    @FXML private Button credentialUploadBtn;
    @FXML private DatePicker credentialExpiryPicker;
    @FXML private CheckBox credentialNoExpiryCheck;

    @FXML private HBox authLetterFileBox;
    @FXML private Label authLetterFileLabel;
    @FXML private Button authLetterUploadBtn;
    @FXML private DatePicker authLetterExpiryPicker;
    @FXML private CheckBox authLetterNoExpiryCheck;

    // Consent & Navigation
    @FXML private CheckBox consentCheckBox;
    @FXML private Button backBtn;
    @FXML private Button submitBtn;

    @FXML
    private void initialize() {
        // Default mode is Food Group as requested
        setRegistrationMode(Mode.FOOD_GROUP);
    }

    /** Switches view between Business Donor, Casual Donor, and Food Group modes */
    public void setRegistrationMode(Mode mode) {
        businessDonorDocsBox.setVisible(false);
        businessDonorDocsBox.setManaged(false);
        casualDonorDocsBox.setVisible(false);
        casualDonorDocsBox.setManaged(false);
        foodGroupDocsBox.setVisible(false);
        foodGroupDocsBox.setManaged(false);

        switch (mode) {
            case FOOD_GROUP -> {
                roleBadgeLabel.setText("Required for: Food group");
                instructionsLabel.setText("Upload one file for each item and type the expiry date printed on it, or tick No expiry where the document doesn't expire. Don't upload government IDs.");
                foodGroupDocsBox.setVisible(true);
                foodGroupDocsBox.setManaged(true);
            }
            case CASUAL_DONOR -> {
                roleBadgeLabel.setText("Required for: Casual donor");
                instructionsLabel.setText("The Barangay Certificate always needs an expiry date. Don't upload government IDs.");
                casualDonorDocsBox.setVisible(true);
                casualDonorDocsBox.setManaged(true);
            }
            case BUSINESS_DONOR -> {
                roleBadgeLabel.setText("Required for: Business donor");
                instructionsLabel.setText("Upload one file for each permit and type the expiry date printed on it. Permits always need a date. Don't upload government IDs.");
                businessDonorDocsBox.setVisible(true);
                businessDonorDocsBox.setManaged(true);
            }
        }
    }

    // Toggle Handlers for 'No expiry' Checkboxes
    @FXML
    private void handleCredentialNoExpiryToggle() {
        boolean noExpiry = credentialNoExpiryCheck.isSelected();
        credentialExpiryPicker.setDisable(noExpiry);
        if (noExpiry) {
            credentialExpiryPicker.setValue(null);
            credentialExpiryPicker.setPromptText("No expiry");
        } else {
            credentialExpiryPicker.setPromptText("Expiry date");
        }
    }

    @FXML
    private void handleAuthLetterNoExpiryToggle() {
        boolean noExpiry = authLetterNoExpiryCheck.isSelected();
        authLetterExpiryPicker.setDisable(noExpiry);
        if (noExpiry) {
            authLetterExpiryPicker.setValue(null);
            authLetterExpiryPicker.setPromptText("No expiry");
        } else {
            authLetterExpiryPicker.setPromptText("Expiry date");
        }
    }

    // Upload Action Stubs
    @FXML private void handleUploadMayorsPermit() {}
    @FXML private void handleUploadSanitaryPermit() {}
    @FXML private void handleUploadBarangayCert() {}
    @FXML private void handleUploadCredential() {}
    @FXML private void handleUploadAuthLetter() {}

    // Navigation & Submission
    @FXML
    private void handleBackToStep1() {
        Navigator.show(Screen.REGISTER);
    }

    @FXML
    private void handleConfirmSubmit() {
        if (!consentCheckBox.isSelected()) {
            return;
        }
        Navigator.show(Screen.REGISTER_SUBMITTED);
    }
}