package com.aniplato.controller.shared;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import com.aniplato.model.Account;

public class HandoffRecordController {

    // Embedded Dialog Controller & Overlay Container
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Dynamic Header Labels
    @FXML private Label itemTitleLabel;
    @FXML private Label recordMetaLabel;
    @FXML private Label statusPillLabel;

    // Dynamic Details Labels
    @FXML private Label donorDetailsLabel;
    @FXML private Label foodGroupDetailsLabel;
    @FXML private Label foodTypeQuantityLabel;
    @FXML private Label statedUseLabel;
    @FXML private Label beneficiariesLabel;
    @FXML private Label pickupTimeLabel;
    @FXML private Label releaseTimestampLabel;
    @FXML private Label receiptTimestampLabel;
    @FXML private Label donorAttestationLabel;
    @FXML private Label groupAttestationLabel;
    @FXML private VBox reportBox;
    @FXML private Label reportStatusLabel;
    @FXML private Label reportMetaLabel;
    @FXML private Label reportSummaryLabel;
    @FXML private Label reportDetailsLabel;

    @FXML
    private void initialize() {
        // TODO(service): Load handoff record details from HandoffService
        boolean donorView = Navigator.currentRole() == Account.Role.BUSINESS_DONOR
                || Navigator.currentRole() == Account.Role.CASUAL_DONOR;
        reportBox.setVisible(donorView);
        reportBox.setManaged(donorView);
        if (donorView) {
            reportStatusLabel.setText("SUBMITTED");
            reportMetaLabel.setText("Submitted Oct 9, 2026, 10:20 AM");
            reportSummaryLabel.setText("School feeding program · Oct 8, 2026 · 82 people served");
            reportDetailsLabel.setText("Evening meal served at the parish hall to 82 pupils, seniors and families. Everything was distributed free.");
        }
    }

    // PATTERN:NAV -> routes back to History list
    @FXML
    private void handleBackToHistory() {
        Navigator.show(Screen.HISTORY);
    }
}