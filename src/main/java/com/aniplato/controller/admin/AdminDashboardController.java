package com.aniplato.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AdminDashboardController {

    @FXML private Label pendingCountLabel;
    @FXML private Label kgListedLabel;
    @FXML private Label rescueRateLabel;
    @FXML private VBox pendingRowsBox;

    @FXML
    private void initialize() {
        // PATTERN:DYNAMIC fills placeholders
        // TODO(service): pendingCountLabel, kgListedLabel, rescueRateLabel from ReportService (derived, A9)

        // PATTERN:LIST fills rows
        // TODO(service): replace the sample row in pendingRowsBox with one row per pending registration
    }
}
