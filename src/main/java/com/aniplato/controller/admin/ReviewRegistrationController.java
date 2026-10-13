package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ReviewRegistrationController {

    // Nested Dialog Controller and Overlay Container (Injected from <fx:include fx:id="dialog" ... />)
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

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

    // PATTERN:ACTION opens Reject modal dialog overlay
    @FXML
    private void handleReject() {
        if (dialog != null && dialogController != null) {
            dialog.setVisible(true);
            dialog.setManaged(true);

            String orgName = (orgNameLabel != null && !orgNameLabel.getText().isEmpty())
                    ? orgNameLabel.getText()
                    : "San Antonio Community Pantry";

            dialogController.showRejectMode(orgName);
        }
    }

    // PATTERN:ACTION opens Approve modal dialog overlay
    @FXML
    private void handleApprove() {
        if (dialog != null && dialogController != null) {
            dialog.setVisible(true);
            dialog.setManaged(true);

            String orgName = (orgNameLabel != null && !orgNameLabel.getText().isEmpty())
                    ? orgNameLabel.getText()
                    : "San Antonio Community Pantry";
            String orgEmail = (emailLabel != null && !emailLabel.getText().isEmpty())
                    ? emailLabel.getText()
                    : "contact@sanantoniopantry.ph";

            dialogController.showApproveMode(orgName, orgEmail);
        }
    }
}