package com.aniplato.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ReportsController {

    // Month Selector
    @FXML private ComboBox<String> monthComboBox;

    // Stat Cards Row 1
    @FXML private Label kgListedLabel;
    @FXML private Label kgRescuedLabel;
    @FXML private Label kgExpiredLabel;
    @FXML private Label rescueRateLabel;

    // Stat Cards Row 2
    @FXML private Label kgCancelledLabel;
    @FXML private Label offersSentCountLabel;
    @FXML private Label listingsCountLabel;
    @FXML private Label offerAcceptanceRateLabel;
    @FXML private Label lapseRateLabel;

    // By City Table & Totals
    @FXML private VBox cityRowsBox;
    @FXML private Label totalCityNameLabel;
    @FXML private Label totalKgListedLabel;
    @FXML private Label totalKgRescuedLabel;
    @FXML private Label totalKgExpiredLabel;
    @FXML private Label totalRescueRateLabel;
    @FXML private ProgressBar totalRescueProgressBar;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): populate monthComboBox with available report months and select current
        // TODO(service): reportService.getReportForMonth(selectedMonth) -> set stat card values
        // TODO(service): populate cityRowsBox dynamically with city report rows
        // TODO(service): set overall total labels and progress bar
    }
}