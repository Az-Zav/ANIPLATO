package com.aniplato.controller.admin;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ReviewRegistrationController {

    // Dynamic Header & Form Fields
    @FXML private Label orgTitleLabel;
    @FXML private Label statusPillLabel;
    @FXML private Label orgNameLabel;
    @FXML private Label rolePillLabel;
    @FXML private Label contactPersonLabel;
    @FXML private Label emailLabel;
    @FXML private Label orgTypeLabel;
    @FXML private Label cityLabel;

    // Dynamic Document Info
    @FXML private Label doc1MetaLabel;
    @FXML private Label doc1FileLabel;
    @FXML private Label doc2MetaLabel;
    @FXML private Label doc2FileLabel;
    @FXML private Label registeredDateLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): populate details and document statuses from RegistrationService
    }

    // PATTERN:ACTION placeholder: verifies document 2
    @FXML
    private void handleVerifyDoc2() {
        // TODO(service): mark document as verified in service
    }

    // PATTERN:ACTION placeholder: flags document problem
    @FXML
    private void handleFlagDoc2() {
        // TODO(service): flag problem on document
    }

    // PATTERN:ACTION placeholder: rejects registration
    @FXML
    private void handleReject() {
        // TODO(service): registrationService.reject(...)
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    // PATTERN:ACTION placeholder: approves registration
    @FXML
    private void handleApprove() {
        // TODO(service): registrationService.approve(...)
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }
}