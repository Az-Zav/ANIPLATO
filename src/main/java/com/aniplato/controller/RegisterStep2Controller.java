package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.DocumentUpload;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegisterStep2Controller {

    // FXML: one documents box per role; the controller shows the one that matches the form from step 1
    @FXML private Label roleBadgeLabel;
    @FXML private VBox donorDocsBox;        // Business donor: Mayor's Permit + Sanitary Permit
    @FXML private VBox casualDocsBox;       // NEW: Casual donor: Barangay Certificate
    @FXML private VBox foodGroupDocsBox;    // Food group: one credential + authorization letter
    @FXML private CheckBox consentCheckBox;

    // Business donor documents. Permits have no "No expiry" box: they always need a date.
    // FXML: remove the old business registration (DTI/SEC/CDA) and FDA blocks.
    @FXML private HBox mayorsPermitFileBox;
    @FXML private Label mayorsPermitFileLabel;
    @FXML private Button mayorsPermitUploadBtn;
    @FXML private DatePicker mayorsPermitExpiryPicker;

    @FXML private HBox sanitaryPermitFileBox;
    @FXML private Label sanitaryPermitFileLabel;
    @FXML private Button sanitaryPermitUploadBtn;
    @FXML private DatePicker sanitaryPermitExpiryPicker;

    // FXML (NEW): Casual donor's Barangay Certificate, same four parts as the permits, no "No expiry" box
    @FXML private HBox barangayCertFileBox;
    @FXML private Label barangayCertFileLabel;
    @FXML private Button barangayCertUploadBtn;
    @FXML private DatePicker barangayCertExpiryPicker;

    // Food group documents
    @FXML private ComboBox<Document.Type> credentialTypeCombo;   // FXML: items come from the controller; remove sample items
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

    private final RegistrationService registrationService = new RegistrationService();
    private File mayorsPermitFile;
    private File sanitaryPermitFile;
    private File barangayCertFile;
    private File credentialFile;
    private File authLetterFile;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // Pure UI state: "No expiry" turns its date picker off
        credentialExpiryPicker.disableProperty().bind(credentialNoExpiryCheck.selectedProperty());
        authLetterExpiryPicker.disableProperty().bind(authLetterNoExpiryCheck.selectedProperty());
        credentialTypeCombo.getItems().setAll(Document.Type.FOOD_GROUP_CREDENTIALS);

        Registration form = RegistrationDraft.get();
        roleBadgeLabel.setText("Required for: " + form.role.displayName());
        show(donorDocsBox, form.role == Account.Role.BUSINESS_DONOR);
        show(casualDocsBox, form.role == Account.Role.CASUAL_DONOR);
        show(foodGroupDocsBox, form.role == Account.Role.FOOD_GROUP);
    }

    // FXML: submitBtn onAction="#handleSubmit"
    // PATTERN:ACTION
    @FXML
    private void handleSubmit() {
        Registration form = RegistrationDraft.get();
        List<DocumentUpload> uploads = new ArrayList<>();
        switch (form.role) {
            case BUSINESS_DONOR -> {
                add(uploads, Document.Type.MAYORS_PERMIT, mayorsPermitFile, mayorsPermitExpiryPicker.getValue(), false);
                add(uploads, Document.Type.SANITARY_PERMIT, sanitaryPermitFile, sanitaryPermitExpiryPicker.getValue(), false);
            }
            case CASUAL_DONOR ->
                add(uploads, Document.Type.BARANGAY_CERTIFICATE, barangayCertFile, barangayCertExpiryPicker.getValue(), false);
            case FOOD_GROUP -> {
                add(uploads, credentialTypeCombo.getValue(), credentialFile, credentialExpiryPicker.getValue(),
                        credentialNoExpiryCheck.isSelected());
                add(uploads, Document.Type.AUTHORIZATION_LETTER, authLetterFile, authLetterExpiryPicker.getValue(),
                        authLetterNoExpiryCheck.isSelected());
            }
            default -> throw new IllegalStateException("Unexpected role: " + form.role);
        }
        try {
            registrationService.submit(form, uploads, consentCheckBox.isSelected());
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());   // A8
            return;
        }
        Navigator.show(Screen.REGISTER_SUBMITTED);
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleOpenTerms() {
        // TODO: open the Terms and Conditions (no terms file exists yet)
    }

    // Each upload button (and the Replace button beside the file name) shares one handler per document.
    // The service checks type and size when the form is submitted.
    // PATTERN:ACTION
    @FXML
    private void handleUploadMayorsPermit() {
        mayorsPermitFile = pick("Mayor's Permit", mayorsPermitFile, mayorsPermitFileLabel, mayorsPermitFileBox, mayorsPermitUploadBtn);
    }

    // PATTERN:ACTION
    @FXML
    private void handleUploadSanitaryPermit() {
        sanitaryPermitFile = pick("Sanitary Permit", sanitaryPermitFile, sanitaryPermitFileLabel, sanitaryPermitFileBox, sanitaryPermitUploadBtn);
    }

    // FXML (NEW): barangayCertUploadBtn and the Replace button onAction="#handleUploadBarangayCert"
    // PATTERN:ACTION
    @FXML
    private void handleUploadBarangayCert() {
        barangayCertFile = pick("Barangay Certificate", barangayCertFile, barangayCertFileLabel, barangayCertFileBox, barangayCertUploadBtn);
    }

    // PATTERN:ACTION
    @FXML
    private void handleUploadCredential() {
        credentialFile = pick("Credential", credentialFile, credentialFileLabel, credentialFileBox, credentialUploadBtn);
    }

    // PATTERN:ACTION
    @FXML
    private void handleUploadAuthLetter() {
        authLetterFile = pick("Authorization Letter", authLetterFile, authLetterFileLabel, authLetterFileBox, authLetterUploadBtn);
    }

    /** Opens the file chooser. Keeps the previous file if the user cancels. Shows the name beside a Replace button. */
    private File pick(String title, File previous, Label fileLabel, HBox fileBox, Button uploadBtn) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose your " + title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        File chosen = chooser.showOpenDialog(fileBox.getScene().getWindow());
        if (chosen == null) return previous;
        fileLabel.setText(chosen.getName());
        show(fileBox, true);
        show(uploadBtn, false);
        return chosen;
    }

    /** A document the user left out is skipped here; the service then asks for it by name. */
    private static void add(List<DocumentUpload> uploads, Document.Type type, File file, LocalDate expiry, boolean noExpiry) {
        if (type != null && file != null) uploads.add(new DocumentUpload(type, file, expiry, noExpiry));
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
