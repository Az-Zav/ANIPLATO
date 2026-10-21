package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.dao.SafetyNoticeDao.NoticeRow;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.SafetyNotice;
import com.aniplato.service.SafetyService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class SafetyNoticesController {

    @FXML private Label noticeSummaryLabel;
    @FXML private VBox safetyNoticeRowsBox;
    @FXML private Label emptyLabel;

    // Embedded Dialog Overlay Controller Injection
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    private final SafetyService safetyService = new SafetyService();

    // PATTERN:DYNAMIC fills the summary and the list from the notices
    @FXML
    private void initialize() {
        load();
    }

    private void load() {
        List<NoticeRow> notices;
        try {
            notices = safetyService.getNoticeRows();
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            noticeSummaryLabel.setText("Could not load the safety notices. Check the database connection.");
            return;
        }
        long open = notices.stream().filter(n -> n.status() == SafetyNotice.Status.OPEN).count();
        noticeSummaryLabel.setText(open + " open · close each as dismissed, or suspend the account responsible");

        // PATTERN:LIST one row per notice, newest first
        safetyNoticeRowsBox.getChildren().clear();
        for (NoticeRow notice : notices) safetyNoticeRowsBox.getChildren().add(buildRow(notice));
        emptyLabel.setVisible(notices.isEmpty());
        emptyLabel.setManaged(notices.isEmpty());
    }

    /** Flagged account with its status and notice number, who raised it, the listing, the concern, and when. */
    private GridPane buildRow(NoticeRow notice) {
        boolean open = notice.status() == SafetyNotice.Status.OPEN;
        GridPane row = AdminTable.row(22, 22, 17, 17, 22);
        AdminTable.put(row, 0, AdminTable.titled(notice.flaggedName(), statusTag(notice.status()),
                AdminTable.label("Notice #" + notice.id(), "table-cell-muted")));
        AdminTable.put(row, 1, AdminTable.pill(notice.raisedByName(), "pill-food-org"));
        AdminTable.put(row, 2, AdminTable.wrapped(notice.listingText() == null ? "" : notice.listingText(), "table-cell-text"));
        AdminTable.put(row, 3, AdminTable.wrapped(concernText(notice), "table-cell-text"));
        AdminTable.put(row, 4, AdminTable.textAndButton(AdminTable.dateTime(notice.raisedAt()), open
                ? AdminTable.button("Resolve", "table-review-btn", e -> resolve(notice))
                : AdminTable.button("View", "secondary-button", e -> view(notice))));
        return row;
    }

    private static Label statusTag(SafetyNotice.Status status) {
        return switch (status) {
            case OPEN -> AdminTable.label("Open", "tag-reregistered");
            case DISMISSED -> AdminTable.label("Dismissed", "table-cell-muted");
            case ACCOUNT_SUSPENDED -> AdminTable.label("Suspended", "required-star");
        };
    }

    private static String concernText(NoticeRow notice) {
        return notice.kind() == SafetyNotice.Kind.OVERDUE_REPORT ? "Report overdue" : notice.concern();
    }

    // PATTERN:ACTION opens the resolve dialog; each button needs the admin's note
    private void resolve(NoticeRow notice) {
        dialogController.showResolveSafetyNoticeMode(notice.raisedByName(), AdminTable.dateTime(notice.raisedAt()),
                concernText(notice), notice.flaggedName(),
                note -> {
                    safetyService.dismiss(notice.id(), note);
                    load();
                },
                note -> {
                    safetyService.suspendFlaggedAccount(notice.id(), note);
                    load();
                });
    }

    // PATTERN:ACTION shows how a closed notice was resolved
    private void view(NoticeRow notice) {
        String how = notice.status() == SafetyNotice.Status.DISMISSED ? "Dismissed" : "Account suspended";
        dialogController.showNoticeResolvedMode(how, notice.adminNote());
    }
}
