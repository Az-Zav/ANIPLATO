package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Account.Role;
import com.aniplato.model.Document;
import com.aniplato.service.MaintenanceService;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class ProfileController {

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


    //This is for testing only
    @FXML
    private void initialize() {
        Role activeRole = Navigator.currentRole();

        boolean isAdmin = (activeRole == Role.ADMIN);
        boolean isDonor = (activeRole == Role.BUSINESS_DONOR || activeRole == Role.CASUAL_DONOR);
        boolean isFoodGroup = (activeRole == Role.FOOD_GROUP);
        boolean isOrg = isDonor || isFoodGroup;

        if (adminCardBox != null) {
            adminCardBox.setVisible(isAdmin);
            adminCardBox.setManaged(isAdmin);
        }

        if (orgCardBox != null) {
            orgCardBox.setVisible(isOrg);
            orgCardBox.setManaged(isOrg);
        }

        if (docsCardBox != null) {
            docsCardBox.setVisible(isOrg);
            docsCardBox.setManaged(isOrg);
        }

        if (isAdmin) {
            if (docAlertBanner != null) {
                docAlertBanner.setVisible(false);
                docAlertBanner.setManaged(false);
            }

            if (adminNameLabel != null) adminNameLabel.setText("ANIPLATO Admin");
            if (adminRoleLabel != null) adminRoleLabel.setText("Admin");
            if (adminSinceLabel != null) adminSinceLabel.setText("Sep 1, 2026");

            if (loginEmailLabel != null) loginEmailLabel.setText("admin@aniplato.ph");
            if (passwordStatusLabel != null) passwordStatusLabel.setText("Last changed Sep 1, 2026");

        } else if (isOrg) {
            if (isFoodGroup) {
                if (orgNameLabel != null) orgNameLabel.setText("Bayanihan Food Group");
                if (orgRoleLabel != null) orgRoleLabel.setText("Food group");
                if (orgTypeLabel != null) orgTypeLabel.setText("Community kitchen");
                if (orgCityLabel != null) orgCityLabel.setText("Quezon City");
                if (orgContactLabel != null) orgContactLabel.setText("Jose Reyes · 0918 456 7890");
                if (orgApprovedDateLabel != null) orgApprovedDateLabel.setText("Oct 12, 2026");
                if (loginEmailLabel != null) loginEmailLabel.setText("contact@bayanihan.ph");
                if (passwordStatusLabel != null) passwordStatusLabel.setText("Last changed Oct 12, 2026");
            } else {
                if (orgNameLabel != null) orgNameLabel.setText("Manila Artisanal Bakeshop");
                if (orgRoleLabel != null) orgRoleLabel.setText("Business donor");
                if (orgTypeLabel != null) orgTypeLabel.setText("Bakery");
                if (orgCityLabel != null) orgCityLabel.setText("Mandaluyong City");
                if (orgContactLabel != null) orgContactLabel.setText("Maria Santos · 0917 123 4567");
                if (orgApprovedDateLabel != null) orgApprovedDateLabel.setText("Oct 5, 2026");
                if (loginEmailLabel != null) loginEmailLabel.setText("contact@manilabakeshop.ph");
                if (passwordStatusLabel != null) passwordStatusLabel.setText("Last changed Oct 5, 2026");
            }

            List<Document> alerts = maintenanceService.getCurrentDocumentAlerts();
            LocalDate today = LocalDate.now();
            long expiredCount = alerts.stream().filter(document -> document.isExpired(today)).count();
            long upcomingCount = alerts.size() - expiredCount;
            boolean hasAlerts = !alerts.isEmpty();

            if (docAlertBanner != null) {
                docAlertBanner.setVisible(hasAlerts);
                docAlertBanner.setManaged(hasAlerts);
            }

            if (hasAlerts && docAlertTextLabel != null) {
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
        }
    }

    // PATTERN:ACTION handleEditContact - triggered by Hyperlink in Organization card
    @FXML
    private void handleEditContact() {
        // TODO(service): open dialog or form to update organization contact info
    }

    // PATTERN:ACTION handleUploadRenewal - triggered by 'Upload renewal' button in Documents card
    @FXML
    private void handleUploadRenewal() {
        if (dialogController != null) {
            dialogController.showUploadRenewalMode("Mayor's / Business Permit");
        }
    }

    // PATTERN:ACTION handleForgotPassword - triggered by 'Forgot password' button in Account card
    @FXML
    private void handleForgotPassword() {
        if (dialogController != null) {
            String currentEmail = (loginEmailLabel != null) ? loginEmailLabel.getText() : "";
            dialogController.showForgotPasswordMode(currentEmail);
        }
    }

    // PATTERN:ACTION handleChangePassword - triggered by 'Change password' button in Account card
    @FXML
    private void handleChangePassword() {
        if (dialogController != null) {
            dialogController.showSetNewPasswordMode(null, () -> {
                if (passwordStatusLabel != null) {
                    passwordStatusLabel.setText("Last changed just now");
                }
            });
        }
    }
}