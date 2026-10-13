package com.aniplato.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AdminDashboardController {

    // Metric Cards
    @FXML private Label pendingCountLabel;
    @FXML private Label kgListedLabel;
    @FXML private Label rescueRateLabel;
    @FXML private Label openSafetyNoticesCountLabel;
    @FXML private Label documentsExpiringCountLabel;
    @FXML private Label suspendedCountLabel;

    // Pending Accounts Table
    @FXML private VBox pendingRowsBox;
    @FXML private Label emptyLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): pendingCountLabel.setText(String.valueOf(reportService.getPendingCount()))
        // TODO(service): kgListedLabel.setText(String.format("%,d", reportService.getKgListedThisMonth()))
        // TODO(service): rescueRateLabel.setText(String.format("%.1f%%", reportService.getRescueRate()))
        // TODO(service): openSafetyNoticesCountLabel.setText(String.valueOf(reportService.getOpenSafetyNoticesCount()))
        // TODO(service): documentsExpiringCountLabel.setText(String.valueOf(reportService.getExpiringDocsCount()))
        // TODO(service): suspendedCountLabel.setText(String.valueOf(reportService.getSuspendedCount()))

        // PATTERN:LIST fills rows
        // TODO(service): populate pendingRowsBox dynamically and toggle emptyLabel visibility
    }
}