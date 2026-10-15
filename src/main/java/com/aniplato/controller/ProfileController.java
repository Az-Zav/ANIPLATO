package com.aniplato.controller;

import com.aniplato.model.Document;
import com.aniplato.service.MaintenanceService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Role;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class ProfileController {

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
    @FXML private Label orgTypeLabel;
    @FXML private Label orgCityLabel;
    @FXML private Label orgContactLabel;
    @FXML private Label orgApprovedDateLabel;

    // Document Table Container
    @FXML private VBox docRowsBox;

    // Account Fields
    @FXML private Label loginEmailLabel;
    @FXML private Label passwordStatusLabel;

    private final MaintenanceService maintenanceService = new MaintenanceService();

    // PATTERN:DYNAMIC fills placeholders according to current authenticated user role
    @FXML
    private void initialize() {
        Role activeRole = Navigator.currentRole();

        boolean isAdmin = (activeRole == Role.ADMIN);
        boolean isDonor = (activeRole == Role.DONOR);
        boolean isFoodGroup = (activeRole == Role.FOOD_GROUP);
        boolean isOrg = isDonor || isFoodGroup;

        // Toggle Container Visibilities
        adminCardBox.setVisible(isAdmin);
        adminCardBox.setManaged(isAdmin);

        orgCardBox.setVisible(isOrg);
        orgCardBox.setManaged(isOrg);

        docsCardBox.setVisible(isOrg);
        docsCardBox.setManaged(isOrg);

        if (isAdmin) {
            docAlertBanner.setVisible(false);
            docAlertBanner.setManaged(false);

            // TODO(service): populate with real admin profile data from service
            adminNameLabel.setText("ANIPLATO Admin");
            adminRoleLabel.setText("Admin");
            adminSinceLabel.setText("Sep 1, 2026");

            loginEmailLabel.setText("admin@aniplato.ph");
            passwordStatusLabel.setText("Last changed Sep 1, 2026");

        } else if (isOrg) {
            List<Document> alerts = maintenanceService.getCurrentDocumentAlerts();
            LocalDate today = LocalDate.now();
            long expiredCount = alerts.stream().filter(document -> document.isExpired(today)).count();
            long upcomingCount = alerts.size() - expiredCount;
            boolean hasAlerts = !alerts.isEmpty();
            docAlertBanner.setVisible(hasAlerts);
            docAlertBanner.setManaged(hasAlerts);
            if (hasAlerts) {
                StringBuilder message = new StringBuilder();
                if (expiredCount > 0) {
                    message.append(expiredCount).append(expiredCount == 1 ? " document expired" : " documents expired");
                }
                if (upcomingCount > 0) {
                    if (!message.isEmpty()) message.append(" and ");
                    message.append(upcomingCount).append(upcomingCount == 1
                            ? " document expires within 30 days" : " documents expire within 30 days");
                }
                docAlertTextLabel.setText(message + ". Upload renewals below; an admin verifies each one.");
            }

            // TODO(service): populate with real organization profile & documents data from service
        }
    }

    // PATTERN:ACTION placeholder: opens contact edit dialog
    @FXML
    private void handleEditContact() {
        // TODO(service): open dialog to edit contact person and phone number
    }

    // PATTERN:ACTION placeholder: uploads renewal document
    @FXML
    private void handleUploadRenewal() {
        // TODO(service): trigger file chooser and save renewal document
    }

    // PATTERN:ACTION placeholder: opens change password dialog/screen
    @FXML
    private void handleChangePassword() {
        // TODO(service): handle password change action
    }
}