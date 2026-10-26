package com.aniplato.controller.admin;

import com.aniplato.controller.TableRows;
import com.aniplato.exception.DataAccessException;
import com.aniplato.service.StatsService;
import com.aniplato.service.StatsService.CityRow;
import com.aniplato.service.StatsService.MonthlyReport;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportsController {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

    // Month Selector
    @FXML private ComboBox<YearMonth> monthComboBox;

    // Stat Cards Row 1
    @FXML private Label kgPostedLabel;
    @FXML private Label kgRescuedLabel;
    @FXML private Label kgExpiredLabel;

    // Stat Cards Row 2
    @FXML private Label rescueRateLabel;
    @FXML private Label handoffsCompletedCountLabel;
    @FXML private Label peopleServedCountLabel;

    // Stat Cards Row 3
    @FXML private Label offerAcceptanceRateLabel;
    @FXML private Label lapseRateLabel;
    @FXML private Label reportComplianceRateLabel;

    // By City Table & Totals
    @FXML private VBox cityRowsBox;
    @FXML private Label totalCityNameLabel;
    @FXML private Label totalKgPostedLabel;
    @FXML private Label totalKgRescuedLabel;
    @FXML private Label totalKgExpiredLabel;
    @FXML private Label totalRescueRateLabel;
    @FXML private ProgressBar totalRescueProgressBar;

    private final StatsService statsService = new StatsService();

    @FXML
    private void initialize() {
        monthComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth month) {
                return month == null ? "" : month.format(MONTH);
            }

            @Override
            public YearMonth fromString(String text) {
                return null;   // the month is only ever picked from the list
            }
        });
        try {
            monthComboBox.getItems().setAll(statsService.getReportMonths(LocalDate.now()));
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            totalCityNameLabel.setText("Could not load the reports. Check the database connection.");
            return;
        }
        monthComboBox.valueProperty().addListener((o, was, month) -> show(month));
        monthComboBox.getSelectionModel().selectFirst();   // the newest month
    }

    private void show(YearMonth month) {
        if (month == null) return;
        MonthlyReport report;
        try {
            report = statsService.getReport(month, LocalDateTime.now());
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            totalCityNameLabel.setText("Could not load the report. Check the database connection.");
            return;
        }
        kgPostedLabel.setText(AdminTable.kg(report.kgPosted()));
        kgRescuedLabel.setText(AdminTable.kg(report.kgRescued()));
        kgExpiredLabel.setText(AdminTable.kg(report.kgExpired()));
        rescueRateLabel.setText(AdminTable.percent(report.rescueRate()));
        handoffsCompletedCountLabel.setText(String.format("%,d", report.handoffsCompleted()));
        peopleServedCountLabel.setText(String.format("%,d", report.peopleServed()));
        offerAcceptanceRateLabel.setText(AdminTable.percent(report.offerAcceptanceRate()));
        lapseRateLabel.setText(AdminTable.percent(report.lapseRate()));
        reportComplianceRateLabel.setText(AdminTable.percent(report.reportCompliance()));

        // PATTERN:LIST one row per city that had posts, biggest first
        cityRowsBox.getChildren().clear();
        for (CityRow city : report.cities()) cityRowsBox.getChildren().add(buildRow(city));

        totalCityNameLabel.setText("All cities");
        totalKgPostedLabel.setText(AdminTable.kg(report.kgPosted()) + " kg");
        totalKgRescuedLabel.setText(AdminTable.kg(report.kgRescued()) + " kg");
        totalKgExpiredLabel.setText(AdminTable.kg(report.kgExpired()) + " kg");
        totalRescueRateLabel.setText(AdminTable.percent(report.rescueRate()));
        totalRescueProgressBar.setProgress(report.rescueRate() == null ? 0 : report.rescueRate());
    }

    /** City, three kg totals, and the rescue rate with a bar. */
    private GridPane buildRow(CityRow city) {
        GridPane row = TableRows.row(24, 18, 18, 18, 22);
        TableRows.put(row, 0, AdminTable.label(city.city(), "form-label"));
        TableRows.put(row, 1, AdminTable.label(AdminTable.kg(city.kgPosted()) + " kg", "table-cell-text"));
        TableRows.put(row, 2, AdminTable.label(AdminTable.kg(city.kgRescued()) + " kg", "table-cell-text"));
        TableRows.put(row, 3, AdminTable.label(AdminTable.kg(city.kgExpired()) + " kg", "table-cell-text"));
        ProgressBar bar = new ProgressBar(city.rescueRate() == null ? 0 : city.rescueRate());
        HBox.setHgrow(bar, Priority.ALWAYS);
        HBox rate = new HBox(10, AdminTable.label(AdminTable.percent(city.rescueRate()), "text-success-bold"), bar);
        rate.setAlignment(Pos.CENTER_LEFT);
        TableRows.put(row, 4, rate);
        return row;
    }
}
