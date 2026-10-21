package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.service.FileStorageService;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.DocumentUpload;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.RegistrationDraft;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    @FXML private ComboBox<Document.Type> credentialTypeCombo;
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

    // FXML: errorLabel (style class "error-text") shows a failed check in place, instead of a dialog (A8)
    @FXML private Label errorLabel;

    /** One document row: its file pill, upload button, expiry picker and (where the row has one) the No expiry box. */
    private static final class Slot {
        final HBox fileBox;
        final Label fileLabel;
        final Button uploadBtn;
        final DatePicker expiryPicker;
        final CheckBox noExpiryCheck;   // null for rows that always need a date
        Document.Type type;             // fixed for permits and certificates; follows the combo for the credential
        File file;

        Slot(Document.Type type, HBox fileBox, Label fileLabel, Button uploadBtn, DatePicker expiryPicker, CheckBox noExpiryCheck) {
            this.type = type;
            this.fileBox = fileBox;
            this.fileLabel = fileLabel;
            this.uploadBtn = uploadBtn;
            this.expiryPicker = expiryPicker;
            this.noExpiryCheck = noExpiryCheck;
        }
    }

    private static File lastFolder;   // the file explorer reopens where the last upload came from

    private final RegistrationService registrationService = new RegistrationService();
    private final FileStorageService fileStorageService = new FileStorageService();
    private Slot mayorsPermit;
    private Slot sanitaryPermit;
    private Slot barangayCert;
    private Slot credential;
    private Slot authLetter;
    private Mode mode = Mode.FOOD_GROUP;

    @FXML
    private void initialize() {
        mayorsPermit = new Slot(Document.Type.MAYORS_PERMIT, mayorsPermitFileBox, mayorsPermitFileLabel,
                mayorsPermitUploadBtn, mayorsPermitExpiryPicker, null);
        sanitaryPermit = new Slot(Document.Type.SANITARY_PERMIT, sanitaryPermitFileBox, sanitaryPermitFileLabel,
                sanitaryPermitUploadBtn, sanitaryPermitExpiryPicker, null);
        barangayCert = new Slot(Document.Type.BARANGAY_CERTIFICATE, barangayCertFileBox, barangayCertFileLabel,
                barangayCertUploadBtn, barangayCertExpiryPicker, null);
        credential = new Slot(null, credentialFileBox, credentialFileLabel,
                credentialUploadBtn, credentialExpiryPicker, credentialNoExpiryCheck);
        authLetter = new Slot(Document.Type.AUTHORIZATION_LETTER, authLetterFileBox, authLetterFileLabel,
                authLetterUploadBtn, authLetterExpiryPicker, authLetterNoExpiryCheck);

        credentialTypeCombo.getItems().setAll(Document.Type.FOOD_GROUP_CREDENTIALS);
        credentialTypeCombo.valueProperty().addListener((o, was, now) -> {
            credential.type = now;
            hideError();
        });

        for (Slot slot : List.of(mayorsPermit, sanitaryPermit, barangayCert, credential, authLetter)) {
            clearSlot(slot);   // the FXML holds sample file names; start every row empty
            if (slot.noExpiryCheck == null) slot.expiryPicker.setPromptText("Expiry date");
            slot.expiryPicker.valueProperty().addListener((o, was, now) -> hideError());
        }
        credentialExpiryPicker.setPromptText("Expiry date");
        authLetterExpiryPicker.setPromptText("No expiry");   // its No expiry box starts ticked, as in the FXML

        Registration draft = RegistrationDraft.get();
        if (draft == null) {
            // reached without step 1: go back to it once this screen has finished loading
            Platform.runLater(() -> Navigator.show(Screen.REGISTER));
            setRegistrationMode(Mode.FOOD_GROUP);
            return;
        }
        setRegistrationMode(switch (draft.role) {
            case BUSINESS_DONOR -> Mode.BUSINESS_DONOR;
            case CASUAL_DONOR -> Mode.CASUAL_DONOR;
            default -> Mode.FOOD_GROUP;
        });
        restoreUploads();
    }

    /** Coming back from step 1 (Back button): show the files and dates the user had already chosen. */
    private void restoreUploads() {
        for (DocumentUpload upload : RegistrationDraft.uploads()) {
            Slot slot = slotFor(upload.type());
            if (slot == null) continue;
            if (slot == credential) credentialTypeCombo.setValue(upload.type());
            slot.file = upload.file();
            showFile(slot);
            if (slot.noExpiryCheck != null) {
                slot.noExpiryCheck.setSelected(upload.noExpiry());
                slot.expiryPicker.setDisable(upload.noExpiry());
            }
            slot.expiryPicker.setValue(upload.expiresOn());
        }
    }

    private Slot slotFor(Document.Type type) {
        if (type == Document.Type.MAYORS_PERMIT) return mayorsPermit;
        if (type == Document.Type.SANITARY_PERMIT) return sanitaryPermit;
        if (type == Document.Type.BARANGAY_CERTIFICATE) return barangayCert;
        if (type == Document.Type.AUTHORIZATION_LETTER) return authLetter;
        return Document.Type.FOOD_GROUP_CREDENTIALS.contains(type) ? credential : null;
    }

    /** Switches view between Business Donor, Casual Donor, and Food Group modes */
    public void setRegistrationMode(Mode mode) {
        this.mode = mode;
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

    // Upload handlers: each opens the file explorer for its own row
    @FXML private void handleUploadMayorsPermit() { upload(mayorsPermit, "Mayor's Permit"); }
    @FXML private void handleUploadSanitaryPermit() { upload(sanitaryPermit, "Sanitary Permit"); }
    @FXML private void handleUploadBarangayCert() { upload(barangayCert, "Barangay Certificate"); }
    @FXML private void handleUploadAuthLetter() { upload(authLetter, "Authorization Letter"); }

    @FXML
    private void handleUploadCredential() {
        if (credential.type == null) {
            showError("Choose a credential first.");
            return;
        }
        upload(credential, credential.type.label());
    }

    /** Opens the system file explorer; a valid PDF, JPG or PNG replaces the row's file. Cancel changes nothing. */
    private void upload(Slot slot, String documentName) {
        hideError();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose your " + documentName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        if (lastFolder != null && lastFolder.isDirectory()) chooser.setInitialDirectory(lastFolder);
        File file = chooser.showOpenDialog(submitBtn.getScene().getWindow());
        if (file == null) return;
        try {
            fileStorageService.validate(file);   // R1.1: type and size, checked now so the user hears about it at once
        } catch (RuleViolationException e) {
            showError(documentName + ": " + e.getMessage());   // A8
            return;
        }
        lastFolder = file.getParentFile();
        slot.file = file;
        showFile(slot);
    }

    private void showFile(Slot slot) {
        slot.fileLabel.setText(slot.file.getName());
        slot.fileLabel.setTooltip(new Tooltip(slot.file.getName()));   // the label shortens long names; hover shows all
        slot.fileBox.setVisible(true);
        slot.fileBox.setManaged(true);
        slot.uploadBtn.setVisible(false);
        slot.uploadBtn.setManaged(false);
    }

    private void clearSlot(Slot slot) {
        slot.file = null;
        slot.fileBox.setVisible(false);
        slot.fileBox.setManaged(false);
        slot.uploadBtn.setVisible(true);
        slot.uploadBtn.setManaged(true);
    }

    /** The chosen files of the rows this role needs, with their expiry dates. */
    private List<DocumentUpload> chosenUploads() {
        List<Slot> shown = switch (mode) {
            case BUSINESS_DONOR -> List.of(mayorsPermit, sanitaryPermit);
            case CASUAL_DONOR -> List.of(barangayCert);
            case FOOD_GROUP -> List.of(credential, authLetter);
        };
        List<DocumentUpload> uploads = new ArrayList<>();
        for (Slot slot : shown) {
            if (slot.file == null || slot.type == null) continue;
            boolean noExpiry = slot.noExpiryCheck != null && slot.noExpiryCheck.isSelected();
            uploads.add(new DocumentUpload(slot.type, slot.file, slot.expiryPicker.getValue(), noExpiry));
        }
        return uploads;
    }

    // Navigation & Submission
    @FXML
    private void handleBackToStep1() {
        RegistrationDraft.setUploads(chosenUploads());   // step 2 shows them again when the user comes back
        Navigator.show(Screen.REGISTER);
    }

    @FXML
    private void handleConfirmSubmit() {
        hideError();
        try {
            // the rules and the saving live in the service (A1, A2)
            registrationService.submit(RegistrationDraft.get(), chosenUploads(), consentCheckBox.isSelected());
        } catch (RuleViolationException e) {
            showError(e.getMessage());   // A8
            return;
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            showError("Could not submit your registration. Check the database connection and try again.");
            return;
        }
        Navigator.show(Screen.REGISTER_SUBMITTED);   // the submitted screen reads and then clears the draft
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
