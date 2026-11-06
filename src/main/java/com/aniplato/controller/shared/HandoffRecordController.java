package com.aniplato.controller.shared;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PostCards;
import com.aniplato.controller.TableRows;
import com.aniplato.model.HandoffRecord;
import com.aniplato.model.Report;
import com.aniplato.service.HandoffService;
import com.aniplato.service.ReportService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The handoff record and, once submitted, its report, read-only. Shared screen: it never checks the role (A5). */
public class HandoffRecordController {

    private final HandoffService handoffService = new HandoffService();
    private final ReportService reportService = new ReportService();
    private int offerId;

    @FXML private Button submitReportButton;
    @FXML private VBox recordBox;
    @FXML private Label disclaimerLabel;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        offerId = Selection.offerId();
        HandoffRecord record = FormSupport.load(() -> handoffService.getHandoffRecord(offerId), null);
        boolean canSubmit = record != null && FormSupport.load(() -> reportService.canSubmit(offerId), false);
        submitReportButton.setVisible(canSubmit);
        submitReportButton.setManaged(canSubmit);
        disclaimerLabel.setText(HandoffRecord.DISCLAIMER);
        if (record == null) return;
        Report report = FormSupport.load(() -> reportService.getReport(offerId), null);
        recordBox.getChildren().add(card(record, report));
    }

    /** One white card: the record's facts, then the report (or the note that it is still due). */
    private VBox card(HandoffRecord r, Report report) {
        Label title = new Label(r.foodDescription());
        title.getStyleClass().add("offer-title");
        Label sub = new Label("Record " + r.number() + " · closed " + FormSupport.when(r.receivedAt()));
        sub.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, new VBox(2, title, sub), spacer, TableRows.pill("Rescued", "done"));
        head.setAlignment(Pos.TOP_LEFT);

        GridPane facts = new GridPane();
        facts.setHgap(32);
        facts.setVgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            facts.getColumnConstraints().add(column);
        }
        fact(facts, 0, 0, "DONOR", r.donorName());
        fact(facts, 1, 0, "FOOD GROUP", r.groupName());
        fact(facts, 0, 1, "FOOD", r.category().label() + " · " + FormSupport.kg(r.agreedKg()) + " (as agreed)");
        fact(facts, 1, 1, "STATED USE", r.useDescription());
        fact(facts, 0, 2, "WHO IT FED", r.headcount() + " people · " + r.beneficiaries());
        fact(facts, 1, 2, "PLACE", r.place());
        fact(facts, 0, 3, "RELEASED BY DONOR", FormSupport.when(r.releasedAt()));
        fact(facts, 1, 3, "RECEIVED BY FOOD GROUP", FormSupport.when(r.receivedAt()));
        fact(facts, 0, 4, "DONOR ATTESTATION", r.donorAttestedAt() == null ? "—" : "Signed " + FormSupport.when(r.donorAttestedAt()));
        fact(facts, 1, 4, "GROUP ATTESTATION", r.groupAttestedAt() == null ? "—" : "Signed " + FormSupport.when(r.groupAttestedAt()));

        Hyperlink flag = new Hyperlink("Flag a concern about this handoff");
        flag.getStyleClass().add("board-link");
        flag.setOnAction(e -> dialogController.showFlagSafetyConcernMode(r.offerId()));

        VBox card = new VBox(16, head, facts, new Separator(), reportSection(r, report), flag);
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(24));
        return card;
    }

    private static Node reportSection(HandoffRecord r, Report report) {
        Label heading = new Label("REPORT");
        heading.getStyleClass().add("fact-label");
        if (report == null) {
            return new VBox(8, heading, new HBox(10, TableRows.pill("Not submitted", "neutral"),
                    plain("Due " + FormSupport.when(r.reportDueAt()) + ".")));
        }
        boolean late = report.isLate(r.reportDueAt());
        HBox status = new HBox(10, TableRows.pill(late ? "Submitted late" : "Submitted", late ? "accepted" : "done"),
                plain("Submitted " + FormSupport.when(report.submittedAt)));
        status.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, heading, status, plain(report.eventName + " · " + report.eventDate + " · " + report.peopleServed + " people served"),
                plain(report.description), PostCards.thumbnails(report.photos));
    }

    private static void fact(GridPane grid, int column, int row, String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("fact-label");
        Label text = new Label(value == null || value.isBlank() ? "—" : value);
        text.getStyleClass().add("fact-value");
        text.setWrapText(true);
        grid.add(new VBox(4, name, text), column, row);
    }

    private static Label plain(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("fact-value");
        label.setWrapText(true);
        return label;
    }

    // PATTERN:ACTION opens the Submit Report form for this handoff
    @FXML
    private void handleSubmitReport() {
        Selection.selectOffer(offerId);
        Navigator.show(Navigator.Screen.FOODGROUP_SUBMIT_REPORT);
    }

    // PATTERN:ACTION back to History
    @FXML
    private void handleBack() {
        Navigator.show(Navigator.Screen.HISTORY);
    }
}
