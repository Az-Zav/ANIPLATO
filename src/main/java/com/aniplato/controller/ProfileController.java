package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Role;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
            // TODO(service): check document expiration status via documentService to toggle banner
            docAlertBanner.setVisible(false);
            docAlertBanner.setManaged(false);

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