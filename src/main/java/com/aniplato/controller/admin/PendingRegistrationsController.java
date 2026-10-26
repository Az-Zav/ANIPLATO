package com.aniplato.controller.admin;

import com.aniplato.controller.TableRows;
import com.aniplato.exception.DataAccessException;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.RegistrationService.PendingRow;
import com.aniplato.util.Selection;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class PendingRegistrationsController {

    @FXML private Label summaryLabel;
    @FXML private VBox pendingRowsBox;
    @FXML private Label emptyLabel;

    private final RegistrationService registrationService = new RegistrationService();

    // PATTERN:DYNAMIC fills the summary and the list from the pending registrations
    @FXML
    private void initialize() {
        List<PendingRow> pending;
        try {
            pending = registrationService.getPending();
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            summaryLabel.setText("Could not load the registrations. Check the database connection.");
            return;
        }
        long again = pending.stream().filter(PendingRow::registeredAgain).count();
        summaryLabel.setText(pending.size() + " registrations awaiting review"
                + (again > 0 ? " · " + again + " registered again" : ""));

        // PATTERN:LIST one row per pending registration
        pendingRowsBox.getChildren().clear();
        for (PendingRow row : pending) pendingRowsBox.getChildren().add(buildRow(row));
        emptyLabel.setVisible(pending.isEmpty());
        emptyLabel.setManaged(pending.isEmpty());
    }

    /** Name (with a "Registered again" tag), role, city, and the registered date with the Review button. */
    private GridPane buildRow(PendingRow pending) {
        GridPane row = TableRows.row(38, 20, 22, 20);
        TableRows.put(row, 0, pending.registeredAgain()
                ? AdminTable.titled(pending.name(), AdminTable.label("Registered again", "tag-reregistered"))
                : AdminTable.label(pending.name(), "form-label"));
        TableRows.put(row, 1, AdminTable.pill(AdminTable.roleName(pending.role()), AdminTable.roleStyle(pending.role())));
        TableRows.put(row, 2, AdminTable.label(pending.city(), "table-cell-text"));
        TableRows.put(row, 3, AdminTable.textAndButton(AdminTable.date(pending.submittedAt()),
                AdminTable.button("Review", "table-review-btn", e -> review(pending.registrationId()))));
        return row;
    }

    // PATTERN:NAV -> REVIEW_REGISTRATION (with the chosen registration)
    private void review(int registrationId) {
        Selection.selectRegistration(registrationId);
        Navigator.show(Screen.REVIEW_REGISTRATION);
    }
}
