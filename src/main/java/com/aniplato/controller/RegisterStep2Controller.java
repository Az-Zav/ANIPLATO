package com.aniplato.controller;

import com.aniplato.model.RegistrationData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;

public class RegisterStep2Controller {

    @FXML private Label roleBadgeLabel;
    @FXML private VBox dynamicFieldsContainer;
    @FXML private CheckBox consentCheckBox;
    @FXML private Hyperlink termsLink;

    @FXML
    public void initialize() {
        RegistrationData data = RegistrationData.getInstance();
        String role = data.getRole() != null ? data.getRole() : "FOOD_GROUP";
        String orgType = data.getOrgType() != null ? data.getOrgType() : "Charity";

        if ("FOOD_GROUP".equalsIgnoreCase(role)) {
            roleBadgeLabel.setText("Required for: Food group · " + orgType);
            buildFoodGroupUI();
        } else {
            roleBadgeLabel.setText("Required for: Business donor · " + orgType);
            buildBusinessDonorUI();
        }
    }

    private void buildBusinessDonorUI() {
        dynamicFieldsContainer.getChildren().clear();

        ComboBox<String> busRegCombo = new ComboBox<>();
        busRegCombo.getItems().addAll("DTI Certificate of Business Name Registration", "SEC Certificate of Incorporation", "CDA Registration");
        busRegCombo.setValue("DTI Certificate of Business Name Registration");
        busRegCombo.setMaxWidth(Double.MAX_VALUE);

        VBox doc1 = createUploadedRow("Business registration *", "DTI, SEC or CDA", busRegCombo, "dti-certificate.pdf", LocalDate.of(2029, 3, 14), false);
        VBox doc2 = createUploadedRow("Mayor's / Business Permit *", "Current year", null, "mayors-permit-2026.pdf", LocalDate.of(2026, 12, 31), false);
        VBox doc3 = createEmptyUploadRow("Sanitary Permit *", "From the City Health Office");
        VBox doc4 = createEmptyUploadRow("FDA License to Operate", "Optional · only if you manufacture or repack packaged food");

        dynamicFieldsContainer.getChildren().addAll(doc1, doc2, doc3, doc4);
    }

    private void buildFoodGroupUI() {
        dynamicFieldsContainer.getChildren().clear();

        ComboBox<String> credCombo = new ComboBox<>();
        credCombo.getItems().addAll("DSWD Certificate of Registration", "PCNC Accreditation", "SEC Certificate (Non-Stock)");
        credCombo.setValue("DSWD Certificate of Registration");
        credCombo.setMaxWidth(Double.MAX_VALUE);

        VBox doc1 = createUploadedRow(
                "Credential *", "Any one: DSWD, PCNC or SEC (non-stock)",
                credCombo,
                "dswd-registration.pdf",
                LocalDate.of(2027, 6, 30),
                false
        );

        VBox doc2 = createUploadedRow(
                "Authorization letter *", "Names the contact person",
                null,
                "authorization-letter.pdf",
                null,
                true
        );

        Label helperNote = new Label("Schools can use a DepEd, CHED or TESDA Certificate of Recognition. Parishes can use an SEC certificate or an endorsement letter from the diocese.");
        helperNote.setWrapText(true);
        helperNote.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        dynamicFieldsContainer.getChildren().addAll(doc1, doc2, helperNote);
    }

    private VBox createUploadedRow(String title, String subtitle, Control optionalCombo, String fileName, LocalDate expiryDate, boolean noExpiry) {
        VBox box = new VBox(6);

        HBox header = new HBox(2);
        header.setAlignment(Pos.CENTER_LEFT);

        boolean isRequired = title.contains("*");
        String cleanTitle = title.replace("*", "").trim();

        Label titleLbl = new Label(cleanTitle);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        header.getChildren().add(titleLbl);

        if (isRequired) {
            Label starLbl = new Label(" *");
            starLbl.setTextFill(Color.web("#EF4444"));
            starLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            header.getChildren().add(starLbl);
        }

        Label subLbl = new Label("  " + subtitle);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        header.getChildren().add(subLbl);

        box.getChildren().add(header);

        if (optionalCombo != null) {
            box.getChildren().add(optionalCombo);
        }

        HBox controlsRow = new HBox(10);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        HBox filePill = new HBox(8);
        filePill.setAlignment(Pos.CENTER_LEFT);
        filePill.setStyle("-fx-background-color: #E6F4EA; -fx-background-radius: 8px; -fx-padding: 8px 12px; -fx-border-color: #BBF7D0; -fx-border-radius: 8px;");
        HBox.setHgrow(filePill, Priority.ALWAYS);

        Label iconLbl = new Label("📄");
        Label fileLbl = new Label(fileName);
        fileLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button replaceBtn = new Button("Replace");
        replaceBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-cursor: hand;");

        filePill.getChildren().addAll(iconLbl, fileLbl, spacer, replaceBtn);

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefWidth(140);
        if (expiryDate != null) {
            datePicker.setValue(expiryDate);
        } else {
            datePicker.setPromptText("No expiry");
        }
        datePicker.setDisable(noExpiry);

        controlsRow.getChildren().addAll(filePill, datePicker);
        box.getChildren().add(controlsRow);

        CheckBox noExpiryCb = new CheckBox("No expiry");
        noExpiryCb.setSelected(noExpiry);
        noExpiryCb.setStyle("-fx-font-size: 11px; -fx-text-fill: #4B5563;");
        noExpiryCb.setOnAction(e -> datePicker.setDisable(noExpiryCb.isSelected()));

        box.getChildren().add(noExpiryCb);

        return box;
    }

    private VBox createEmptyUploadRow(String title, String subtitle) {
        VBox box = new VBox(6);

        HBox header = new HBox(2);
        header.setAlignment(Pos.CENTER_LEFT);

        boolean isRequired = title.contains("*");
        String cleanTitle = title.replace("*", "").trim();

        Label titleLbl = new Label(cleanTitle);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        header.getChildren().add(titleLbl);

        if (isRequired) {
            Label starLbl = new Label(" *");
            starLbl.setTextFill(Color.web("#EF4444"));
            starLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            header.getChildren().add(starLbl);
        }

        Label subLbl = new Label("  " + subtitle);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        header.getChildren().add(subLbl);

        box.getChildren().add(header);

        HBox controlsRow = new HBox(10);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        Button uploadBtn = new Button("📤  Upload PDF, JPG or PNG   Max 5 MB");
        uploadBtn.setMaxWidth(Double.MAX_VALUE);
        uploadBtn.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #D1D5DB; -fx-border-style: dashed; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px; -fx-text-fill: #15803D; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox.setHgrow(uploadBtn, Priority.ALWAYS);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Expiry date");
        datePicker.setPrefWidth(140);

        controlsRow.getChildren().addAll(uploadBtn, datePicker);
        box.getChildren().add(controlsRow);

        return box;
    }

    @FXML
    private void handleOpenTerms(ActionEvent event) {
        try {
            URL htmlResource = getClass().getResource("/terms_and_conditions.html");
            if (htmlResource != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlResource.toURI());
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://aniplato.ph/terms"));
            }
        } catch (Exception e) {
            System.err.println("❌ Could not open browser for Terms and Conditions:");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            URL step1Location = getClass().getResource("/com/aniplato/view/register.fxml");
            if (step1Location != null) {
                Parent step1 = FXMLLoader.load(step1Location);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(step1);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to navigate back to Step 1:");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (consentCheckBox != null && !consentCheckBox.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Consent Required");
            alert.setHeaderText("Terms and Conditions Agreement");
            alert.setContentText("Please check the box to agree to ANIPLATO's Terms and Conditions before submitting your application.");
            alert.showAndWait();
            return;
        }

        try {
            URL submittedLocation = getClass().getResource("/com/aniplato/view/register_submitted.fxml");
            if (submittedLocation == null) {
                submittedLocation = getClass().getResource("/com/aniplato/view/shared/register_submitted.fxml");
            }

            if (submittedLocation != null) {
                System.out.println("Registration submitted successfully!");
                Parent submittedRoot = FXMLLoader.load(submittedLocation);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(submittedRoot);
            } else {
                System.err.println("❌ ERROR: Cannot locate register_submitted.fxml on classpath.");
            }
        } catch (IOException e) {
            System.err.println("❌ Navigation to Submitted View failed:");
            e.printStackTrace();
        }
    }
}