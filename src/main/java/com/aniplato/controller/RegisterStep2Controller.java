package com.aniplato.controller;

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

    @FXML private Label roleBadgeLabel;
    @FXML private VBox donorDocsBox;
    @FXML private VBox foodGroupDocsBox;
    @FXML private CheckBox consentCheckBox;

    // Business donor documents
    @FXML private ComboBox<String> businessRegTypeCombo;
    @FXML private HBox businessRegFileBox;
    @FXML private Label businessRegFileLabel;
    @FXML private Button businessRegUploadBtn;
    @FXML private DatePicker businessRegExpiryPicker;
    @FXML private CheckBox businessRegNoExpiryCheck;

    @FXML private HBox mayorsPermitFileBox;
    @FXML private Label mayorsPermitFileLabel;
    @FXML private Button mayorsPermitUploadBtn;
    @FXML private DatePicker mayorsPermitExpiryPicker;

    @FXML private HBox sanitaryPermitFileBox;
    @FXML private Label sanitaryPermitFileLabel;
    @FXML private Button sanitaryPermitUploadBtn;
    @FXML private DatePicker sanitaryPermitExpiryPicker;

    @FXML private HBox fdaLicenseFileBox;
    @FXML private Label fdaLicenseFileLabel;
    @FXML private Button fdaLicenseUploadBtn;
    @FXML private DatePicker fdaLicenseExpiryPicker;

    // Food group documents
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

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // Pure UI state: "No expiry" turns its date picker off
        businessRegExpiryPicker.disableProperty().bind(businessRegNoExpiryCheck.selectedProperty());
        credentialExpiryPicker.disableProperty().bind(credentialNoExpiryCheck.selectedProperty());
        authLetterExpiryPicker.disableProperty().bind(authLetterNoExpiryCheck.selectedProperty());

        // TODO(service): roleBadgeLabel.setText("Required for: " + role + " · " + orgType)
        // TODO(service): show donorDocsBox or foodGroupDocsBox by the registration's role (setVisible + setManaged)
        // TODO(service): per document, if a file is uploaded: <doc>FileLabel.setText(fileName) and show <doc>FileBox,
        //                otherwise show <doc>UploadBtn (setVisible + setManaged on both) -- R1.1
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

    // PATTERN:ACTION placeholder: does nothing yet (upload and Replace share one handler per document)
    @FXML
    private void handleUploadBusinessReg() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1, 5 MB PDF/JPG/PNG
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleUploadMayorsPermit() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleUploadSanitaryPermit() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleUploadFdaLicense() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1 (optional document)
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleUploadCredential() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleUploadAuthLetter() {
        // TODO(service): choose a file, then fileStorageService.save(...) -- R1.1
    }
}