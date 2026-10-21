package com.aniplato.controller.admin;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
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

    // Pending Accounts Table & Header Controls
    @FXML private Label totalPendingBadgeLabel;
    @FXML private Hyperlink viewAllLink;
    @FXML private VBox pendingRowsBox;
    @FXML private Label emptyLabel;

    // Pagination Footer Controls
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // Pagination State
    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalPendingRecords = 124; // Mock dataset count (supports > 100 records)

    @FXML
    private void initialize() {
        // TODO(service): pendingCountLabel.setText(String.valueOf(reportService.getPendingCount()))
        // TODO(service): kgListedLabel.setText(String.format("%,d", reportService.getKgListedThisMonth()))
        // TODO(service): rescueRateLabel.setText(String.format("%.1f%%", reportService.getRescueRate()))
        // TODO(service): openSafetyNoticesCountLabel.setText(String.valueOf(reportService.getOpenSafetyNoticesCount()))
        // TODO(service): documentsExpiringCountLabel.setText(String.valueOf(reportService.getExpiringDocsCount()))
        // TODO(service): suspendedCountLabel.setText(String.valueOf(reportService.getSuspendedCount()))

        updatePaginationState();
    }

    /**
     * Calculates page range and updates button states / footer text
     */
    private void updatePaginationState() {
        int totalPages = (int) Math.ceil((double) totalPendingRecords / pageSize);
        if (totalPages == 0) totalPages = 1;

        int startRecord = ((currentPage - 1) * pageSize) + 1;
        int endRecord = Math.min(currentPage * pageSize, totalPendingRecords);

        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Showing " + startRecord + "–" + endRecord + " of " + totalPendingRecords);
        }

        if (totalPendingBadgeLabel != null) {
            totalPendingBadgeLabel.setText(totalPendingRecords + " total");
        }

        if (prevPageBtn != null) {
            prevPageBtn.setDisable(currentPage <= 1);
        }

        if (nextPageBtn != null) {
            nextPageBtn.setDisable(currentPage >= totalPages);
        }

        // TODO(service): Fetch sublist for range [startRecord - 1, endRecord) and populate pendingRowsBox
        // TODO(service): Toggle emptyLabel visibility when totalPendingRecords == 0
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationState();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalPendingRecords / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationState();
        }
    }

    @FXML
    private void handleNavPendingRegistrations() {
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }
}