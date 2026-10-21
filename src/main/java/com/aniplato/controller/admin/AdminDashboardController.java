package com.aniplato.controller.admin;

import com.aniplato.exception.DataAccessException;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.PendingRow;
import com.aniplato.service.StatsService;
import com.aniplato.service.StatsService.AdminSummary;
import com.aniplato.util.AdminSelection;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

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

    private final StatsService statsService = new StatsService();
    private final RegistrationService registrationService = new RegistrationService();

    // Pagination State
    private int currentPage = 1;
    private final int pageSize = 10;
    private List<PendingRow> pending = List.of();

    @FXML
    private void initialize() {
        try {
            AdminSummary summary = statsService.getAdminSummary(LocalDate.now());
            pending = registrationService.getPending();
            pendingCountLabel.setText(String.valueOf(summary.pendingRegistrations()));
            kgListedLabel.setText(AdminTable.kg(summary.kgListedThisMonth()));
            rescueRateLabel.setText(AdminTable.percent(summary.rescueRate()));
            openSafetyNoticesCountLabel.setText(String.valueOf(summary.openSafetyNotices()));
            documentsExpiringCountLabel.setText(String.valueOf(summary.documentsEnding()));
            suspendedCountLabel.setText(String.valueOf(summary.suspendedAccounts()));
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            pageInfoLabel.setText("Could not load the dashboard. Check the database connection.");
            return;
        }
        updatePaginationState();
    }

    /**
     * Calculates page range, fills the rows of the current page and updates button states / footer text
     */
    private void updatePaginationState() {
        int total = pending.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(currentPage * pageSize, total);

        pageInfoLabel.setText(total == 0 ? "Showing 0 of 0" : "Showing " + (startIndex + 1) + "–" + endIndex + " of " + total);
        totalPendingBadgeLabel.setText(total + " total");
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        // PATTERN:LIST one row per pending registration on this page
        pendingRowsBox.getChildren().clear();
        for (PendingRow row : pending.subList(startIndex, endIndex)) pendingRowsBox.getChildren().add(buildRow(row));
        emptyLabel.setVisible(total == 0);
        emptyLabel.setManaged(total == 0);
    }

    /** Organization, role, city, and the registered date with the Review button. */
    private GridPane buildRow(PendingRow pendingRow) {
        GridPane row = AdminTable.row(38, 20, 22, 20);
        AdminTable.put(row, 0, AdminTable.label(pendingRow.name(), "form-label"));
        AdminTable.put(row, 1, AdminTable.pill(AdminTable.roleName(pendingRow.role()), AdminTable.roleStyle(pendingRow.role())));
        AdminTable.put(row, 2, AdminTable.label(pendingRow.city(), "table-cell-text"));
        AdminTable.put(row, 3, AdminTable.textAndButton(AdminTable.date(pendingRow.submittedAt()),
                AdminTable.button("Review ›", "table-review-btn", e -> {
                    AdminSelection.selectRegistration(pendingRow.registrationId());
                    Navigator.show(Screen.REVIEW_REGISTRATION);
                })));
        return row;
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
        int totalPages = (int) Math.ceil((double) pending.size() / pageSize);
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
