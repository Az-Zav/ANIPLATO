package com.aniplato.controller.admin;

import com.aniplato.controller.TableRows;
import com.aniplato.exception.DataAccessException;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.PendingRow;
import com.aniplato.service.StatsService;
import com.aniplato.service.StatsService.AdminSummary;
import com.aniplato.util.Selection;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Settings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class AdminDashboardController {

    // Metric Cards
    @FXML private Label pendingCountLabel;
    @FXML private Label kgListedLabel;
    @FXML private Label rescueRateLabel;
    @FXML private Label openSafetyNoticesCountLabel;
    @FXML private Label documentsExpiringCountLabel;
    @FXML private Label documentsExpiringCaptionLabel;
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
    private static final int PAGE_SIZE = 10;
    private final Pager<PendingRow> pager = new Pager<>(PAGE_SIZE);

    @FXML
    private void initialize() {
        try {
            AdminSummary summary = statsService.getAdminSummary(LocalDate.now());
            pager.setItems(registrationService.getPending());
            pendingCountLabel.setText(String.valueOf(summary.pendingRegistrations()));
            kgListedLabel.setText(AdminTable.kg(summary.kgListedThisMonth()));
            rescueRateLabel.setText(AdminTable.percent(summary.rescueRate()));
            openSafetyNoticesCountLabel.setText(String.valueOf(summary.openSafetyNotices()));
            documentsExpiringCountLabel.setText(String.valueOf(summary.documentsEnding()));
            documentsExpiringCaptionLabel.setText("Within the next " + Settings.DOCUMENT_WARNING_DAYS + " days");
            suspendedCountLabel.setText(String.valueOf(summary.suspendedAccounts()));
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            pageInfoLabel.setText("Could not load the dashboard. Check the database connection.");
            return;
        }
        updatePaginationState();
    }

    /**
     * Fills the rows of the current page and updates button states / footer text
     */
    private void updatePaginationState() {
        pageInfoLabel.setText(pager.info());
        totalPendingBadgeLabel.setText(pager.total() + " total");
        prevPageBtn.setDisable(!pager.hasPrevious());
        nextPageBtn.setDisable(!pager.hasNext());

        // PATTERN:LIST one row per pending registration on this page
        pendingRowsBox.getChildren().clear();
        for (PendingRow row : pager.current()) pendingRowsBox.getChildren().add(buildRow(row));
        emptyLabel.setVisible(pager.total() == 0);
        emptyLabel.setManaged(pager.total() == 0);
    }

    /** Organization, role, city, and the registered date with the Review button. */
    private GridPane buildRow(PendingRow pendingRow) {
        GridPane row = TableRows.row(38, 20, 22, 20);
        TableRows.put(row, 0, AdminTable.label(pendingRow.name(), "form-label"));
        TableRows.put(row, 1, AdminTable.pill(AdminTable.roleName(pendingRow.role()), AdminTable.roleStyle(pendingRow.role())));
        TableRows.put(row, 2, AdminTable.label(pendingRow.city(), "table-cell-text"));
        TableRows.put(row, 3, AdminTable.textAndButton(AdminTable.date(pendingRow.submittedAt()),
                AdminTable.button("Review ›", "table-review-btn", e -> {
                    Selection.selectRegistration(pendingRow.registrationId());
                    Navigator.show(Screen.REVIEW_REGISTRATION);
                })));
        return row;
    }

    @FXML
    private void handlePrevPage() {
        pager.previous();
        updatePaginationState();
    }

    @FXML
    private void handleNextPage() {
        pager.next();
        updatePaginationState();
    }

    @FXML
    private void handleNavPendingRegistrations() {
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }
}
