package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Account;
import com.aniplato.model.Admin;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.service.DocumentReviewService;
import com.aniplato.service.FileStorageService;
import com.aniplato.service.MaintenanceService;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ProfileController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    // Nested Dialog Controller (Injected from <fx:include fx:id="dialog" ... />)
    @FXML private DialogController dialogController;

    // Dynamic Visibility Containers
    @FXML private HBox docAlertBanner;
    @FXML private Label docAlertTextLabel;
    @FXML private VBox adminCardBox;
    @FXML private VBox orgCardBox;
    @FXML private VBox docsCardBox;

    // Admin Card Fields
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label adminSinceLabel;

    // Organization Card Fields
    @FXML private Label orgNameLabel;
    @FXML private Label orgRoleLabel;
    @FXML private Label orgTypeLabel;       // shows the organization's contact email: the schema has no "type"
    @FXML private Label orgCityLabel;
    @FXML private Label orgContactLabel;
    @FXML private Label orgApprovedDateLabel;

    // Document Table Container
    @FXML private VBox docRowsBox;

    // Account Fields
    @FXML private Label loginEmailLabel;
    @FXML private Label passwordStatusLabel;

    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final DocumentReviewService documentReviewService = new DocumentReviewService();

    @FXML
    private void initialize() {
        Account account = Session.require();
        Registration registration = account.registration;   // null for the admin, who has no organization
        boolean isOrg = registration != null;

        setShown(adminCardBox, !isOrg);
        setShown(orgCardBox, isOrg);
        setShown(docsCardBox, isOrg);
        setShown(docAlertBanner, false);

        loginEmailLabel.setText(account.email);
        passwordStatusLabel.setText("Last changed " + date(account.passwordChangedAt));

        if (account instanceof Admin admin) {
            adminNameLabel.setText(admin.adminName);
            adminRoleLabel.setText(account.role.displayName());
            adminSinceLabel.setText(date(account.createdAt));
            return;
        }

        orgNameLabel.setText(registration.name);
        orgRoleLabel.setText(account.role.displayName());
        orgTypeLabel.setText(registration.email);
        orgCityLabel.setText(registration.city.name);
        orgContactLabel.setText(registration.contactPerson);   // the phone number is not shown here (A7)
        orgApprovedDateLabel.setText(registration.reviewedAt == null ? "—" : date(registration.reviewedAt));

        loadDocuments();
        loadDocumentAlert();
    }

    /** One row per required document: type, file, expiry and status (R1.4, 6.6). */
    private void loadDocuments() {
        docRowsBox.getChildren().clear();
        List<Document> documents;
        try {
            documents = documentReviewService.getMyDocuments();
        } catch (RuntimeException e) {
            docRowsBox.getChildren().add(styled(new Label("Could not load your documents."), "text-muted"));
            return;
        }
        LocalDate today = LocalDate.now();
        for (Document d : documents) docRowsBox.getChildren().add(documentRow(d, today));
    }

    private GridPane documentRow(Document d, LocalDate today) {
        GridPane row = new GridPane();
        row.getStyleClass().add("table-data-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        for (double percent : new double[] {28, 26, 18, 28}) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            row.getColumnConstraints().add(column);
        }

        Label type = styled(new Label(d.type.label()), "form-label");
        Label file = styled(new Label(new File(d.filePath).getName()), "table-cell-text");
        Label expires = styled(new Label(d.expiresOn == null ? "No expiry" : d.expiresOn.format(DATE)), "table-cell-text");

        boolean needsRenewal = d.review == Document.Review.OK
                && (d.isExpired(today) || d.expiresWithin(Settings.DOCUMENT_WARNING_DAYS, today));
        Label status;
        if (d.review == Document.Review.OK && !needsRenewal) {
            status = styled(new Label("Verified"), "pill-food-donor");
        } else if (d.review == Document.Review.FLAGGED) {
            status = styled(new Label("Flagged"), "tag-reregistered");
        } else if (d.review == null) {
            status = styled(new Label("Awaiting review"), "tag-reregistered");
        } else if (d.isExpired(today)) {
            status = styled(new Label("Expired"), "tag-reregistered");
        } else {
            long days = ChronoUnit.DAYS.between(today, d.expiresOn);
            status = styled(new Label("Expires in " + days + (days == 1 ? " day" : " days")), "tag-reregistered");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusCell = new HBox(8, status, spacer);
        statusCell.setAlignment(Pos.CENTER_LEFT);
        if (needsRenewal) {
            Button renew = new Button("Upload renewal");
            renew.getStyleClass().add("table-review-btn");
            renew.setMinWidth(Region.USE_PREF_SIZE);
            renew.setOnAction(e -> handleUploadRenewal(d.type));
            statusCell.getChildren().add(renew);
        } else {
            Hyperlink view = new Hyperlink("View file");
            view.getStyleClass().add("terms-link");
            view.setOnAction(e -> openFile(d));
            statusCell.getChildren().add(view);
        }

        GridPane.setColumnIndex(type, 0);
        GridPane.setColumnIndex(file, 1);
        GridPane.setColumnIndex(expires, 2);
        GridPane.setColumnIndex(statusCell, 3);
        row.getChildren().addAll(type, file, expires, statusCell);
        return row;
    }

    /** The banner above the cards: how many documents are expired or about to expire (6.6). */
    private void loadDocumentAlert() {
        List<Document> alerts;
        try {
            alerts = maintenanceService.getCurrentDocumentAlerts();
        } catch (RuntimeException e) {
            return;   // no banner is better than a wrong one
        }
        LocalDate today = LocalDate.now();
        long expiredCount = alerts.stream().filter(document -> document.isExpired(today)).count();
        long upcomingCount = alerts.size() - expiredCount;
        if (alerts.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        if (expiredCount > 0) {
            message.append(expiredCount).append(expiredCount == 1 ? " document expired" : " documents expired");
        }
        if (upcomingCount > 0) {
            if (!message.isEmpty()) message.append(" and ");
            message.append(upcomingCount).append(upcomingCount == 1
                    ? " document expires within " : " documents expire within ")
                    .append(Settings.DOCUMENT_WARNING_DAYS).append(" days");
        }
        docAlertTextLabel.setText(message + ". Upload renewals below; an admin verifies each one.");
        setShown(docAlertBanner, true);
    }

    private void openFile(Document d) {
        File file = new FileStorageService().resolve(d.filePath);
        try {
            if (!file.isFile() || !Desktop.isDesktopSupported()) {
                DialogController.showError("The file " + file.getName() + " is not in the uploads folder.");
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (java.io.IOException e) {
            DialogController.showError("Could not open " + file.getName() + ".");
        }
    }

    private static void setShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static Label styled(Label label, String styleClass) {
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static String date(LocalDateTime when) {
        return when.format(DATE);
    }

    // PATTERN:ACTION handleEditContact - triggered by Hyperlink in Organization card
    @FXML
    private void handleEditContact() {
        // TODO(service): open dialog or form to update organization contact info
    }

    private void handleUploadRenewal(Document.Type type) {
        dialogController.showUploadRenewalMode(type.label());
    }

    // PATTERN:ACTION handleForgotPassword - triggered by 'Forgot password' button in Account card
    @FXML
    private void handleForgotPassword() {
        dialogController.showForgotPasswordMode(loginEmailLabel.getText());
    }

    // PATTERN:ACTION handleChangePassword - triggered by 'Change password' button in Account card
    @FXML
    private void handleChangePassword() {
        dialogController.showSetNewPasswordMode(null, () -> passwordStatusLabel.setText("Last changed just now"));
    }
}
