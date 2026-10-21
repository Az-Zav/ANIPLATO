package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.Account;
import com.aniplato.service.AccountStatusService;
import com.aniplato.service.AccountStatusService.AccountRow;
import com.aniplato.service.AccountStatusService.AccountsOverview;
import com.aniplato.util.Settings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class AccountsController {

    @FXML private Label accountsSummaryLabel;
    @FXML private VBox accountRowsBox;

    // Embedded Dialog Overlay Controller Injection
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Pagination Footer Controls
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    private static final int PAGE_SIZE = 10;

    private final AccountStatusService accountStatusService = new AccountStatusService();
    private final Pager<AccountRow> pager = new Pager<>(PAGE_SIZE);

    // PATTERN:DYNAMIC fills the summary and the list from the organization accounts
    @FXML
    private void initialize() {
        load();
    }

    private void load() {
        AccountsOverview overview;
        try {
            overview = accountStatusService.getAccounts(LocalDate.now());
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            accountsSummaryLabel.setText("Could not load the accounts. Check the database connection.");
            return;
        }
        accountsSummaryLabel.setText(overview.approved() + " approved · " + overview.suspended() + " suspended · "
                + overview.documentsEnding() + " with documents expiring in " + Settings.DOCUMENT_WARNING_DAYS + " days");
        pager.setItems(overview.rows());   // keeps the page the admin is on after a suspend or reinstate
        showPage();
    }

    /** Fills the rows of the current page and the footer. */
    private void showPage() {
        accountRowsBox.getChildren().clear();
        for (AccountRow row : pager.current()) accountRowsBox.getChildren().add(buildRow(row));
        pageInfoLabel.setText(pager.info());
        prevPageBtn.setDisable(!pager.hasPrevious());
        nextPageBtn.setDisable(!pager.hasNext());
    }

    @FXML
    private void handlePrevPage() {
        pager.previous();
        showPage();
    }

    @FXML
    private void handleNextPage() {
        pager.next();
        showPage();
    }

    /** Name with its status, role, city, and the document note with Suspend or Reinstate. */
    private GridPane buildRow(AccountRow account) {
        boolean suspended = account.status() == Account.Status.SUSPENDED;
        GridPane row = AdminTable.row(32, 18, 18, 32);
        AdminTable.put(row, 0, suspended
                ? AdminTable.titled(account.name(), AdminTable.label("Suspended", "required-star"),
                        AdminTable.label(account.statusNote() == null ? "" : account.statusNote(), "table-cell-muted"))
                : AdminTable.titled(account.name(), AdminTable.label("Approved", "pill-food-donor")));
        AdminTable.put(row, 1, AdminTable.pill(AdminTable.roleName(account.role()), AdminTable.roleStyle(account.role())));
        AdminTable.put(row, 2, AdminTable.label(account.city(), "table-cell-text"));
        AdminTable.put(row, 3, AdminTable.textAndButton(account.documentNote(), suspended
                ? AdminTable.button("Reinstate", "table-review-btn", e -> reinstate(account))
                : AdminTable.button("Suspend", "btn-suspend-outline", e -> suspend(account))));
        return row;
    }

    private void suspend(AccountRow account) {
        dialogController.showSuspendAccountMode(account.name(), reason -> {
            accountStatusService.suspendAccount(account.accountId(), reason);   // the service needs a reason
            load();
        });
    }

    private void reinstate(AccountRow account) {
        dialogController.showReinstateAccountMode(account.name(), null, () -> {
            accountStatusService.reinstateAccount(account.accountId());
            load();
        });
    }
}
