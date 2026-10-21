package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.Account;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.AccountRow;
import com.aniplato.service.RegistrationService.AccountsOverview;
import javafx.fxml.FXML;
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

    private final RegistrationService registrationService = new RegistrationService();

    // PATTERN:DYNAMIC fills the summary and the list from the organization accounts
    @FXML
    private void initialize() {
        load();
    }

    private void load() {
        AccountsOverview overview;
        try {
            overview = registrationService.getAccounts(LocalDate.now());
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            accountsSummaryLabel.setText("Could not load the accounts. Check the database connection.");
            return;
        }
        accountsSummaryLabel.setText(overview.approved() + " approved · " + overview.suspended() + " suspended · "
                + overview.documentsEnding() + " with documents expiring in 30 days");
        accountRowsBox.getChildren().clear();
        for (AccountRow row : overview.rows()) accountRowsBox.getChildren().add(buildRow(row));
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
            registrationService.suspendAccount(account.accountId(), reason);   // the service needs a reason
            load();
        });
    }

    private void reinstate(AccountRow account) {
        dialogController.showReinstateAccountMode(account.name(), null, () -> {
            registrationService.reinstateAccount(account.accountId());
            load();
        });
    }
}
